/*
 * Created on Jul 16, 2007 by Spyros Voulgaris
 *
 */
package peernet.core;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;

import peernet.core.Engine.AddressType;
import peernet.transport.Address;
import peernet.transport.AddressNet;
import peernet.transport.AddressSim;
import peernet.transport.TransportUDP;

/**
 * A Peer represents a link between two nodes. It is the information
 * a node has about its neighbors.
 * 
 * Typically, the Peer should include the node's address, and other
 * information advertised along with it.
 * 
 * @author Spyros Voulgaris
 *
 */
public class Peer implements Serializable, Cloneable
{
  private static final long serialVersionUID = 1;
  public static InetAddress localhost;

  static
  {
    try
    {
      localhost  = InetAddress.getLocalHost();
    }
    catch (UnknownHostException e)
    {
      e.printStackTrace();
    }
  }
  
//  static
//  {
//    Enumeration<NetworkInterface> ifaces;
//    try
//    {
//      ifaces = NetworkInterface.getNetworkInterfaces();
//      for (NetworkInterface iface: Collections.list(ifaces))
//      {
//        if (!iface.getDisplayName().startsWith("eth"))
//          continue;
//
////        Enumeration<NetworkInterface> virtualIfaces = iface.getSubInterfaces();
////        for (NetworkInterface viface: Collections.list(virtualIfaces))
////        {
////          System.out.println(iface.getDisplayName()+" VIRT "+viface.getDisplayName());
////          Enumeration<InetAddress> vaddrs = viface.getInetAddresses();
////          for (InetAddress vaddr: Collections.list(vaddrs))
////          {
////            System.out.println("\t"+vaddr.toString());
////          }
////        }
//        System.out.println("Real iface addresses: "+iface.getDisplayName());
//        Enumeration<InetAddress> raddrs = iface.getInetAddresses();
//        for (InetAddress raddr: Collections.list(raddrs))
//          if (raddr instanceof java.net.Inet4Address)
//          {
//            localhost = raddr;
//            break;
//          }
//        
//        if (localhost != null)
//          break;
//      }
//      assert localhost != null: "Did not find local eth0 address";
//    }
//    catch (SocketException e)
//    {
//      e.printStackTrace();
//    }
//  }
  /**
   * This is the address where this node can be contacted.
   */
  public Address address;

  
  
  /**
   * The ID of the reference node.
   */
  public long ID;



  /**
   * Default constructor
   */
	public Peer(Node node, int pid)
	{
	  // set the address
	  if (Engine.getAddressType() == AddressType.NET)
	    address = new AddressNet(localhost, ((TransportUDP)node.getTransport(0)).getPort()); //FIXME: change 0 to pid-something
	  else
	    address = new AddressSim(node);

	  // set the ID
	  ID = node.getID();
	}



  /**
   * Returns the ID of the node referenced by this Peer.
   * 
   * IMPORTANT: IDs are unique for each node, both for single
   * machine simulations and distributed emulations.
   */
  public long getID()
  {
    return ID;
//    if (address instanceof AddressSim)
//      return ((AddressSim)address).node.getID();
//    else
//      return ((AddressNet)address).ID;
  }



  /**
	 * Checks whether two Peers refer to the same node.
	 * The equality check is performed based on IDs, as they are unique.
	 * 
	 * @param otherPeer
	 * @return True if peers refer to the same node, false otherwise.
	 */
	public boolean equals(Object otherPeer)
	{
	  return address.equals( ((Peer)otherPeer).address );
	}



	/**
	 * Custom hashCode() to be consistent with equals().
	 */
  public /*final*/ int hashCode()
  {
    return address.hashCode();
  }



	/**
	 * clone()
	 */
  public Object clone()
  {
    Peer peer = null;
    try
    {
      peer = (Peer)super.clone();
    }
    catch (CloneNotSupportedException e)
    {
      System.out.println(e);
    }
    peer.address = (Address)address.clone();
    return peer;
  }



  public String toString()
  {
    return ""+ID;
  }
}