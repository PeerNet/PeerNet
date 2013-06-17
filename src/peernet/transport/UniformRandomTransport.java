package peernet.transport;

import peernet.config.Configuration;
import peernet.config.IllegalParameterException;
import peernet.core.CommonState;
import peernet.core.Engine;
import peernet.core.Node;





/**
 * Implement a transport layer that reliably delivers messages with a random
 * delay, that is drawn from the configured interval according to the uniform
 * distribution.
 * 
 * @author Alberto Montresor
 * @version $Revision: 1.12 $
 */
public class UniformRandomTransport extends Transport
{
  /**
   * String name of the parameter used to configure the minimum latency.
   * 
   * @config
   */
  private static final String PAR_MINDELAY = "mindelay";
  /**
   * String name of the parameter used to configure the maximum latency.
   * Defaults to {@value #PAR_MINDELAY}, which results in a constant delay.
   * 
   * @config
   */
  private static final String PAR_MAXDELAY = "maxdelay";
  /** Minimum delay for message sending */
  private final long min;
  /**
   * Difference between the max and min delay plus one. That is, max delay is
   * min+range-1.
   */
  private final long range;



  /**
   * Reads configuration parameter.
   */
  public UniformRandomTransport(String prefix)
  {
    min = Configuration.getLong(prefix+"."+PAR_MINDELAY, 0);
    long max = Configuration.getLong(prefix+"."+PAR_MAXDELAY, min);
    if (max<min)
      throw new IllegalParameterException(prefix+"."+PAR_MAXDELAY,
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
   * Delivers the message with a random delay, that is drawn from the configured
   * interval according to the uniform distribution.
   */
  public void send(Node src, Address dest, int pid, Object payload)
  {
    // avoid calling nextLong if possible
    long delay = (range==1 ? min : min+CommonState.r.nextLong(range));
    Address senderAddress = new AddressSim(src);
    Engine.instance().add(delay, senderAddress, ((AddressSim) dest).node, pid,
        payload);
  }
}
