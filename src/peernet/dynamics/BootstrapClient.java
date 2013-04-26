/*
 * Created on May 21, 2012 by Spyros Voulgaris
 */
package peernet.dynamics;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import peernet.config.Configuration;
import peernet.core.Control;
import peernet.core.Network;
import peernet.core.Node;
import peernet.core.Protocol;
import peernet.transport.Address;
import peernet.transport.AddressNet;





public class BootstrapClient extends TimerTask implements Control
{
  private final static String PAR_SERVER = "host"; //XXX change to addr:port
  private final static String PAR_PORT = "port";
  private final static String PAR_COORDINATOR = "coordinator";

  private static HashMap<String, BootstrapClient> map = new HashMap<String, BootstrapClient>();

  private int pid;
  private Address address;
  private String coordinatorName;
  private ArrayList<Node> remainingNodes;
  private Timer timer;

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

    map.put(coordinatorName, this);
  }



  public void run()
  {
    synchronized (remainingNodes)
    {
      System.out.println("run(), remaining: "+remainingNodes.size());
      Collections.shuffle(remainingNodes);
      for (Node node: remainingNodes)
      {
        // TODO: Check what happens if a node has died
        Protocol prot = node.getProtocol(pid);
        prot.send(address, pid, coordinatorName);
      }
    }
  }



  @Override
  public boolean execute()
  {
    for (int n = 0; n<Network.size(); n++)
      remainingNodes.add(Network.get(n));

    timer = new Timer();
    timer.schedule(this, 0, 2000);
    return false;
  }



  public static void report(String coordinatorName, Node node)
  {
    BootstrapClient b = map.get(coordinatorName);
    synchronized (b.remainingNodes)
    {
      b.remainingNodes.remove(node);
      if (b.remainingNodes.size() == 0)  // cancel periodic task
      {
        b.timer.cancel();
        System.out.println("CANCELLING TIMER");
      }
    }
  }
}