/*
 * Created on Aug 23, 2007 by Spyros Voulgaris
 *
 */
package peernet.transport;

import peernet.config.Configuration;
import peernet.core.CommonState;
import peernet.edsim.Engine;
import peernet.edsim.Engine.AddressType;


public class TransportEmu extends Transport
{
  /**
   * The delay that corresponds to the time spent on the source (and destination)
   * nodes. In other words, full latency is calculated by fetching the latency
   * that belongs to communicating between two routers, incremented by
   * twice this delay. Defaults to 0.
   * @config
   */
  private static final String PAR_LOCAL = "local";


  private int local;

  public TransportEmu(String prefix)
  {
    local = Configuration.getInt(prefix + "." + PAR_LOCAL, 0);
    assert Engine.getAddressType() == AddressType.SIM;
  }


  public void send(Address dest, int pid, Object payload)
  {
    int senderRouter = (int)CommonState.getNode().getID() % RouterNetwork.getSize();
    int receiverRouter = dest.hashCode() % RouterNetwork.getSize();
    Address senderAddress = new AddressSim(CommonState.getNode());

    int latency = RouterNetwork.getLatency(senderRouter, receiverRouter) + local*2;

    if (latency >= 0) // if latency < 0, it's a broken link
      Engine.add(latency, senderAddress, ((AddressSim)dest).node, pid, payload);
  }

  public Object clone()
  {
    return this;
  }
}
