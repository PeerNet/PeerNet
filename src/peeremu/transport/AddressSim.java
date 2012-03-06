/*
 * Created on Feb 24, 2012 by Spyros Voulgaris
 *
 */
package peeremu.transport;

import peeremu.core.Node;

public class AddressSim implements Address
{
  /**
   * Keeps a reference to the respective Node object.
   * 
   * In single-machine simulated environments, a reference to Node is
   * sufficient to address the node.
   */
  protected Node node;



  /**
   * Default constructor
   */
  public AddressSim()
  {
  }



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



  public String toString()
  {
    if (node==null)
      return "??";
    else
      return ""+node.getID();
  }
}
