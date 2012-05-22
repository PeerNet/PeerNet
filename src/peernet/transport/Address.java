/*
 * Created on Feb 21, 2012 by Spyros Voulgaris
 *
 */
package peernet.transport;

import java.io.Serializable;

public interface Address extends Serializable
{
  public Object clone();
}
