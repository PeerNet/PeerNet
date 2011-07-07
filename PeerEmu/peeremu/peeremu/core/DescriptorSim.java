/*
 * Created on Jul 16, 2007 by Spyros Voulgaris
 *
 */
package peeremu.core;

public class DescriptorSim implements Descriptor
{
	/**
	 * Keeps a reference to the respective Node object.
	 * 
	 * In single-machine simulated environments, a reference to Node is
	 * sufficient to fully describe the node.
	 */
	protected final Node node;


	/**
	 * This is the default constructor expected in Descriptor classes.
	 * Unfortunately interfaces in Java cannot define constructors...
	 * 
	 * @param node
	 * @param pid
	 */
	public DescriptorSim(Node node, int pid)
	{
		this.node = node;
	}


	public long getID()
	{
		return node.getID();
	}


	@Override
	public boolean equals(Object otherDescriptor)
	{
		return getID() == ((DescriptorSim)otherDescriptor).getID();
	}



  @Override
  public int hashCode()
  {
    return (int)getID();
  }



  public Node getNode()
  {
    return node;
  }


  public String toString()
  {
    return ""+getID();
  }
  
  public Object clone() throws CloneNotSupportedException
  {
    // 'node' is *not* deep copied.
    Object result = super.clone();

    return result;
  }
}
