/*
 * Created on Feb 24, 2012 by Spyros Voulgaris
 *
 */
package peernet.transport;

import peernet.core.Node;

public class AddressSim implements Address
{
  private static final long serialVersionUID = 1;

  /**
   * Keeps a reference to the respective Node object.
   * 
   * In single-machine simulated environments, a reference to Node is
   * sufficient to address the node.
   */
  public final Node node;



  /**
   * Constructor based on a Node object.
   * 
   * @param node
   */
  public AddressSim(Node node)
  {
    this.node = node;
  }



  public Object clone()
  {
    try
    {
      return super.clone();
    }
    catch (CloneNotSupportedException e)
    {
      e.printStackTrace();
      System.exit(-1);
    }
    return null;
  }



  @Override
  public boolean equals(Object other)
  {
    // The following checks whether 'node' and 'other' are references to the
    // same object. As Node.equals() is not defined, Object.equals() is invoked.
    return node.equals(((AddressSim)other).node);
  }



  public String toString()
  {
    if (node==null)
      return "??";
    else
      return ""+node.getID();
  }

  public int hashCode()
  {
    return node.hashCode();
  }
}
