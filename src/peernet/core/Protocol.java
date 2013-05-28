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
 */
public abstract class Protocol implements Cloneable
{
  /**
   * Parameter name in configuration that attaches a transport layer protocol to
   * a protocol.
   * 
   * @config
   */
  public static final String PAR_SETTINGS = "settings"; //XXX should not be public

  protected ProtocolSettings settings;

  protected Node node;

  public Protocol(String prefix)
  {
    if (Configuration.contains(prefix+"."+PAR_SETTINGS)) // custom settings
      settings = (ProtocolSettings)Configuration.getInstance(prefix+"."+PAR_SETTINGS);
    else // dafault settings
      settings = new ProtocolSettings(prefix+"."+PAR_SETTINGS);
  }

  /**
   * This method is invoked by the scheduler to deliver events to the protocol.
   * Apart from the event object, information about the node and the protocol
   * identifier are also provided. Additional information can be accessed
   * through the {@link CommonState} class.
   * 
   * @param from the address of the event's sender
   * @param node the local node
   * @param pid the identifier of this protocol
   * @param event the delivered event
   */
  public abstract void processEvent(Address src, Node node, int pid, Object event);



  /**
   * A protocol which is defined by performing an algorithm in more or less
   * regular periodic intervals. This method is called by the simulator engine
   * once in each cycle with the appropriate parameters.
   * 
   * @param node the node on which this component is run
   * @param protocolID the id of this protocol in the protocol array
   */
  public abstract void nextCycle(Node node, int protocolID);



  public long nextDelay()
  {
    return 0;
  }



  public void send(Address dest, int pid, Object event)
  {
    node.getTransportByPid(settings.getPid()).send(node, dest, pid, event);
  }


  /**
   * Returns a new instance of the Descriptor used for the referred protocol.
   * Calls the constructor of the Descriptor class defined for this protocol.
   * 
   * XXX: Spyros, 2007-11-02: Should I move this to the Protocol interface?
   * XXX: Spyros, 2012-05-23: Yes, I should! ;-)
   */
  public Descriptor getOwnDescriptor()
  {
    Descriptor d = null;
    Constructor<Descriptor> c = settings.getDescriptorConstructor();
    try
    {
      d = c.newInstance(node, settings.getPid());
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
    return d;
  }



  public Descriptor getForeignDescriptor(Address address)
  {
    Descriptor d = null;
    Constructor<Descriptor> c = settings.getAddrDescriptorConstructor();
    try
    {
      d = c.newInstance(address);
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
    return d;
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
      // We intentionally do shallow cloning of ProtocolSettings
      protocol = (Protocol) super.clone();
    }
    catch (CloneNotSupportedException e)
    {
      e.printStackTrace();
    }
    return protocol;
  }
}
