/*
 * Created on Jul 18, 2007 by Spyros Voulgaris
 *
 */
package peersim.trans;

import peersim.core.Descriptor;
import peersim.core.Protocol;

public interface TransportInterface extends Protocol
{
	public void send(Descriptor src, Descriptor dest, int pid, Object payload);
}
