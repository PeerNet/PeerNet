/*
 * Created on Jul 19, 2007 by Spyros Voulgaris
 *
 */
package peersim.trans;

import java.net.InetAddress;

public interface TransportInet extends TransportInterface
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
