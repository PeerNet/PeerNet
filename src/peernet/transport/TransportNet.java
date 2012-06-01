/*
 * Created on Jul 19, 2007 by Spyros Voulgaris
 */
package peernet.transport;

import java.net.InetAddress;





public abstract class TransportNet extends Transport
{
  /**
   * Returns the IP address where this Transport is listening to.
   */
  public abstract InetAddress getAddress();



  /**
   * Returns the port where this Transport is listening to.
   */
  public abstract int getPort();



  /**
   * Waits (i.e., blocks) for the next packet from the network,
   * marshals it, and returns it as a Packet.
   * 
   * @return The Packet received through the network.
   */
  public abstract Packet receive();
}
