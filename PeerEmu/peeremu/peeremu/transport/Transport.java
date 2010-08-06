/*
 * Created on Jul 18, 2007 by Spyros Voulgaris
 *
 */
package peeremu.transport;

import peeremu.core.Descriptor;
import peeremu.core.Protocol;

/**
 * This is a generic transport interface.
 * It is used to send messages to other nodes.
 * 
 * @author Spyros Voulgaris
 */
public interface Transport extends Protocol
{
  /**
   * Used to send a message to another node, given the node's descriptor.
   */
  public void send(Descriptor src, Descriptor dest, int pid, Object payload);
}
