/*
 * Created on Jul 19, 2007 by Spyros Voulgaris
 *
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

	public abstract Packet receive();
}
