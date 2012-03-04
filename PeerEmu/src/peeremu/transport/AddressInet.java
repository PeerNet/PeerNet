/*
 * Created on Feb 24, 2012 by Spyros Voulgaris
 *
 */
package peeremu.transport;

import java.net.InetAddress;
import java.net.UnknownHostException;


public class AddressInet implements Address
{
  /**
   * The IP Address and Port of the referenced node.
   */
  protected InetAddress ip;
  protected int port;

  public Object clone()
  {
    AddressInet addr = null;
    try
    {
      addr = (AddressInet) super.clone();
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
}
