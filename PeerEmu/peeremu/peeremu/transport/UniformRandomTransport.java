package peeremu.transport;

import peeremu.config.*;
import peeremu.core.*;
import peeremu.edsim.*;


/**
 * Implement a transport layer that reliably delivers messages with a random
 * delay, that is drawn from the configured interval according to the uniform
 * distribution.
 *
 * @author Alberto Montresor
 * @version $Revision: 1.12 $
 */
public class UniformRandomTransport implements Transport
{
  /** 
   * String name of the parameter used to configure the minimum latency.
   * @config
   */	
  private static final String PAR_MINDELAY = "mindelay";

  /** 
   * String name of the parameter used to configure the maximum latency.
   * Defaults to {@value #PAR_MINDELAY}, which results in a constant delay.
   * @config 
   */	
  private static final String PAR_MAXDELAY = "maxdelay";

  /** Minimum delay for message sending */
  private final long min;

  /**
   *  Difference between the max and min delay plus one.
   *  That is, max delay is min+range-1.
   */
  private final long range;

  /**
   * Reads configuration parameter.
   */
  public UniformRandomTransport(String prefix)
  {
    min = Configuration.getLong(prefix + "." + PAR_MINDELAY);
    long max = Configuration.getLong(prefix + "." + PAR_MAXDELAY,min);
    if (max < min) 
      throw new IllegalParameterException(prefix + "." + PAR_MAXDELAY, 
      "The maximum latency cannot be smaller than the minimum latency");
    range = max-min+1;
  }




  /**
   * Returns <code>this</code>. This way only one instance exists in the system
   * that is linked from all the nodes. This is because this protocol has no
   * node specific state.
   */
  public Object clone()
  {
    return this;
  }




  /**
   * Delivers the message with a random
   * delay, that is drawn from the configured interval according to the uniform
   * distribution.
   */
  public void send(Descriptor src, Descriptor dest, int pid, Object payload)
  {
    // avoid calling nextLong if possible
    long delay = (range==1?min:min + CommonState.r.nextLong(range));

    EDSimulator.add(delay, payload, ((DescriptorSim)dest).getNode(), pid);
  }
}
