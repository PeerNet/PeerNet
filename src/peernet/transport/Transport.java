/*
 * Created on Jul 18, 2007 by Spyros Voulgaris
 *
 */
package peernet.transport;

import peernet.core.Node;





/**
 * This is a generic transport interface. It is used to send messages to other
 * nodes.
 * 
 * @author Spyros Voulgaris
 */
public abstract class Transport implements Cloneable
{
  /**
   * Used to send a message to another node, given the node's address.
   */
  public abstract void send(Node src, Address dest, int pid, Object payload);



  public Object clone()
  {
    Transport transport = null;
    try
    {
      transport = (Transport) super.clone();
    }
    catch (CloneNotSupportedException e)
    {
      e.printStackTrace();
    }
    return transport;
  }
}
