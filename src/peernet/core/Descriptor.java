/*
 * Created on Jul 16, 2007 by Spyros Voulgaris
 *
 */
package peernet.core;

import java.io.Serializable;

import peernet.core.Engine.AddressType;
import peernet.transport.Address;
import peernet.transport.AddressNet;
import peernet.transport.AddressSim;

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
public class Descriptor implements Serializable
{
  private static final long serialVersionUID = 1;

  /**
   * This is the address where this node can be contacted.
   */
  public Address address;



  /**
   * Default constructor
   */
	public Descriptor()
	{
	  if (Engine.getAddressType() == AddressType.NET)
	    address = new AddressNet(null, 0); //FIXME
	  else
	    address = new AddressSim();
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
	 * clone()
	 */
  public Object clone() throws CloneNotSupportedException
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