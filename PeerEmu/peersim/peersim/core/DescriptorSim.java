/*
 * Created on Jul 16, 2007 by Spyros Voulgaris
 *
 */
package peersim.core;

public class DescriptorSim implements Descriptor
{
	/**
	 * Keeps a reference to the respective Node object.
	 * 
	 * In single-machine simulated environments, a reference to Node is
	 * sufficient to fully describe the node.
	 */
	private final Node node;


	public DescriptorSim(Node node)
	{
		this.node = node;
	}


	public long getID()
	{
		return node.getID();
	}


	public boolean equals(Object otherDescriptor)
	{
		return getID() == ((DescriptorSim)otherDescriptor).getID();
	}
}
