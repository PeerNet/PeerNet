/*
 * Created on Apr 26, 2012 by Spyros Voulgaris
 *
 */

/*
 * Copyright (c) 2003-2005 The BISON Project
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
package peernet.core;

import java.util.Arrays;

import peernet.config.Configuration;
import peernet.config.IllegalParameterException;
import peernet.dynamics.NodeInitializer;
import peernet.transport.Address;





/**
 * Event-driven simulator engine. It is a fully static singleton class. For an
 * event driven simulation the configuration has to describe a set of
 * {@link Protocol}s, a set of {@link Control}s and their ordering and a set of
 * initializers and their ordering. See parameters {@value #PAR_INIT},
 * {@value #PAR_CONTROL}.
 * <p>
 * One experiment run by {@link #nextExperiment} works as follows. First the
 * initializers are run in the specified order. Then the first execution of all
 * specified controls is scheduled in the event queue. This scheduling is
 * defined by the {@link Schedule} parameters of each control component. After
 * this, the first event is taken from the event queue. If the event wraps a
 * control, the control is executed, otherwise the event is delivered to the
 * destination protocol, that must implement {@link EDProtocol}. This is
 * iterated while the current time is less than {@value #PAR_DURATION} or the
 * queue becomes empty. If more control events fall at the same time point, then
 * the order given in the configuration is respected. If more non-control events
 * fall at the same time point, they are processed in a random order.
 * <p>
 * The engine also provides the interface to add events to the queue. Note that
 * this engine does not explicitly run the protocols. In all cases at least one
 * control or initializer has to be defined that sends event(s) to protocols.
 * <p>
 * Controls can be scheduled (using the {@link Schedule} parameters in the
 * configuration) to run after the experiment has finished. That is, each
 * experiment is finished by running the controls that are scheduled to be run
 * after the experiment.
 * <p>
 * Any control can interrupt an experiment at any time it is executed by
 * returning true in method {@link Control#execute}. However, the controls
 * scheduled to run after the experiment are still executed completely,
 * irrespective of their return value and even if the experiment was
 * interrupted.
 * <p>
 * {@link CDScheduler} has to be mentioned that is a control that can bridge the
 * gap between {@link peernet.cdsim} and the event driven engine. It can wrap
 * {@link peernet.edsim.CDProtocol} appropriately so that the execution of the
 * cycles are scheduled in configurable ways for each node individually. In some
 * cases this can add a more fine-grained control and more realism to
 * {@link peernet.edsim.CDProtocol} simulations, at the cost of some loss in
 * performance.
 * <p>
 * When protocols at different nodes send messages to each other, they might
 * want to use a model of the transport layer so that in the simulation message
 * delay and message omissions can be modeled in a modular way. This
 * functionality is implemented in package {@link peernet.transport}.
 * 
 * @see Configuration
 */
public abstract class Engine
{
  private static final String PREFIX = "engine";


  /**
   * The duration of the experiment. Only events that have a strictly smaller
   * value are executed.
   * 
   * @config
   */
  private static final String PAR_DURATION = "duration";

  /**
   * This parameter specifies how often the simulator should log the current
   * time on the standard error.
   * 
   * @config
   */
  private static final String PAR_LOGTIME = "logtime";

  /**
   * This parameter specifies how many bits are used to order events that occur
   * at the same time. Defaults to 8. A value smaller than 8 causes an
   * IllegalParameterException. Higher values allow for a better discrimination,
   * but reduce the maximal time steps that can be simulated.
   * 
   * @config
   */
  private static final String PAR_RBITS = "timebits";

  /**
   * This is the prefix for initializers. These have to be of type
   * {@link Control}. They are run at the beginning of each experiment, in the
   * order specified by the configuration.
   * 
   * @see Configuration
   * @config
   * @config
   */
  private static final String PAR_INIT = "init";

  /**
   * This is the prefix for {@link Control} components. They are run at the time
   * points defined by the {@link Schedule} associated to them. If some
   * controls have to be executed at the same time point, they are executed in
   * the order specified in the configuration.
   * 
   * @see Configuration
   * @config
   */
  private static final String PAR_CONTROL = "control";

  private static final String PAR_PROTOCOL = "protocol";

  private static final String PAR_MODE = "mode";

  // ---------------------------------------------------------------------
  // Fields
  // ---------------------------------------------------------------------

  /** Maximum time for simulation */
  static long endtime;

  /** Log time */
  protected static long logtime;

  /** Number of bits used for random */
  static int rbits;

  /** holds the modifiers of this simulation */
  static Control[] controls = null;

  /** Holds the control schedules */
  protected static Schedule[] controlSchedules = null;

  /** Holds the protocol schedules */
  protected static Schedule[] protocolSchedules = null;

  protected static long nextlog = 0;

  private static Engine instance = null;

  
  
  
  
  
  
  private static final Type type;
  private static final AddressType addressType;

  public enum Type
  {
    SIM, EMU, NET, COORDINATOR;
  }

  public enum AddressType
  {
    SIM, NET;
  }

  static
  {
    String typeStr = Configuration.getString(PREFIX+"."+PAR_MODE, "");
    if (typeStr.equals("sim"))
    {
      type = Type.SIM;
      addressType = AddressType.SIM;
    }
    else if (typeStr.equals("emu"))
    {
      type = Type.EMU;
      addressType = AddressType.SIM;
    }
    else if (typeStr.equals("net"))
    {
      type = Type.NET;
      addressType = AddressType.NET;
    }
    else if (typeStr.equals("coordinator"))
    {
      type = Type.COORDINATOR;
      addressType = null;
    }
    else
      throw new IllegalParameterException(PREFIX+"."+PAR_MODE, "Possible types: sim, emu, net, coordinator");
  }

  public static Type getType()
  {
    return type;
  }

  public static AddressType getAddressType()
  {
    return addressType;
  }






  // ---------------------------------------------------------------------
  // Private methods
  // ---------------------------------------------------------------------
  /**
   * Load and run initializers.
   */
  private void runInitializers()
  {
    Object[] inits = Configuration.getInstanceArray(PAR_INIT);
    String names[] = Configuration.getNames(PAR_INIT);
    for (int i = 0; i<inits.length; ++i)
    {
      System.err.println("- Running initializer "+names[i]+": "+inits[i].getClass());
      ((Control) inits[i]).execute();
    }
  }



  /**
   * Schedule all controls in the provided heap.
   * 
   * @param heap
   */
  private void scheduleControls()
  {
    // load controls
    String[] names = Configuration.getNames(PAR_CONTROL);
    controls = new Control[names.length];
    controlSchedules = new Schedule[names.length];
    for (int i = 0; i<names.length; i++)
    {
      controls[i] = (Control) Configuration.getInstance(names[i]);
      controlSchedules[i] = new Schedule(names[i]);
    }
    System.err.println("Engine: loaded controls "+Arrays.asList(names));

    // Schedule controls execution
    int order = 0;
    for (int i=0; i<controls.length; i++)
    {
      if (i>Byte.MAX_VALUE)
        throw new IllegalArgumentException("Too many control objects");
      long delay = controlSchedules[i].initialDelay();
      if (delay >= 0)
        add(delay,  null, null, i, null);
    }
  }



  /**
   * Private method that schedules the first execution of a node's protocols.
   * 
   * @param node
   */
  private void scheduleProtocols(Node node)
  {
    for (int j=0; j<protocolSchedules.length; j++)
    {
      long delay = protocolSchedules[j].initialDelay();
      if (delay >= 0)
        add(delay, null, node, j, scheduledEvent);
    }
  }



  /**
   * Private method that loads the schedule of each protocol, and then
   * applies them on all nodes.
   */
  private void scheduleProtocols()
  {
    // Load protocol schedules
    String[] protocolNames = Configuration.getNames(PAR_PROTOCOL);
    protocolSchedules = new Schedule[protocolNames.length];
    for (int i=0; i<protocolNames.length; i++)
      protocolSchedules[i] = new Schedule(protocolNames[i]);

    // Schedule protocols for all nodes
    for (int i=0; i<Network.size(); i++)
    {
      Node node = Network.get(i);
      scheduleProtocols(node);
    }
  }



  /**
   * This is a dummy class without any fields, used for scheduling periodic
   * events (i.e., for nextCycle).
   * 
   * @author spyros
   */
  protected static class ScheduledEvent {};
  protected static ScheduledEvent scheduledEvent = new ScheduledEvent();


  
  // ---------------------------------------------------------------------
  /**
   * Adds a new event to be scheduled, specifying the number of time units of
   * delay, and the execution order parameter.
   * 
   * @param time The actual time at which the next event should be scheduled.
   * @param order The index used to specify the order in which control events
   *          should be executed, if they happen to be at the same time, which
   *          is typically the case.
   * @param event The control event
   */
//  static void addControlEvent(long time, int order, ControlEvent event)
//  {
//    if (time>=endtime)
//      return;
//    time = (time<<rbits)|order;
//    heap.add(time, null, null, (byte)0, event);
//  }


  protected abstract void createHeaps();




  /**
   * Adds a new event to be scheduled, specifying the number of time units of
   * delay, and the node and the protocol identifier to which the event will be
   * delivered.
   * 
   * @param delay The number of time units before the event is scheduled. Has to
   *          be non-negative.
   * @param event The object associated to this event
   * @param node The node associated to the event.
   * @param pid The identifier of the protocol to which the event will be
   *          delivered
   */
  public void add(long delay, Address src, Node node, int pid, Object event)
  {
    long nextTime = CommonState.getTime()+delay;

    if (delay<0)
    {
      System.err.println("NOT ignoring event with negative delay: "+delay);
//      return;
    }

//XXX Check for this somewhere in initialization, not in EACH new event!!
//    if (pid>Byte.MAX_VALUE)
//      throw new IllegalArgumentException("This version does not support more than "+Byte.MAX_VALUE+" protocols");

    addAtTime(nextTime, src, node, pid, event);
  }

  
  public abstract void addAtTime(long time, Address src, Node node, int pid, Object event);
  public abstract int pendingEvents();


  /**
   * Runs an experiment, resetting everything except the random seed.
   */
  public void startExperiment()
  {
    System.err.println("Engine: starting experiment in "+getType()+" mode");

    rbits = Configuration.getInt(PREFIX+"."+PAR_RBITS, 8);
    if (rbits<8||rbits>=64)
      throw new IllegalParameterException(PREFIX+"."+PAR_RBITS, "This parameter should be >= 8 or < 64");

    endtime = Configuration.getLong(PREFIX+"."+PAR_DURATION, Long.MAX_VALUE);
    CommonState.setEndTime(endtime);

    // Logging
    //XXX: should I keep this?
    logtime = Configuration.getLong(PREFIX+"."+PAR_LOGTIME, Long.MAX_VALUE);
    nextlog = 0;

    // initialization
    System.err.println("Engine: resetting");  // XXX: change to debug() or notify()
    Network.reset();
    createHeaps();
    runInitializers();
    scheduleControls();
    scheduleProtocols();
  }




  public static Engine instance()
  {
    if (instance==null)
    {
      switch (getType())
      {
        case SIM:
          instance = new EngineSim();
          break;
        case EMU:
          instance = new EngineNet();
          break;
        case NET:
          instance = new EngineNet();
          break;
        default:
          throw new IllegalParameterException(PREFIX+"."+PAR_MODE, Configuration.getString(PREFIX+"."+PAR_MODE));
      }
    }

    return instance;
  }



  /**
   * Allows the addition of a new node during the course of an experiment,
   * typically to simulate churn. It adds the node to the Network, runs the
   * provided {@link NodeInitializer}s, and then schedules its protocols for
   * execution.
   * 
   * @return
   */
  public Node addNode(NodeInitializer[] inits)
  {
    // Create and insert new node in the Network
    Node n = Network.addNode();

    // Run NodeInitializer instances
    for (int i=0; i<inits.length; ++i)
      inits[i].initialize(n);

    // Schedule new node's protocol for execution
    scheduleProtocols(n);

    // Return the new node
    return n;
  }
}
