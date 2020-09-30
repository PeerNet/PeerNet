/*
 * Created on May 21, 2012 by Spyros Voulgaris
 */
package peernet.dynamics;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import peernet.config.Configuration;
import peernet.core.CommonState;
import peernet.core.Control;
import peernet.core.Peer;
import peernet.core.Engine;
import peernet.core.Linkable;
import peernet.core.Network;
import peernet.core.Node;
import peernet.core.Protocol;
import peernet.dynamics.BootstrapServer.BootstrapMessage;
import peernet.dynamics.BootstrapServer.BootstrapMessage.Type;
import peernet.transport.Address;
import peernet.transport.AddressNet;





public class BootstrapClient extends TimerTask implements Control
{
  private final static String PAR_SERVER = "host"; //XXX change to addr:port
  private final static String PAR_PORT = "port";
  private final static String PAR_COORDINATOR = "coordinator";
  private final static String PAR_SEQUENTIAL_ID = "SEQ";

  private static HashMap<String, BootstrapClient> map = new HashMap<String, BootstrapClient>();

  private int pid;
  private Address address;
  private String coordinatorName;
  private ArrayList<Node> remainingNodes;
  private Timer timer;
  private int seq;
  private int numNodes = -1;

  private HashSet<Long> bootstrappedNodes;

  public BootstrapClient(String prefix)
  {
    try
    {
      pid = Configuration.getPid(prefix+"."+PAR_PROTOCOL);
      String serverAddress = Configuration.getString(prefix+"."+PAR_SERVER);
      InetAddress ip = InetAddress.getByName(serverAddress);

      int serverPort = Configuration.getInt(prefix+"."+PAR_PORT);
      address = new AddressNet(ip, serverPort);
    }
    catch (UnknownHostException e)
    {
      e.printStackTrace();
    }

    remainingNodes = new ArrayList<Node>();
    coordinatorName = Configuration.getString(prefix+"."+PAR_COORDINATOR, "");
    bootstrappedNodes = new HashSet<Long>();
    seq = Configuration.getInt(PAR_SEQUENTIAL_ID, -1);

    map.put(coordinatorName, this);
  }



  public void run()
  {
    synchronized (remainingNodes)
    {
      System.out.println("Worker: "+seq+"  remaining nodes: "+remainingNodes.size());
//      try
//      {
//        Thread.sleep(System.currentTimeMillis() % 1000);
//      }
//      catch (InterruptedException e)
//      {
//        // TODO Auto-generated catch block
//        e.printStackTrace();
//      }
      Collections.shuffle(remainingNodes);
      BootstrapMessage msg = new BootstrapMessage(Type.REQUEST);
      msg.coordinatorName = coordinatorName;
      msg.peers = new Peer[1];
      for (Node node: remainingNodes)
      {
        if (node.getID() % 3==0)
        {
          try
          {
            Thread.sleep(1);
          }
          catch (InterruptedException e)
          {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        }

        // TODO: Check what happens if a node has died
        Protocol prot = node.getProtocol(pid);
        msg.peers[0] = prot.myself();
        prot.send(address, pid, msg);
      }
    }
  }



  @Override
  public boolean execute()
  {
    System.out.println("STARTING BootstrapClient.execute()");
    Engine.instance().blockingInitializerStart();

    numNodes = Network.size();
    for (int n = 0; n<Network.size(); n++)
      remainingNodes.add(Network.get(n));

    Random r = new Random(System.currentTimeMillis());
    int timeOffset = r.nextInt(2000);
//    timeOffset = 0; //XXX remove this!!

    timer = new Timer();
    timer.schedule(this, timeOffset, 2000);
    return false;
  }



  public static void report(Node node, BootstrapMessage msg)
  {
    BootstrapClient b = map.get(msg.coordinatorName);

    switch (msg.type)
    {
      case REQUEST:
        assert false;

      case REQUEST_ACK:
        node.setID(msg.nodeId);

        synchronized (b.remainingNodes)
        {
          b.remainingNodes.remove(node);
          if (b.remainingNodes.size() == 0)  // cancel periodic task
            b.timer.cancel();
        }
        break;

      case RESPONSE:
        node.setID(msg.nodeId);

        Protocol prot = node.getProtocol(b.pid);

        synchronized (b.bootstrappedNodes)
        {
          if (!b.bootstrappedNodes.contains(node.getID())) // not bootstrapped yet
          {
            b.bootstrappedNodes.add(node.getID());
            node.acquireLock();
            for (Peer d: msg.peers)
              ((Linkable)prot).addNeighbor(d);
            node.releaseLock();

            if (b.bootstrappedNodes.size() == b.numNodes)
              Engine.instance().blockingInitializerDone();
          }
        }

        BootstrapMessage ack = new BootstrapMessage(Type.RESPONSE_ACK);
        ack.coordinatorName = msg.coordinatorName;
        ack.peers = null;

        try
        {
          Thread.sleep(CommonState.r.nextInt(1000));
        }
        catch (InterruptedException e)
        {
          e.printStackTrace();
        }
        prot.send(b.address, b.pid, ack);
        break;

      case RESPONSE_ACK:
        assert false;

      default:
        throw new RuntimeException("Received BootstrapMessage of unknown type: "+msg.type);
    }
  }
}
