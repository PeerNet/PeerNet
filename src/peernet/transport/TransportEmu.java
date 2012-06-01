/*
 * Created on Aug 23, 2007 by Spyros Voulgaris
 */
package peernet.transport;

import peernet.config.Configuration;
import peernet.core.Engine;
import peernet.core.Engine.AddressType;
import peernet.core.Node;





public class TransportEmu extends Transport
{
  /**
   * The delay that corresponds to the time spent on the source (and
   * destination) nodes. In other words, full latency is calculated by fetching
   * the latency that belongs to communicating between two routers, incremented
   * by twice this delay. Defaults to 0.
   * 
   * @config
   */
  private static final String PAR_LOCAL = "local";
  private int local;
  private Engine engine;



  public TransportEmu(String prefix)
  {
    local = Configuration.getInt(prefix+"."+PAR_LOCAL, 0);
    engine = Engine.instance();
    assert Engine.getAddressType()==AddressType.SIM;
  }



  public void send(Node src, Address dest, int pid, Object payload)
  {
    int senderRouter = (int) src.getID()%RouterNetwork.getSize();
    int receiverRouter = dest.hashCode()%RouterNetwork.getSize();
    Address senderAddress = new AddressSim(src);
    int latency = RouterNetwork.getLatency(senderRouter, receiverRouter)+local*2;
    if (latency>=0) // if latency < 0, it's a broken link
      engine.add(latency, senderAddress, ((AddressSim) dest).node, pid, payload);
  }



  @Override
  public Object clone()
  {
    return this; // In SIM or EMU modes, all nodes use a single transport instance
  }
}
