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
import peernet.graph.NeighborListGraph;
import peernet.transport.Address;
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
  private HashSet<Address> addresses;

  private String name;



  private Coordinator(String prefix)
  {
    name = prefix.substring(prefix.lastIndexOf('.') + 1);
    numNodes = Configuration.getInt(prefix+"."+PAR_SIZE, Integer.MAX_VALUE);
    timeout = Configuration.getInt(prefix+"."+PAR_TIMEOUT, -1);

    initializer = (WireControl) Configuration.getInstance(prefix+"."+PAR_INIT);

    graph = new NeighborListGraph(true); // new directed graph
    addresses = new HashSet<Address>();

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
  public synchronized void registerPeerAddress(Address address, @SuppressWarnings("hiding") int pid)
  {
    count++;
    if (this.pid == -1) // not set yet
      this.pid = pid;
    assert this.pid == pid; // once set, all incoming requests should be for the same pid

    if (!completed)
    {
      if (!addresses.contains(address))
      {
        // Add this node to the storage of this coordinator
        graph.addNode(address);
        // Check if we received sufficient nodes for this coordinator to close
        // it
        if (graph.size()==numNodes)
        {
          System.out.println("Received messages from all nodes ("+numNodes+")");
          notifyNodes();
        }
        addresses.add(address);
        System.out.println("---> "+graph.size()+" "+count);
      }
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
      Address address = (Address) graph.getNode(i);
      Collection<Integer> neighbors = graph.getNeighbours(i);
      BootstrapList list = new BootstrapList();
      list.coordinatorName = name;
      list.addresses = new Address[neighbors.size()];

      int j=0;
      for (int n: neighbors)
        list.addresses[j++] = (Address) graph.getNode(n);

      transport.send(null, address, pid, list);
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
      assert packet.event instanceof String;
      String name = (String) packet.event;
      // If no coordinator has been created for this bootstrapId, create it
      if (!map.containsKey(name))
      {
        Coordinator coordinator = new Coordinator(PAR_PREFIX+"."+name);
        map.put(name, coordinator);
      }
      // Fetch the appropriate coordinator from the HashMap.
      Coordinator coordinator = map.get(name);
      //assert packet.pid==coordinator.pid;
      coordinator.registerPeerAddress(packet.src, packet.pid);
    }
  }
}
