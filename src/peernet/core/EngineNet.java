/*
 * Created on May 14, 2012 by Spyros Voulgaris
 *
 */
package peernet.core;

import peernet.config.FastConfig;
import peernet.transport.Packet;
import peernet.transport.TransportNet;


public class EngineNet extends EngineEmu
{
  public void startExperiment()
  {
    super.startExperiment();

    int tid = FastConfig.getTransport(0);
    TransportNet transport = (TransportNet) Network.get(0).getProtocol(tid);

    for (int n=0; n<Network.size(); n++)
    {
      Node node = Network.get(n);
      new ListeningThread(node, node.getHeap(), transport).start();
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
        synchronized (heap)
        {
          heap.add(0, packet.src, node, (byte)packet.pid, packet.event);
          heap.notify();
        }
      }
    }
  }
}
