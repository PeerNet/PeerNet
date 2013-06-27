/*
 * Created on Feb 24, 2012 by Spyros Voulgaris
 *
 */
package peernet.transport;

import java.net.InetAddress;
import java.net.UnknownHostException;


public class AddressNet implements Address
{
  private static final long serialVersionUID = 1;

  /**
   * The IP Address and Port of the referenced node.
   */
  protected InetAddress ip;
  protected int port;



  public AddressNet(InetAddress ip, int port)
  {
    this.ip = ip;
    this.port = port;
  }



  @Override
  public Object clone()
  {
    AddressNet addr = null;
    try
    {
      addr = (AddressNet) super.clone();
      addr.ip = InetAddress.getByAddress(ip.getAddress());
    }
    catch (CloneNotSupportedException e)
    {
      e.printStackTrace();
      System.exit(-1);
    }
    catch (UnknownHostException e)
    {
      e.printStackTrace();
      System.exit(-1);
    }
    return addr;
  }



  /**
   * Public getter for the IP address.
   */
  public final InetAddress getIP()
  {
    return ip;
  }



  /**
   * Public getter for the port.
   */
  public final int getPort()
  {
    return port;
  }



  /**
   * Default toString()
   */
  @Override
  public String toString()
  {
    return ""+ip+":"+port;
  }



  /**
   * Methods equals() and hashCode() depend on the IP address and port
   * exclusively. They explicitly do not take the node ID into account,
   * for protocols that are ID-agnostic.
   */
  @Override
  public final int hashCode()
  {
    return ip.hashCode()+port;
  }



  /**
   * Methods equals() and hashCode() depend on the IP address and port
   * exclusively. They explicitly do not take the node ID into account,
   * for protocols that are ID-agnostic.
   */
  @Override
  public final boolean equals(Object other)
  {
    return ip.equals(((AddressNet)other).ip) && port==((AddressNet)other).port;
  }
}
