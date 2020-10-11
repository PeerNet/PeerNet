/*
 * Copyright (c) 2003-2005 The BISON Project
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License version 2 as published by
 * the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package peernet.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import peernet.config.Configuration;
import peernet.transport.Address;



/**
 * Protocol abstract class
 * 
 * XXX: Add documentation
 */
public abstract class Protocol implements Cloneable
{
  /**
   * Parameter for assigning a settings class to a protocol.
   * 
   * @config
   */
  public static final String PAR_SETTINGS = "settings";

  /**
   * Settings of this protocol
   */
  protected ProtocolSettings settings;

  /**
   * The Node this protocol belongs to. Available through the public
   * <code>getNode()</code> method.
   */
  Node node;

  public Protocol(String prefix)
  {
    if (Configuration.contains(prefix+"."+PAR_SETTINGS)) // custom settings
      settings = (ProtocolSettings)Configuration.getInstance(prefix+"."+PAR_SETTINGS);
    else // default settings
      settings = new ProtocolSettings(prefix+"."+PAR_SETTINGS);
  }

  public Node myNode()
  {
    return node;
  }

  public int myPid()
  {
    return settings.pid;
  }



  /**
   * A protocol which is defined by performing an algorithm in more or less
   * regular periodic intervals. This method is called by the simulator engine
   * once in each cycle.
   */
  public abstract void nextCycle(int schedId);



  /**
   * This method is invoked by the scheduler to deliver events to the protocol.
   * The parameters passed are the address of the sender, and the event object.
   * 
   * @param src the address of the event's sender
   * @param event the delivered event
   */
  public abstract void processEvent(Address src, Object event);



  public long nextDelay()  //XXX What is this????
  {
    return 0;
  }



  public void send(Address dest, int pid, Object event)
  {
    node.getTransportByPid(settings.getPid()).send(node, dest, pid, event);
  }


  /**
   * Returns a new instance of the Peer used for the referred protocol.
   * Calls the constructor of the Peer class defined for this protocol.
   * 
   * XXX: Spyros, 2007-11-02: Should I move this to the Protocol interface?
   * XXX: Spyros, 2012-05-23: Yes, I should! ;-)
   */
  public Peer myPeer()
  {
    Peer p = null;
    Constructor<Peer> c = settings.getPeerConstructor();
    try
    {
      p = c.newInstance(node, settings.getPid());
    }
    catch (IllegalArgumentException e)
    {
      e.printStackTrace();
    }
    catch (InstantiationException e)
    {
      e.printStackTrace();
    }
    catch (IllegalAccessException e)
    {
      e.printStackTrace();
    }
    catch (InvocationTargetException e)
    {
      e.printStackTrace();
    }
    return p;
  }



  /**
   * Returns a clone of the protocol. It is important to pay attention to
   * implement this carefully because in PeerNet all nodes are generated by
   * cloning except a prototype node. That is, the constructor of protocols is
   * used only to construct the prototype. Initialization can be done via
   * {@link Control}s.
   */
  public Object clone()
  {
    Protocol protocol = null;
    try
    {
      // We intentionally do shallow cloning of the Settings instance
      protocol = (Protocol) super.clone();
    }
    catch (CloneNotSupportedException e)
    {
      e.printStackTrace();
    }
    return protocol;
  }
}
