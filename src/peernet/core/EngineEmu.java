/*
 * Created on Apr 28, 2012 by Spyros Voulgaris
 *
 */
package peernet.core;

import peernet.transport.Address;


public class EngineEmu extends Engine
{
  ExecutionThread controlThread = null;



  public class ExecutionThread extends Thread
  {
    Node node = null;
    Heap heap = null;

    public ExecutionThread(Node node)
    {
      this.heap = new Heap();
      this.node = node;
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
          while ((remainingTime = heap.getEarliestTime()>>rbits - CommonState.getTime()) > 0)
            try
            {
              System.err.println("Thread "+node.getID()+" sleeping for "+(remainingTime));
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
//      node.getThread().start();
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
  
  /**
   * Execute and remove the next event from the ordered event list.
   * 
   * @return true if the execution should be stopped.
   */
  private boolean executeNext(Heap.Event ev)
  {
    long time = ev.time>>rbits;
    if (time>=endtime)
      return true;

    int pid = ev.pid;
    if (ev.node==null)  // control event; handled through a special method
    {
      boolean ret = controls[pid].execute();
      long delay = controlSchedules[pid].nextDelay(time);
      if (delay>=0 && delay+CommonState.getTime()<CommonState.getEndTime())
        add(delay,  null, null, pid, null);
      return ret;
    }
    else if (ev.node.isUp())
    {
      assert ev.node != Network.prototype;
      CommonState.setPid(pid);  // XXX try to entirely avoid CommonState
      CommonState.setNode(ev.node);
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

  @Override
  public void add(long delay, Address src, Node node, int pid, Object event)
  {
    if (delay<0)
    {
      System.err.println("Ignoring event with negative delay: "+delay);
      return;
    }

    System.err.println("Adding event with delay: "+delay);
      
//      throw new IllegalArgumentException("Protocol "+node.getProtocol(pid)+
//          " is trying to add event "+event+
//          " with a negative delay: "+delay);

    if (pid>Byte.MAX_VALUE)
      throw new IllegalArgumentException("This version does not support more than "+Byte.MAX_VALUE+" protocols");

    long time = CommonState.getTime()+delay;
    if (time>=endtime)
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
}
