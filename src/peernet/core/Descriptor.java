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
 * A Descriptor represents a link between two nodes. It is the information
 * a node has about its neighbors.
 * 
 * Typically, the Descriptor should include the node's address, and other
 * information advertised along with it.
 * 
 * @author Spyros Voulgaris
 *
 */
public class Descriptor implements Serializable, Cloneable
{
  //XXX: add comments!

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
   * Default constructor
   */
	public Descriptor(Node node, int pid)
	{
	  if (Engine.getAddressType() == AddressType.NET)
	    address = new AddressNet(localhost, ((TransportUDP)node.getTransport(0)).getPort()); //FIXME: change 0 to pid-something
	  else
	    address = new AddressSim(node); //XXX: added 'node' while working on powerlaw
	}



  /**
   * This constructor should be used to initialize descriptors of foreign peers,
   * therefore it should initialize all fields to generic value, not to values
   * of the calling node.
   */
  public Descriptor(Address address)  // TODO: lower the accessibility of Descriptor's constructors
  {
    this.address = address;
  }



  /**
   * Returns the ID of the node referenced by this descriptor.
   * 
   * IMPORTANT: IDs are unique for each node, both for single
   * machine simulations and distributed emulations.
   */
  //public abstract long getID();



  /**
	 * Checks whether two descriptors refer to the same node.
	 * The equality check is performed based on IDs, as they are unique.
	 * 
	 * @param otherDescriptor
	 * @return
	 */
	public boolean equals(Object otherDescriptor)
	{
	  return address.equals( ((Descriptor)otherDescriptor).address );
	}



	/**
	 * Custom hashCode() to be consistent with equals().
	 */
  public int hashCode()
  {
    return address.hashCode();
  }



	/**
	 * clone()
	 */
  public Object clone()
  {
    Descriptor descriptor = null;
    try
    {
      descriptor = (Descriptor)super.clone();
    }
    catch (CloneNotSupportedException e)
    {
      System.out.println(e);
    }
    descriptor.address = (Address)address.clone();
    return descriptor;
  }
}