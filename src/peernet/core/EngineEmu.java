/*
 * Created on Apr 28, 2012 by Spyros Voulgaris
 *
 */
package peernet.core;

import java.util.concurrent.Semaphore;

import peernet.transport.Address;


public class EngineEmu extends Engine
{
  ExecutionThread controlThread = null;



  public class ExecutionThread extends Thread
  {
    Node node = null;
    Heap heap = null;
    Semaphore sem = null;

    public ExecutionThread(Node node)
    {
      this.heap = new Heap();
      this.node = node;
      this.sem = new Semaphore(1);
    }

    public void run()
    {
      boolean exit = false;

      long remainingTime;
      while (!exit)
      {
        Heap.Event event = null;
        synchronized (this)
        {
          while ((remainingTime = (heap.getEarliestTime()>>rbits) - CommonState.getTime()) > 0)
            try
            {
              //System.err.println("Thread "+node.getID()+" sleeping for "+(remainingTime));
              wait(remainingTime);
            }
            catch (InterruptedException e)
            {
              e.printStackTrace();
            }
          event = heap.removeFirst();
        }
        exit = executeNext(event);
      }
    }

    public Object clone()
    {
      return new Heap();
    }

    /**
     * Execute and remove the next event from the ordered event list.
     * 
     * @return true if the execution should be stopped.
     */
    private boolean executeNext(Heap.Event ev)
    {
      long time = ev.time>>rbits;
      if (time>=endtime) // XXX Should we also check here, or only when scheduling an event?
        return true;
    
      int pid = ev.pid;
      if (ev.node==null)  // XXX ugly way to identify control events
      {
        try {
          for (int n=0; n<Network.size(); n++)
          {
            Node node = Network.get(n);
            ExecutionThread thread = node.getThread();
            thread.sem.acquire();
          }
        }
        catch (InterruptedException e) {e.printStackTrace();}

        boolean ret = controls[pid].execute();

        for (int n=0; n<Network.size(); n++)
        {
          Node node = Network.get(n);
          ExecutionThread thread = node.getThread();
          thread.sem.release();
        }

        long delay = controlSchedules[pid].nextDelay(time);
        if (delay>=0)
          addAtTime(time+delay,  null, null, pid, null);
        return ret;
      }
      else if (ev.node.isUp())
      {
        assert ev.node != Network.prototype;
//        CommonState.setPid(pid);  // XXX try to entirely avoid CommonState
//        CommonState.setNode(ev.node);
        if (ev.event instanceof ScheduledEvent)
        {
          Protocol prot = ev.node.getProtocol(pid);

          try {sem.acquire();}
          catch (InterruptedException e) {e.printStackTrace();}

          prot.nextCycle(ev.node, pid);
          sem.release();

          long delay = prot.nextDelay();
          if (delay == 0)
            delay = protocolSchedules[pid].nextDelay(time);
    
          if (delay > 0)
            addAtTime(time+delay, null, ev.node, pid, scheduledEvent);
        }
        else // call Protocol.processEvent()
        {
          Protocol prot = ev.node.getProtocol(pid);

          try {sem.acquire();}
          catch (InterruptedException e) {e.printStackTrace();}
          prot.processEvent(ev.src, ev.node, pid, ev.event);
          sem.release();
        }
      }
      return false;
    }
  }


  @Override
  protected void createHeaps()
  {
    for (int n=0; n<Network.size(); n++)
    {
      Node node = Network.get(n);
      node.setThread(new ExecutionThread(node));
    }
    controlThread = new ExecutionThread(Network.prototype);
  }



  protected void executionLoop()
  {
    controlThread.start();
    for (int n=0; n<Network.size(); n++)
    {
      Node node = Network.get(n);
      node.getThread().start();
    }
//
//    // analysis after the simulation
//    CommonState.setPhase(CommonState.POST_SIMULATION);
//    for (int j = 0; j<controls.length; ++j)
//    {
//      if (controlSchedules[j].fin)
//        controls[j].execute();
//    }
  }
  
  public void addAtTime(long time, Address src, Node node, int pid, Object event)
  {
    if (time >= endtime)
      return;

    time = (time<<rbits) | CommonState.r.nextInt(1<<rbits);

    ExecutionThread thread = null;
    if (node == null)  // control event
      thread = controlThread;
    else
      thread = node.getThread();

    synchronized (thread)
    {
      thread.heap.add(time, src, node, (byte) pid, event);
      thread.notify();
    }
  }
  
  public int pendingEvents()
  {
    int events = 0;
    for (int n=0; n<Network.size(); n++)
      events += Network.get(n).getThread().heap.size();
    return events;
  }
}
