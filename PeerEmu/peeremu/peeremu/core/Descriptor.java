/*
 * Created on Jul 16, 2007 by Spyros Voulgaris
 *
 */
package peeremu.core;

/**
 * A Descriptor represents a link between two nodes. It is the information
 * a node has about its neighbors.
 * 
 * Typically, the Descriptor should the node's address, and other information
 * advertised along with it.
 * 
 * @author Spyros Voulgaris
 *
 */
public interface Descriptor extends Cloneable
{
	/**
	 * Returns the ID of the node referenced by this descriptor.
	 * 
	 * IMPORTANT: IDs are unique for each node, both for single
	 * machine simulations and distributed emulations.
	 */
	public long getID();

	/**
	 * Checks whether two descriptors refer to the same node.
	 * The equality check is performed based on IDs, as they are unique.
	 * 
	 * @param otherDescriptor
	 * @return
	 */
	public boolean equals(Object otherDescriptor);

  public Object clone() throws CloneNotSupportedException;
	
  /**
   * Returns the node referred by the descriptor.
   * 
   * However, it should be avoided, because it makes no sense in Internet
   * Emulation mode. To be used only for convenience 
   * 
   * @param otherDescriptor
   * @return
   */
//  public Node getNode();
}