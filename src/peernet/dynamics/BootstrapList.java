/*
 * Created on Nov 24, 2012 by Spyros Voulgaris
 *
 */
package peernet.dynamics;

import java.io.Serializable;

import peernet.transport.Address;

public class BootstrapList implements Serializable
{
  private static final long serialVersionUID = 7821791956620397834L;

  public String coordinatorName;
  public Address[] addresses;
}
