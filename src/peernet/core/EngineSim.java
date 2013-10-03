/*
 * Created on Apr 28, 2012 by Spyros Voulgaris
 *
 */
package peernet.core;

import peernet.transport.Address;


public class EngineSim extends Engine
{
  Heap simHeap = null;



  @Override
  public void startExperiment()
  {
    super.startExperiment();

    // Perform the actual simulation; executeNext() will tell when to stop.
    boolean exit = false;
    while (!exit)
      exit = executeNext(simHeap);

    // analysis after the simulation
    //CommonState.setPhase(CommonState.POST_SIMULATION);
    for (int j = 0; j<controls.length; ++j)
    {
      if (controlSchedules[j].fin)
        controls[j].execute();
    }
  }

  /**
   * Execute and remove the next event from the ordered event list.
   * 
   * @return true if the execution should be stopped.
   */
  private boolean executeNext(Heap heap)
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
    if (ev.node==null)  //XXX: Not an elegant way to identify control events
    {
      boolean ret = controls[pid].execute();
      long delay = controlSchedules[pid].nextDelay(time);
      if (delay>=0)
        add(delay,  null, null, pid, null);
      return ret;
    }
    else if (ev.node.isUp())
    {
//      CommonState.setPid(pid);  // XXX try to entirely avoid CommonState
//      CommonState.setNode(ev.node);
      if (ev.event instanceof ScheduledEvent)
      {
        Protocol prot = ev.node.getProtocol(pid);
        prot.nextCycle(ev.node, pid);

        long delay = prot.nextDelay();
        if (delay == 0)
          delay = protocolSchedules[pid].nextDelay(time);

        if (delay > 0)
          add(delay, null, ev.node, pid, scheduledEvent);
      }
      else // call Protocol.processEvent()
      {
        Protocol prot = ev.node.getProtocol(pid);
        prot.processEvent(ev.src, ev.node, pid, ev.event);
      }
    }
    return false;
  }

  public void addAtTime(long time, Address src, Node node, int pid, Object event)
  {
    time = (time<<rbits) | CommonState.r.nextInt(1<<rbits);
    simHeap.add(time, src, node, (byte) pid, event);
  }

  @Override
  protected void createHeaps()
  {
    simHeap = new Heap();
  }
  
  public int pendingEvents()
  {
    return simHeap.size();
  }
}
