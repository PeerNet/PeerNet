/*
 * Created on Jul 18, 2007 by Spyros Voulgaris
 *
 */
package peersim.trans;

import peersim.core.Descriptor;
import peersim.core.Protocol;

/**
 * This is a generic transport interface.
 * It is used to send messages to other nodes.
 * 
 * @author Spyros
 */
public interface TransportInterface extends Protocol
{
  /**
   * Used to send a message to another node, given the node's descriptor.
   */
  public void send(Descriptor src, Descriptor dest, int pid, Object payload);
}
