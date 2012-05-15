/*
 * Created on May 14, 2012 by Spyros Voulgaris
 *
 */
package peernet.transport;


public class Packet
{
  public Address src;
  public int pid;
  public Object event;

  public Packet(Address src, int pid, Object event)
  {
    this.src = src;
    this.pid = pid;
    this.event = event;
  }
}
