/*
 * Created on Jul 19, 2007 by Spyros Voulgaris
 *
 */
package peeremu.transport;

import java.net.InetAddress;

public interface TransportInet extends Transport
{
	/**
	 * Returns the IP address where this Transport is listening to.
	 */
	InetAddress getAddress();

	/**
	 * Returns the port where this Transport is listening to.
	 */
	int getPort();
}
