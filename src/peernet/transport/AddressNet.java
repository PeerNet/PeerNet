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



  @Override
  public boolean equals(Object other)
  {
    return ip.equals(((AddressNet)other).ip) && port==((AddressNet)other).port;
  }


  public String toString()
  {
    return ""+ip+":"+port;
  }

  public int hashCode()
  {
    return ip.hashCode()+port;
  }
}
