/*
 * Copyright (c) 2003 The BISON Project
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License version 2 as published by
 * the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package peeremu.emu;

import java.util.Arrays;
import java.util.Observer;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import peeremu.config.ConfigProperties;
import peeremu.config.Configuration;
import peeremu.core.CommonState;
import peeremu.core.Control;
import peeremu.core.Network;
import peeremu.core.Node;
import peeremu.edsim.CDProtocol;
import peeremu.edsim.EDProtocol;





/**
 * Event-driven simulator. The simulator is able to run both event-driven
 * protocols {@link EDProtocol} and cycle-driven protocols {@link CDProtocol}.
 * To execute any of the cycle-based classes (observers, dynamics, and
 * protocols), the <code>step</code> parameter of the {@link RunnableScheduler}
 * associated to the class must be specified, to define the length of cycles.
 * 
 * 
 * 
 * @author Alberto Montresor
 * @version $Revision: 92 $
 */
public class MTSimulator
{
  public static Object mutex = new Object();
  // ---------------------------------------------------------------------
  // Parameters
  // ---------------------------------------------------------------------
  /**
   * The string name of the configuration parameter that specifies the ending
   * time for simulation, in milliseconds. No event after this value will be
   * executed.
   */
  public static final String PAR_DURATION = "simulation.duration";
  /**
   * The string name of the configuration parameter that specifies how often the
   * simulator should log the current time on the console. Standard error is
   * used for such logs. For Multi-Threaded Simulations, this defaults to 1
   * second.
   */
  public static final String PAR_LOGTIME = "simulation.logtime";
  /**
   * If this parameter is present, the order of visiting each node for
   * cycle-based protocols is shuffled at each cycle. The default is no shuffle.
   */
  // public static final String PAR_SHUFFLE = "simulation.shuffle";
  /**
   * String name of the configuration parameter that specifies how many bits are
   * used to order events that occurs at the same time. Defaults to 8. A value
   * smaller than 8 causes an IllegalParameterException. Higher values allow for
   * a better discrimination, but may reduce the granularity of time values.
   */
  public static final String PAR_RBITS = "simulation.timebits";
  /**
   * This is the prefix for initializers. These have to be of type
   * {@link Dynamics}.
   */
  public static final String PAR_INIT = "init";
  /**
   * This is the prefix for network dynamism managers. These have to be of type
   * {@link Dynamics}.
   */
  public static final String PAR_DYN = "dynamics";
  /**
   * This is the prefix for observers. These have to be of type {@link Observer}
   * .
   */
  public static final String PAR_OBS = "observer";
  // ---------------------------------------------------------------------
  // Fields
  // ---------------------------------------------------------------------
  public static long startTime;
  /** Duration for simulation, in milliseconds */
  protected static long duration;
  /** Log time */
  // protected static long logtime;
  /** If true, when executing a cycle-based protocols items are shuffled */
  // private static boolean shuffle;
  /** holds the observers of this simulation */
  protected static RunnableObserver[] observers = null;
  /** Holds the observer schedulers of this simulation */
  protected static RunnableScheduler[] obsSchedules = null;
  /** holds the modifiers of this simulation */
  // protected static Dynamics[] dynamics=null;
  /** Holds the pids of the CDProtocols to be executed in this simulation */
  protected static int[] cdprotocols = null;
  /** Holds the protocol schedulers of this simulation */
  protected static RunnableScheduler[] protSchedules = null;
  /** Ordered list of events (heap) */
  // protected static Heap heap = new Heap();
  private static ScheduledThreadPoolExecutor executor;



  // ---------------------------------------------------------------------
  // Private methods
  // ---------------------------------------------------------------------
  /**
   * Load and run initializers.
   */
  protected static void runInitializers()
  {
    Object[] inits = Configuration.getInstanceArray(PAR_INIT);
    String names[] = Configuration.getNames(PAR_INIT);
    for (int i = 0; i<inits.length; ++i)
    {
      System.err.println("- Running initializer "+names[i]+": "+inits[i].getClass());
      ((Control) inits[i]).execute();
    }
  }



  // --------------------------------------------------------------------
  protected static String[] loadObservers()
  {
    // load observers
    String[] names = Configuration.getNames(PAR_OBS);
    observers = new RunnableObserver[names.length];
    obsSchedules = new RunnableScheduler[names.length];
    for (int i = 0; i<names.length; ++i)
    {
      observers[i] = (RunnableObserver) Configuration.getInstance(names[i]);
      obsSchedules[i] = new RunnableScheduler(names[i]);
    }
    System.err.println("MTSimulator: loaded observers "+Arrays.asList(names));
    return names;
  }



  // XXX Need to add this.
  // ---------------------------------------------------------------------
  //
  // protected static String[] loadDynamics()
  // {
  // // load dynamism managers
  // String[] names = Configuration.getNames(PAR_DYN);
  // dynamics = new Dynamics[names.length];
  // dynSchedules = new RunnableScheduler[names.length];
  // for(int i=0; i<names.length; ++i)
  // {
  // dynamics[i]=(Dynamics)Configuration.getInstance(names[i]);
  // dynSchedules[i] = new RunnableScheduler(names[i], false);
  // }
  // System.err.println("MTSimulator: loaded dynamics "+
  // Arrays.asList(names));
  // return names;
  // }
  // ---------------------------------------------------------------------
  protected static void loadProtocolSchedules()
  {
    // load protocol schedulers
    String[] names = Configuration.getNames(Node.PAR_PROT);
    protSchedules = new RunnableScheduler[names.length];
    for (int i = 0; i<names.length; ++i)
    {
      protSchedules[i] = new RunnableScheduler(names[i]);
    }
  }



  private static void schedule(Runnable command, RunnableScheduler sched, int initialFixedDelay, int initialRandomDelay)
  {
    if (sched.periodic())
    {
      initialRandomDelay++; // to prevent calling Random.nextInt(0)
      executor.scheduleAtFixedRate(new RunnableSchedulerWrapper(command, sched),
          initialFixedDelay+CommonState.r.nextInt(initialRandomDelay), sched.period, TimeUnit.MILLISECONDS);
    }
    else
      executor.schedule(command, sched.at, TimeUnit.MILLISECONDS);
  }



  // ---------------------------------------------------------------------
  // Public methods
  // ---------------------------------------------------------------------
  /**
   * Runs an experiment
   */
  public static void nextExperiment()
  {
    // Reading parameters
    duration = Configuration.getLong(PAR_DURATION);
    // initialization
    System.err.println("MTSimulator: resetting");
    Network.reset();
    System.err.println("MTSimulator: running initializers");
    // CommonState.setTime(0); // -NOT- needed here
    // WAS: CommonState.setPhase(CommonState.PRE_DYNAMICS);
    runInitializers();
    // Load observer, dynamics, protocol schedules
    loadObservers();
    // loadDynamics();
    loadProtocolSchedules();
    // XXX Allow non-runnable (non-executable) protocols
    // XXX compute how many threads will be needed
    // (protocols * items) + observers + dynamics
    executor = new ScheduledThreadPoolExecutor(500);
    startTime = System.currentTimeMillis();
    System.out.println("Starting...");
    // Schedule observer execution
    for (int i = 0; i<observers.length; i++)
      schedule(observers[i], obsSchedules[i], obsSchedules[i].period*99/100, 0);
    // Schedule protocol execution
    for (int p = 0; p<protSchedules.length; p++)
      for (int i = 0; i<Network.size(); i++)
        schedule((Runnable) Network.get(i).getProtocol(p), protSchedules[p], 0, protSchedules[p].period*95/100);
    // If the duration is defined, manage termination of the experiment.
    if (duration>=0)
    {
      try
      {
        executor.awaitTermination(duration, TimeUnit.MILLISECONDS);
      }
      catch (InterruptedException e)
      {
        e.printStackTrace();
      }
      executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
      executor.shutdown();
    }
  }



  // ---------------------------------------------------------------------
  /**
   * Adds a new event to be scheduled, specifying the number of time units of
   * delay, in milliseconds, and runnable object to be invoked.
   * 
   * @param command The Runnable object to be invoked
   * @param delay The number of milliseconds before the event is executed
   */
  public static void add(Runnable command, long delay)
  {
    executor.schedule(command, delay, TimeUnit.MILLISECONDS);
  }



  // ---------------------------------------------------------------------
  public static void main(String args[])
  {
    System.err.println("Simulator: loading configuration");
    Configuration.setConfig(new ConfigProperties(args));
    nextExperiment();
  }
}
