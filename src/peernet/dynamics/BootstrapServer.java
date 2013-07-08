/*
 * Created on Nov 24, 2012 by Spyros Voulgaris
 */
package peernet.dynamics;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Timer;
import java.util.Vector;

import peernet.config.Configuration;
import peernet.core.Control;
import peernet.core.Descriptor;
import peernet.dynamics.BootstrapServer.BootstrapMessage.Type;
import peernet.graph.NeighborListGraph;
import peernet.transport.Address;
import peernet.transport.AddressNet;
import peernet.transport.Packet;
import peernet.transport.TransportUDP;





public class BootstrapServer
{
  /**
   * Coordinator prefix
   */
  private final static String PAR_PREFIX = "coordinator";

  /**
   * Tranport prefix
   */
  private final static String PAR_TRANSPORT = "transport";

  /**
   * Minimum number of nodes to expect before creating the topology.
   */
  private final static String PAR_SIZE = "nodes";

  /**
   * Timeout for creating the topology, counted from the moment the first node
   * registers. When this timeout expires, the topology is generated with any
   * nodes that have registered, although they are fewer than anticipated.
   */
  private final static String PAR_TIMEOUT = "timeout";

  /**
   * The initializer {@link Control} in charge for initializing this topology.
   */
  private final static String PAR_INIT = "init";

  private static TransportUDP transport = null;
  static long ID = 0;

  private HashMap<Address, Long> addressToIdMap = new HashMap<Address, Long>();
  private HashMap<String, Coordinator> map = new HashMap<String, Coordinator>();

  AddressNet addr;
  Timer timer;



  public BootstrapServer()
  {
    transport = new TransportUDP(PAR_TRANSPORT);
    transport = (TransportUDP) transport.clone();

    new Thread(new NetworkListener()).start();
  }



  protected long assignNodeId(Descriptor descr)
  {
    return ID++;
  }



  private void setNodeId(Descriptor descr)
  {
    Long assignedId = addressToIdMap.get(descr.address);
    if (assignedId==null)
    {
      assignedId = assignNodeId(descr);
      addressToIdMap.put(descr.address, assignedId);
    }
    descr.ID = assignedId;
  }



  private class Coordinator
  {
    private int numNodes;
    private int timeout;
    private boolean completed;
    private int pid = -1;
    private NeighborListGraph graph;
    private WireControl initializer = null;
    private HashMap<Address, Integer> uninformedNodes;
    private String name;
    
    private int responsesRound;
    private int acksTotal;
    private int acksRound;


    private Coordinator(String prefix)
    {
      name = prefix.substring(prefix.lastIndexOf('.')+1);
      numNodes = Configuration.getInt(prefix+"."+PAR_SIZE);
      timeout = Configuration.getInt(prefix+"."+PAR_TIMEOUT, -1);
      initializer = (WireControl) Configuration
          .getInstance(prefix+"."+PAR_INIT);
      graph = new NeighborListGraph(true); // new directed graph
      uninformedNodes = new HashMap<Address, Integer>();
      timer = new Timer();
    }



    private void printProgress()
    {
      System.out.print("\r"+addressToIdMap.size()+"\t"+acksTotal+'\t'+responsesRound+'\t'+acksRound);
    }



    private void process(Address src, int pid, BootstrapMessage msg)
    {
      switch (msg.type)
      {
        case REQUEST:
          if (this.pid==-1) // not set yet
            this.pid = pid;

          assert this.pid==pid; // once set, all incoming requests should be for
                                // the same pid

          // each node is supposed to send only its own descriptor
          assert msg.descriptors.length==1;

          Descriptor descr = msg.descriptors[0];
          descr.address = src; // set the node's address in its descriptor

          // Set the node ID in its address
          setNodeId(descr);

          // register the node in local topology structure
          registerNewNode(descr);

          // Send ACK so that the node stops trying to register
          BootstrapMessage ack = new BootstrapMessage(Type.REQUEST_ACK);
          ack.nodeId = descr.getID();
          ack.descriptors = null;
          ack.coordinatorName = msg.coordinatorName;
          transport.send(null, descr.address, pid, ack);
          break;

        case RESPONSE_ACK:
          // Ok, this node is now informed. Stop sending it bootstrap information.
          if (uninformedNodes.remove(src) != null)
          {
            acksTotal++;
            acksRound++;
            printProgress();
          }
          break;

        case REQUEST_ACK:
          assert false;

        case RESPONSE:
          assert false;

        default:
          break;
      }
    }



    private synchronized void registerNewNode(Descriptor descr)
    {
      if (!completed)
      {
//        if (uninformedNodes.isEmpty()) // this is the first node registering
//        {
//          new Timer().schedule(new TimerTask()
//          {
//            public void run()
//            {
//              if (!completed)
//              {
//                System.out
//                    .println("\n\nTimeout expired. Received messages from "+
//                        graph.size()+" nodes");
//                buildTopology();
//              }
//            }
//          }, timeout);
//        }
        if (!uninformedNodes.containsKey(descr.address))
        {
          int index = graph.addNode(descr); // Add node to local overlay
          uninformedNodes.put(descr.address, index); // Add address to list of uninformed nodes

          printProgress();

          if (!completed && graph.size()==numNodes) // If all nodes have registered,
          {
            completed = true;
            buildTopology(); // build the topology and notify them.
          }
        }
      }
      else
      {
        // deal with late comers
      }
    }



    private synchronized void buildTopology()
    {
      // Run the appropriate wiring algorithm to design the overlay
      initializer.setGraph(graph);
      initializer.execute();

      // And finally start the thread that informs all nodes of their neighbors
      new Thread(new ResponseSender()).start();
    }



    private void sendResponses()
    {
      while (true)
      {
        Vector<Integer> indexes;
        synchronized (uninformedNodes)
        {
          indexes = new Vector<Integer>(uninformedNodes.values());
        }

        System.out.println("\nGoing to send "+indexes.size()+" responses.");
        acksRound = 0;
        responsesRound = indexes.size();

        for (int index: indexes)
        {
          Descriptor descr = (Descriptor) graph.getNode(index);
          Collection<Integer> neighbors = graph.getNeighbours(index);

          BootstrapMessage msg = new BootstrapMessage(Type.RESPONSE);
          msg.coordinatorName = name;
          msg.nodeId = descr.getID();
          msg.descriptors = new Descriptor[neighbors.size()];

          int j = 0;
          for (int n: neighbors)
          {
            Descriptor d = (Descriptor) graph.getNode(n);
            msg.descriptors[j++] = d;
          }

          transport.send(null, descr.address, pid, msg);
        }

        try
        {
          Thread.sleep(10000);
        }
        catch (InterruptedException e)
        {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }



    private class ResponseSender implements Runnable
    {
      @Override
      public void run()
      {
        sendResponses();
        try
        {
          Thread.sleep(2000);
        }
        catch (InterruptedException e)
        {
          e.printStackTrace();
        }
      }
    }
  }



  public class NetworkListener implements Runnable
  {
    @Override
    public void run()
    {
      while (true)
      {
        Packet packet = transport.receive();
        if (!(packet.event instanceof BootstrapMessage))
          continue;
        BootstrapMessage msg = (BootstrapMessage) packet.event;
        if (msg.type!=Type.REQUEST&&msg.type!=Type.RESPONSE_ACK)
        {
          System.out.println(" *** Received "+msg.type+" from "+packet.src);
          continue;
        }
        // If no coordinator has been created for this bootstrapId, create it
        if (!map.containsKey(msg.coordinatorName))
        {
          Coordinator coordinator = new Coordinator(PAR_PREFIX+"."+msg.coordinatorName);
          map.put(msg.coordinatorName, coordinator);
          System.out.println("BootstrapServer listening at port "+
              transport.getPort()+", waiting for "+coordinator.numNodes+
              " nodes, or "+coordinator.timeout+" milliseconds.");
        }
        // Fetch the appropriate coordinator from the HashMap.
        Coordinator coordinator = map.get(msg.coordinatorName);
        coordinator.process(packet.src, packet.pid, msg);
      }
    }
  }



  public static class BootstrapMessage implements Serializable
  {
    private static final long serialVersionUID = 7821791956620397834L;

    public enum Type {
      REQUEST, REQUEST_ACK, RESPONSE, RESPONSE_ACK
    }

    public Type type;
    public String coordinatorName;
    public long nodeId;
    public Descriptor[] descriptors;


    public BootstrapMessage(Type type)
    {
      this.type = type;
    }
  }
}
