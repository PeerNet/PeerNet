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
package peernet.edsim;

import java.util.Arrays;

import peernet.config.Configuration;
import peernet.config.IllegalParameterException;
import peernet.core.CommonState;
import peernet.core.Control;
import peernet.core.Network;
import peernet.core.Node;
import peernet.core.Protocol;
import peernet.core.Schedule;
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
public class Engine
{
  private static final String PREFIX = "simulation";


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

  private static final String PAR_TYPE = "type";

  // ---------------------------------------------------------------------
  // Fields
  // ---------------------------------------------------------------------

  /** Maximum time for simulation */
  static long endtime;

  /** Log time */
  private static long logtime;

  /** Number of bits used for random */
  static int rbits;

  /** holds the modifiers of this simulation */
  private static Control[] controls = null;

  /** Holds the control schedules */
  private static Schedule[] controlSchedules = null;

  /** Holds the protocol schedules */
  private static Schedule[] protocolSchedules = null;

  /** Ordered list of events (heap) */
  private static Heap simHeap = null;

  private static long nextlog = 0;

  private static Engine instance = null;

  
  
  
  
  
  
  private static final Type type;
  private static final AddressType addressType;

  public enum Type
  {
    SIM, EMU, NET;
  }

  public enum AddressType
  {
    SIM, NET;
  }

  static
  {
    String typeStr = Configuration.getString(PREFIX+"."+PAR_TYPE, "");
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
    else
      throw new IllegalParameterException(PREFIX+"."+PAR_TYPE, "Possible types: sim, emu, net");
  }

  public static Type getType()
  {
    return type;
  }

  public static AddressType getAddressType()
  {
    return addressType;
  }





  // =============== initialization ======================================
  // =====================================================================
  /** to prevent construction */
  protected Engine()
  {
  }



  // ---------------------------------------------------------------------
  // Private methods
  // ---------------------------------------------------------------------
  /**
   * Load and run initializers.
   */
  private static void runInitializers()
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
  private static void scheduleControls(Heap heap)
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
    for (int i = 0; i<controls.length; i++)
    {
      ControlEvent event = new ControlEvent(heap, controls[i], controlSchedules[i], order++);
      if (order>((1<<rbits)-1))
        throw new IllegalArgumentException("Too many control objects");
    }
  }

  private static void scheduleProtocols()
  {
    // load protocols
    String[] protocolNames = Configuration.getNames(PAR_PROTOCOL);
    protocolSchedules = new Schedule[protocolNames.length];
    for (int i=0; i<protocolNames.length; i++)
      protocolSchedules[i] = new Schedule(protocolNames[i]);
    
    for (int i=0; i<Network.size(); i++)
    {
      Node node = Network.get(i);
      for (int j=0; j<protocolNames.length; j++)
      {
        long delay = protocolSchedules[j].nextDelay(0);
        if (delay >= 0)
          Engine.add(delay, null, node, j, scheduledEvent);
      }
    }
  }

  private static class ScheduledEvent {};
  private static ScheduledEvent scheduledEvent = new ScheduledEvent();

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



  // ---------------------------------------------------------------------
  /**
   * This method is used to check whether the current configuration can be used
   * for event driven simulations. It checks for the existence of config
   * parameter {@value #PAR_DURATION}.
   */
  public static final boolean isConfigurationEventDriven()
  {
    return Configuration.contains(PREFIX+"."+PAR_DURATION);
  }



  // ---------------------------------------------------------------------
  /**
   * Execute and remove the next event from the ordered event list.
   * 
   * @return true if the execution should be stopped.
   */
  private static boolean executeNext(Heap heap)
  {
    Heap.Event ev = heap.removeFirst();
    if (ev==null)
    {
      System.err.println("Engine: queue is empty, quitting"+" at time "+CommonState.getTime());
      return true;
    }
    long time = ev.time>>rbits;
    if (time>=nextlog)
    {
      System.err.println("Current time: "+time);
      do
      {
        nextlog += logtime;
      }
      while (time>=nextlog);
    }
    if (time>=endtime)
    {
      System.err.println("Engine: reached end time, quitting, leaving "+heap.size()+" unprocessed events in the queue");
      return true;
    }
    CommonState.setTime(time);
    int pid = ev.pid;
    if (ev.node==null)
    {
      // control event; handled through a special method
      ControlEvent ctrl = (ControlEvent) ev.event;
      return ctrl.execute();
    }
    else if (ev.node.isUp())
    {
      assert ev.node != Network.prototype;
      CommonState.setPid(pid);  // XXX try to entirely avoid CommonState
      CommonState.setNode(ev.node);
//      if (ev.event instanceof NextCycleEvent)
//      {
//        NextCycleEvent nce = (NextCycleEvent) ev.event;
//        nce.execute();
//        Protocol prot = ev.node.getProtocol(pid);
//        prot.nextCycle(ev.node, pid);
//
//        long delay = prot.nextDelay();
//        if (delay == 0)
//        long delay = CDScheduler.sch[pid].step;
//
//        if (delay > 0)
//          Engine.add(delay, null, ev.node, pid, nextCycleEvent);
//      }
      if (ev.event instanceof ScheduledEvent)
      {
        Protocol prot = ev.node.getProtocol(pid);
        prot.nextCycle(ev.node, pid);

        long delay = prot.nextDelay();
        if (delay == 0)
          delay = protocolSchedules[pid].nextDelay(time);

        if (delay > 0)
          Engine.add(delay, null, ev.node, pid, scheduledEvent);
      }
      else // call Protocol.processEvent()
      {
        Protocol prot = ev.node.getProtocol(pid);
        prot.processEvent(ev.src, ev.node, pid, ev.event);
        // try {
        // EDProtocol prot = (EDProtocol) ev.node.getProtocol(pid);
        // prot.processEvent(ev.node, pid, ev.event);
        // } catch (ClassCastException e) {
        // throw new IllegalArgumentException("Protocol " +
        // Configuration.lookupPid(pid) +
        // " does not implement EDProtocol; " + ev.event.getClass() );
        // }
      }
    }
    return false;
  }



  // ---------------------------------------------------------------------
  // Public methods
  // ---------------------------------------------------------------------
  /**
   * Runs an experiment, resetting everything except the random seed.
   */
  public static void nextExperiment()
  {
    rbits = Configuration.getInt(PREFIX+"."+PAR_RBITS, 8);
    if (rbits<8||rbits>=64)
      throw new IllegalParameterException(PREFIX+"."+PAR_RBITS, "This parameter should be >= 8 or < 64");

    endtime = Configuration.getLong(PREFIX+"."+PAR_DURATION);
    CommonState.setEndTime(endtime);

    logtime = Configuration.getLong(PREFIX+"."+PAR_LOGTIME, Long.MAX_VALUE);

    // initialization
    System.err.println("Engine: resetting");  // XXX: change to debug() or notify()
    controls = null;
    controlSchedules = null;

    simHeap = new Heap(); // XXX single heap only in simulation
    nextlog = 0;
    Network.reset();

    System.err.println("Engine: running initializers");
    
    if (getType()==Type.SIM)
      CommonState.setTime(0); // needed here

    runInitializers();
    scheduleControls(simHeap);
    scheduleProtocols();

    // Perform the actual simulation; executeNext() will tell when to stop.
    boolean exit = false;
    while (!exit)
      exit = executeNext(simHeap);

    // analysis after the simulation
    CommonState.setPhase(CommonState.POST_SIMULATION);
    for (int j = 0; j<controls.length; ++j)
    {
      if (controlSchedules[j].fin)
        controls[j].execute();
    }
  }


  public static void startExperiment()
  {
    rbits = Configuration.getInt(PREFIX+"."+PAR_RBITS, 8);
    if (rbits<8||rbits>=64)
      throw new IllegalParameterException(PREFIX+"."+PAR_RBITS, "This parameter should be >= 8 or < 64");

    endtime = Configuration.getLong(PREFIX+"."+PAR_DURATION);
    if (CommonState.getEndTime()<0) // not initialized yet
      CommonState.setEndTime(endtime);

    logtime = Configuration.getLong(PREFIX+"."+PAR_LOGTIME, Long.MAX_VALUE);

    // initialization
    System.err.println("Engine: resetting");  // XXX: change to debug() or notify()
    controls = null;
    controlSchedules = null;

    //XXX heap = new Heap(); // XXX single heap only in simulation
    nextlog = 0;
    Network.reset();
    
    NodeThread[] nodeThreads = new NodeThread[Network.size()];
    for (int i=0; i<Network.size(); i++)
    {
      nodeThreads[i] = new NodeThread(Network.get(i));
    }

    Heap controlHeap = new Heap();
    
    System.err.println("Engine: running initializers");

    //XXX CommonState.setTime(0); // needed here
    runInitializers();
    scheduleControls(controlHeap);

    // Perform the actual simulation; executeNext() will tell when to stop.
//    boolean exit = false;
//    while (!exit)
//      exit = executeNext();

    // analysis after the simulation
    CommonState.setPhase(CommonState.POST_SIMULATION);
    for (int j = 0; j<controls.length; ++j)
    {
      if (controlSchedules[j].fin)
        controls[j].execute();
    }
  }

  public static class NodeThread extends Thread
  {
    Node node = null;
    Heap heap = null;

    public NodeThread(Node node)
    {
      this.node = node;
      this.heap = new Heap();
    }
    
    public void run()
    {
      boolean exit = false;
      while (!exit)
        exit = executeNext(heap);
      
    }
  }


  // ---------------------------------------------------------------------
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
  public static void add(long delay, Address src, Node node, int pid, Object event)
  {
    if (delay<0)
      throw new IllegalArgumentException("Protocol "+node.getProtocol(pid)+" is trying to add event "+event+
          " with a negative delay: "+delay);
    if (pid>Byte.MAX_VALUE)
      throw new IllegalArgumentException("This version does not support more than "+Byte.MAX_VALUE+" protocols");
    long time = CommonState.getTime()+delay;
    if (time>=endtime)
      return;
    time = (time<<rbits)|CommonState.r.nextInt(1<<rbits);

    Heap heap;
    if (getType() == Type.SIM)
      heap = simHeap;
    else
    {
      heap = simHeap; // XXX to be changed
    }
    heap.add(time, src, node, (byte) pid, event);
  }
  
  
  public static Engine instance()
  {
    if (instance==null)
    {
      if (getType()==Type.SIM)
        instance = new EngineSim();
      else
        instance = new EngineEmu();
    }
    return instance;
  }


  public void addNode()
  {
    
  }
}
