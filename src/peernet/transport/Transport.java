/*
 * Created on Jul 18, 2007 by Spyros Voulgaris
 *
 */
package peernet.transport;

import java.util.Properties;

import peernet.config.Configuration;
import peernet.core.Engine;
import peernet.core.Node;





/**
 * This is a generic transport interface. It is used to send messages to other
 * nodes.
 * 
 * Typically, a single Transport class is used by all protocols, but it is
 * possible for individual protocols to define separate Transport classes.
 * 
 * Note that separate Transport instance(s) are created for each Node.
 * 
 * @author Spyros Voulgaris
 */
public abstract class Transport implements Cloneable
{
  /**
   * Parameter name in configuration that attaches a transport layer protocol to
   * a protocol.
   * 
   * @config
   */
  private static final String PAR_TRANSPORT = "transport";

  private static final String defaultTransportSim = "peernet.transport.UniformRandomTransport";
  private static final String defaultTransportEmu = "peernet.transport.UniformRandomTransport";
  private static final String defaultTransportNet = "peernet.transport.TransportUDP";
  private static Transport defaultTransportInstance = null;





  public static Properties setDefaultTransports()
  {
    Properties p = new Properties();

    if (!Configuration.contains("sim.transport"))
      p.setProperty("sim.transport", defaultTransportSim);

    if (!Configuration.contains("emu.transport"))
      p.setProperty("emu.transport", defaultTransportEmu);

    if (!Configuration.contains("net.transport"))
      p.setProperty("net.transport", defaultTransportNet);
    
    return p;
  }





  /**
   * Used to send a message to another node, given the node's address.
   */
  public abstract void send(Node src, Address dest, int pid, Object payload);



  public Object clone()
  {
    Transport transport = null;
    try
    {
      transport = (Transport) super.clone();
    }
    catch (CloneNotSupportedException e)
    {
      e.printStackTrace();
    }
    return transport;
  }



  public static Transport defaultTransportInstance()
  {
    if (defaultTransportInstance == null)
    {
      String key = Engine.getType().toString().toLowerCase() + "." + PAR_TRANSPORT;
      if (Configuration.contains(key))
        defaultTransportInstance = (Transport) Configuration.getInstance(key);
      else // no default transport defined ==> use the absolutely default one
        switch (Engine.getType())
        {
          case SIM:
          case EMU:
            //XXX defaultTransportInstance = (Transport) Configuration.getInstance(defaultTransportSim);
          case NET:
            //defaultTransportInstance = (Transport) Configuration.getInstance(defaultTransportNet);
          default:
            break;
        }
    }
    return defaultTransportInstance;
  }
}
