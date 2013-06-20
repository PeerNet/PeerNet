/*
 * Created on Nov 24, 2012 by Spyros Voulgaris
 */
package peernet.dynamics;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;

import peernet.config.Configuration;
import peernet.core.Descriptor;
import peernet.graph.NeighborListGraph;
import peernet.transport.Packet;
import peernet.transport.TransportUDP;





public class Coordinator
{
  private final static String PAR_PREFIX = "coordinator";
  private final static String PAR_TRANSPORT = "transport";
  private final static String PAR_SIZE = "size";
  private final static String PAR_TIMEOUT = "timeout";
  private final static String PAR_INIT = "init";

  private static TransportUDP transport = null;

  private int numNodes;
  private int timeout;
  private boolean completed;
  private int pid = -1;
  private NeighborListGraph graph;
  private WireControl initializer = null;
  private HashSet<Descriptor> descriptorSet;

  private String name;



  private Coordinator(String prefix)
  {
    name = prefix.substring(prefix.lastIndexOf('.') + 1);
    numNodes = Configuration.getInt(prefix+"."+PAR_SIZE, Integer.MAX_VALUE);
    timeout = Configuration.getInt(prefix+"."+PAR_TIMEOUT, -1);

    initializer = (WireControl) Configuration.getInstance(prefix+"."+PAR_INIT);

    graph = new NeighborListGraph(true); // new directed graph
    descriptorSet = new HashSet<Descriptor>();

    Timer timer = new Timer();
    timer.schedule(new TimerTask()
    {
      public void run()
      {
        if (!completed)
        {
          System.out.println("Timeout expired. Received messages from "+graph.size()+" nodes");
          notifyNodes();
        }
      }
    }, timeout);
  }


  private int count;
  public synchronized void registerPeerAddress(Descriptor descr, int pid)
  {
    count++;
    if (this.pid == -1) // not set yet
      this.pid = pid;
    assert this.pid == pid; // once set, all incoming requests should be for the same pid

    if (!completed)
    {
      if (!descriptorSet.contains(descr))
      {
        descriptorSet.add(descr);

        // Add this node to the storage of this coordinator
        graph.addNode(descr);
        // Check if we received sufficient nodes for this coordinator to close it
        if (graph.size()==numNodes)
        {
          System.out.println("Received messages from all nodes ("+numNodes+")");
          notifyNodes();
        }
        System.out.println("---> "+graph.size()+" "+count);
      }
      else
        System.out.println("Duplicate: "+descr.address);
    }
    else
      System.out.println("Completed!! "+graph.size()+" "+count);
  }



  private synchronized void notifyNodes()
  {
    // First, set coordinator to completed
    completed = true;

    // Now run the appropriate wiring algorithm to design the overlay
    initializer.setGraph(graph);
    initializer.execute();

    // Finally, send messages to nodes to inform them of their neighbor lists
    for (int i = 0; i<graph.size(); i++)
    {
      Descriptor descr = (Descriptor) graph.getNode(i);
      Collection<Integer> neighbors = graph.getNeighbours(i);
      BootstrapList list = new BootstrapList();
      list.coordinatorName = name;
      list.descriptors = new Descriptor[neighbors.size()];

      System.out.print(descr.address+":\t");
      int j=0;
      for (int n: neighbors)
      {
        Descriptor d = (Descriptor) graph.getNode(n);
        list.descriptors[j++] = d;
        System.out.print(d.address+"  ");
      }
      System.out.println();

      transport.send(null, descr.address, pid, list);
    }
  }



  public static void start()
  {
    transport = new TransportUDP(PAR_TRANSPORT);
    transport = (TransportUDP) transport.clone();
    System.out.println("Coordinator listening at port "+transport.getPort());

    HashMap<String, Coordinator> map = new HashMap<String, Coordinator>();

    while (true)
    {
      Packet packet = transport.receive();
      BootstrapList bl = (BootstrapList) packet.event;
      System.out.println("received from: "+packet.src);

      // If no coordinator has been created for this bootstrapId, create it
      if (!map.containsKey(bl.coordinatorName))
      {
        Coordinator coordinator = new Coordinator(PAR_PREFIX+"."+bl.coordinatorName);
        map.put(bl.coordinatorName, coordinator);
      }

      // Fetch the appropriate coordinator from the HashMap.
      Coordinator coordinator = map.get(bl.coordinatorName);
      assert bl.descriptors.length == 1;  // a node must have sent only its own descriptor
      Descriptor descr = bl.descriptors[0];
      descr.address = packet.src;
      coordinator.registerPeerAddress(descr, packet.pid);
    }
  }
}
