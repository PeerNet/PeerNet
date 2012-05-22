/*
 * Created on May 21, 2012 by Spyros Voulgaris
 */
package peernet.dynamics;

import java.net.InetAddress;
import java.net.UnknownHostException;

import peernet.config.Configuration;
import peernet.core.Control;
import peernet.core.Network;
import peernet.core.Node;
import peernet.core.Protocol;
import peernet.transport.Address;
import peernet.transport.AddressNet;





public class BootstrapStar implements Control
{
  private static final String PAR_ADDRESS = "address";
  private static final String PAR_PORT = "port";
  private int pid;
  private Address address;



  public BootstrapStar(String prefix)
  {
    try
    {
      pid = Configuration.getPid(prefix+"."+PAR_PROTOCOL);
      String host = Configuration.getString(prefix+"."+PAR_ADDRESS);
      int port = Configuration.getInt(prefix+"."+PAR_PORT);
      InetAddress ip;
      ip = InetAddress.getByName(host);
      address = new AddressNet(ip, port);
    }
    catch (UnknownHostException e)
    {
      e.printStackTrace();
    }
  }



  @Override
  public boolean execute()
  {
    for (int n = 0; n<Network.size(); n++)
    {
      Node node = Network.get(n);
      Protocol prot = node.getProtocol(pid);
      prot.addBootstrap(node, pid, address);
    }
    return false;
  }
}
