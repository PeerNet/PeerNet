/*
 * Created on Jul 18, 2007 by Spyros Voulgaris
 *
 */
package peernet.transport;

import peernet.core.Protocol;

/**
 * This is a generic transport interface.
 * It is used to send messages to other nodes.
 * 
 * @author Spyros Voulgaris
 */
public interface Transport extends Protocol
{
  /**
   * Used to send a message to another node, given the node's address.
   */
  public void send(Address dest, int pid, Object payload);
}
