/*
 * Created on Aug 23, 2007 by Spyros Voulgaris
 *
 */
package peeremu.transport;

import peeremu.config.Configuration;
import peeremu.core.CommonState;
import peeremu.core.Engine;
import peeremu.edsim.EDSimulator;


public class TransportEmu implements Transport
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
    assert Engine.isAddressTypeSim();
  }


  public void send(Address dest, int pid, Object payload)
  {
    int senderRouter = (int)CommonState.getNode().getID() % RouterNetwork.getSize();
    int receiverRouter = dest.hashCode() % RouterNetwork.getSize();
    Address senderAddress = new AddressSim(CommonState.getNode());

    int latency = RouterNetwork.getLatency(senderRouter, receiverRouter) + local*2;

    if (latency >= 0) // if latency < 0, it's a broken link
      EDSimulator.add(latency, senderAddress, ((AddressSim)dest).node, pid, payload);
  }

  public Object clone()
  {
    return this;
  }
}
