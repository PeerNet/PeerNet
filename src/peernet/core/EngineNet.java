/*
 * Created on Apr 28, 2012 by Spyros Voulgaris
 *
 */
package peernet.core;

import peernet.dynamics.BootstrapClient;
import peernet.dynamics.BootstrapServer.BootstrapMessage;
import peernet.transport.Address;
import peernet.transport.Packet;
import peernet.transport.TransportNet;


public class EngineNet extends Engine
{
  Heap controlHeap = null;


  @Override
  protected void createHeaps()
  {
    // one heap per node
    for (int n=0; n<Network.size(); n++)
      Network.get(n).setHeap(new Heap());

    // and one heap for all controls together
    controlHeap = new Heap();
  }



  @Override
  public void startExperiment()
  {
    super.startExperiment();

    if (getType()==Type.NET)
    {
      for (int n=0; n<Network.size(); n++)
      {
        Node node = Network.get(n);
        for (int j=0; j<node.getTransports(); j++)
        {
          new ListeningThread(node, node.getHeap(), (TransportNet) node.getTransport(j)).start();
        }
      }
    }

    for (int n=0; n<Network.size(); n++)
    {
      Node node = Network.get(n);
      node.initLock();
      new ExecutionThread(node.getHeap()).start();
    }

    new ExecutionThread(controlHeap).start();

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

    Heap heap = null;
    if (node == null)  // control event
      heap = controlHeap;
    else
      heap = node.getHeap();

    synchronized (heap)
    {
      heap.add(time, src, node, (byte) pid, event);
      heap.notify();
    }
  }

  public int pendingEvents()
  {
    int events = 0;
    for (int n=0; n<Network.size(); n++)
      events += Network.get(n).getHeap().size();
    return events;
  }



  /**
   * Execute and remove the next event from the ordered event list.
   * 
   * @return true if the execution should be stopped.
   */
  protected boolean executeNext(Heap.Event ev)
  {
    long time = ev.time>>rbits;
    if (time>=endtime) // XXX Should we also check here, or only when scheduling an event?
      return true;

    int pid = ev.pid;
    if (ev.node==null)  // XXX ugly way to identify control events
    {
      for (int n=0; n<Network.size(); n++) //XXX The network size might change in the meantime
        Network.get(n).acquireLock();

      boolean ret = controls[pid].execute();

      for (int n=0; n<Network.size(); n++)
        Network.get(n).releaseLock();

      long delay = controlSchedules[pid].nextDelay(time);
      if (delay>=0)
        addAtTime(time+delay, null, null, pid, null);
      return ret;
    }
    else if (ev.node.isUp())
    {
//        CommonState.setPid(pid);  // XXX try to entirely avoid CommonState
//        CommonState.setNode(ev.node);

      Protocol prot = ev.node.getProtocol(pid);

      if (ev.event instanceof ScheduledEvent)
      {
        ev.node.acquireLock();
        prot.nextCycle(ev.node, pid);
        ev.node.releaseLock();

        long delay = prot.nextDelay();
        if (delay == 0)
          delay = protocolSchedules[pid].nextDelay(time);

        if (delay > 0)
          addAtTime(time+delay, null, ev.node, pid, scheduledEvent);
      }

      else if (ev.event instanceof BootstrapMessage)
      {
        BootstrapClient.report(ev.node, (BootstrapMessage)ev.event);
      }

      else // call Protocol.processEvent()
      {
        ev.node.acquireLock();
        prot.processEvent(ev.src, ev.node, pid, ev.event);
        ev.node.releaseLock();
      }
    }
    return false;
  }



  public class ExecutionThread extends Thread
  {
    private Heap heap = null;

    public ExecutionThread(Heap heap)
    {
      this.heap = heap;
    }



    public void run()
    {
      boolean exit = false;

      long remainingTime;
      while (!exit)
      {
        Heap.Event event = null;
        synchronized (heap)
        {
          while ( (remainingTime = (heap.getNextTime()>>rbits) - CommonState.getTime()) > 0)
          {
            try
            {
              heap.wait(remainingTime);
            }
            catch (InterruptedException e)
            {
              e.printStackTrace();
            }
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
  
  
  public class ListeningThread extends Thread
  {
    Node node = null;
    Heap heap = null;
    TransportNet transport = null;

    public ListeningThread(Node node, Heap heap, TransportNet transport)
    {
      this.node = node;
      this.heap = heap;
      this.transport = transport;
    }

    public void run()
    {
      while (true)
      {
        Packet packet = transport.receive();

        assert packet!=null : "packet is null!";
        assert packet.src!=null : "packet.src is null!";
        assert packet.event!=null : "packet.event is null!";
        synchronized (heap)
        {
          heap.add(0, packet.src, node, (byte)packet.pid, packet.event);
          heap.notify();
        }
      }
    }
  }
}
