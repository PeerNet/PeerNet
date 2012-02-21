/*
 * Created on Aug 23, 2007 by Spyros Voulgaris
 *
 */
package peeremu.transport;

import peeremu.config.Configuration;
import peeremu.core.Descriptor;
import peeremu.core.DescriptorSim;
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
  }


  public void send(Descriptor src, Descriptor dest, int pid, Object payload)
  {
    int senderRouter = (int)src.getID() % RouterNetwork.getSize();
    int receiverRouter = (int)dest.getID() % RouterNetwork.getSize();

    int latency = RouterNetwork.getLatency(senderRouter, receiverRouter) + local*2;

    if (latency >= 0) // if latency < 0, it's a broken link
      EDSimulator.add(latency, payload, ((DescriptorSim)dest).getNode(), pid);
  }

  public Object clone()
  {
    return this;
  }
}
