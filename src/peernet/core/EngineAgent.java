/*
 * Created on Oct 28, 2020 by Spyros Voulgaris
 *
 */
package peernet.core;

import peernet.transport.Address;

public class EngineAgent
{
  protected void addEventIn(long delay, Address src, Node node, int pid, Object event)
  {
    Engine.instance().addEventIn(delay, src, node, pid, event);
  }
}
