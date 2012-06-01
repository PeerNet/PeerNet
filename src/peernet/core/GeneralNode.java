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

import java.util.Vector;
import java.util.concurrent.Semaphore;

import peernet.config.Configuration;
import peernet.transport.Transport;





/**
 * This is the default {@link Node} class that is used to compose the
 * {@link Network}.
 */
public class GeneralNode implements Node
{
  /** used to generate unique id-s */
  private static long counterID = -1;

  /**
   * The protocols on this node.
   */
  protected Protocol[] protocols = null;

  /**
   * The transports on this node.
   */
  private Transport[] transports = null;

  private int mappingProtTrans[] = null;

  /**
   * The current index of this node in the node list of the {@link Network}. It
   * can change any time. This is necessary to allow the implementation of
   * efficient graph algorithms.
   */
  private int index;

  /**
   * The fail state of the node.
   */
  protected int failstate = Fallible.OK;

  /**
   * The id of the node. It should be final, however it can't be final because
   * clone must be able to set it.
   */
  private long ID;

  private Heap heap;
  private Semaphore semaphore;



  /**
   * Used to construct the prototype node. This class currently does not have
   * specific configuration parameters and so the parameter <code>prefix</code>
   * is not used. It reads the protocol components (components that have type
   * {@value peernet.core.Node#PAR_PROT}) from the configuration.
   */
  public GeneralNode(String prefix)
  {
    String[] names = Configuration.getNames(PAR_PROT);
    // CommonState.setNode(this);
    ID = nextID();
    protocols = new Protocol[names.length];

    // Find out how many distinct transports are being used per node.
    mappingProtTrans = new int[names.length];
    Vector<String> transportNames = new Vector<String>();
    for (int i = 0; i<names.length; i++)
    {
      String transportName = Configuration.getString(names[i]+"."+PAR_TRANSPORT, defaultTransportName());
      int j = transportNames.indexOf(transportName);
      if (j >= 0)
        mappingProtTrans[i] = j;
      else
      {
        transportNames.add(transportName);
        mappingProtTrans[i] = transportNames.indexOf(transportName);
      }
    }

    // Instantiate these transports
    transports = new Transport[transportNames.size()];
    for (int i=0; i<transports.length; i++)
      transports[i] = (Transport) Configuration.getInstance(PAR_TRANSPORT+"."+transportNames.get(i));

    for (int i = 0; i<names.length; i++)
    {
      // CommonState.setPid(i);
      Protocol p = (Protocol) Configuration.getInstance(names[i]);
      protocols[i] = p;
    }
  }



  private String defaultTransportName()
  {
    String transportName = Engine.getType().toString().toLowerCase();
    if (Configuration.contains(PAR_TRANSPORT+"."+transportName))
      return Configuration.getString(transportName);
    return null;
  }



  @Override
  public int getTransports()
  {
    return transports.length;
  }



  @Override
  public Transport getTransportByPid(int pid)
  {
    return transports[mappingProtTrans[pid]];
  }



  @Override
  public Transport getTransport(int i)
  {
    return transports[i];
  }



  public Object clone()
  {
    GeneralNode node = null;
    try
    {
      node = (GeneralNode) super.clone();
    }
    catch (CloneNotSupportedException e)
    {} // never happens

    node.protocols = new Protocol[protocols.length];
    // CommonState.setNode(result);
    node.ID = nextID();
    for (int i = 0; i<protocols.length; ++i)
    {
      // CommonState.setPid(i);
      node.protocols[i] = (Protocol) protocols[i].clone();
      node.protocols[i].node = node;
    }

    node.transports = transports.clone();
    for (int i=0; i<transports.length; i++)
      node.transports[i] = (Transport) transports[i].clone();

    return node;
  }



  /** returns the next unique ID */
  private long nextID()
  {
    return counterID++;
  }



  public void setFailState(int failState)
  {
    // after a node is dead, all operations on it are errors by definition
    if (failstate==DEAD&&failState!=DEAD)
      throw new IllegalStateException(
          "Cannot change fail state: node is already DEAD");
    switch (failState)
    {
      case OK:
        failstate = OK;
        break;
      case DEAD:
        // protocol = null;
        index = -1;
        failstate = DEAD;
        for (int i = 0; i<protocols.length; ++i)
          if (protocols[i] instanceof Cleanable)
            ((Cleanable) protocols[i]).onKill();
        break;
      case DOWN:
        failstate = DOWN;
        break;
      default:
        throw new IllegalArgumentException("failState="+failState);
    }
  }



  public int getFailState()
  {
    return failstate;
  }



  public boolean isUp()
  {
    return failstate==OK;
  }



  public Protocol getProtocol(int i)
  {
    return protocols[i];
  }



  public int protocolSize()
  {
    return protocols.length;
  }



  public int getIndex()
  {
    return index;
  }



  public void setIndex(int index)
  {
    this.index = index;
  }



  /**
   * Returns the ID of this node. The ID-s are generated using a counter (ie
   * they are not random).
   */
  public long getID()
  {
    return ID;
  }



  public String toString()
  {
    StringBuffer buffer = new StringBuffer();
    buffer.append("ID: "+ID+" index: "+index+"\n");
    for (int i = 0; i<protocols.length; ++i)
    {
      buffer.append("protocol["+i+"]="+protocols[i]+"\n");
    }
    return buffer.toString();
  }



  // ------------------------------------------------------------------
  /** Implemented as <code>(int)getID()</code>. */
  public int hashCode()
  {
    return (int) getID();
  }



  @Override
  public void setHeap(Heap heap)
  {
    this.heap = heap;
  }



  @Override
  public Heap getHeap()
  {
    return heap;
  }



  @Override
  public void initLock()
  {
    semaphore = new Semaphore(1);
  }



  @Override
  public void acquireLock()
  {
    try
    {
      semaphore.acquire();
    }
    catch (InterruptedException e) // XXX When does this happen?
    {
      e.printStackTrace();
      System.exit(-1);
    }
  }



  @Override
  public void releaseLock()
  {
    semaphore.release();
  }
}
