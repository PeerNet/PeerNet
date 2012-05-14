/*
 * Created on Jul 18, 2007 by Spyros Voulgaris
 *
 */
package peernet.transport;

import peernet.core.Node;
import peernet.core.Protocol;

/**
 * This is a generic transport interface.
 * It is used to send messages to other nodes.
 * 
 * @author Spyros Voulgaris
 */
public abstract class Transport extends Protocol
{
  /**
   * Used to send a message to another node, given the node's address.
   */
  public abstract void send(Node src, Address dest, int pid, Object payload);
  
  


  @Override
  public final void processEvent(Address src, Node node, int pid, Object event)
  {
    assert false;
  }


  @Override
  public final void nextCycle(Node node, int protocolID)
  {
    assert false;
  }
}
