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
 * 
 */
package peernet.core;

import java.util.Vector;
import java.util.concurrent.Semaphore;

import peernet.config.Configuration;
import peernet.transport.Transport;


/**
 * Class that represents one node with a network address. An {@link Network} is
 * made of a set of nodes. The functionality of this class is thin: it must be
 * able to represent failure states and store a list of protocols. It is the
 * protocols that do the interesting job.
 */
public class Node implements Fallible, Cloneable
{
  /**
   * Prefix of the parameters that defines protocols.
   * 
   * @config
   */
  private static final String PAR_PROTOCOL = "protocol";

  /**
   * Parameter name in configuration that attaches a transport layer protocol to
   * a protocol.
   * 
   * @config
   */
  private static final String PAR_TRANSPORT = "transport";



  /** used to generate unique IDs */
  private static long counterID = -1;

  /**
   * The protocols on this node.
   */
  protected Protocol[] protocols = null;

  /**
   * The transports on this node.
   */
  private Transport[] transports = null;

  /** Mapping i->j means protocol[i] has transport[j] */
  private int mappingProtTrans[] = null;

  /**
   * The current index of this node in the node list of the {@link Network}. It
   * can change any time. This is necessary to allow the implementation of
   * efficient graph algorithms.
   */
  private int index;

  /** The fail state of the node. */
  protected int failstate = Fallible.OK;

  /**
   * The id of the node. It should be final, however it can't be final because
   * clone must be able to set it.
   */
  private long ID;

  /** The heap storing events for this node */
  private Heap heap;

  /** The semaphore controlling access to the node's heap */
  private Semaphore semaphore;




  /**
   * Returns the <code>i</code>-th protocol in this node. If <code>i</code>
   * is not a valid protocol id (negative or larger than or equal to the number
   * of protocols), then it throws IndexOutOfBoundsException.
   */
  public Protocol getProtocol(int i)
  {
    return protocols[i];
  }



  /**
   * Returns the ID of this node. The IDs are generated using a counter (i.e.,
   * they are not random).
   */
  public final long getID()
  {
    return ID;
  }



  /**
   * Sets the ID of this node.
   * This method has 'package' access, to be used only by PeerNet.
   * Not intended for use by the application.
   */
  public final void setID(long id)
  {
    ID = id;
  }



  /**
   * Used to construct the prototype node. This class currently does not have
   * specific configuration parameters and so the parameter <code>prefix</code>
   * is not used. It reads the protocol components (components that have type
   * {@value peernet.core.Node#PAR_PROTOCOL}) from the configuration.
   */
  public Node(String prefix)
  {
    String[] protNames = Configuration.getNames(PAR_PROTOCOL);
    // CommonState.setNode(this);
    ID = nextID();
    protocols = new Protocol[protNames.length];

    // Find out how many distinct transports are being used per node.
    mappingProtTrans = new int[protNames.length];
    Vector<String> transportNames = new Vector<String>();
    for (int i=0; i<protNames.length; i++)
    {
      //String transportName = Configuration.getString(protNames[i]+"."+PAR_TRANSPORT, defaultTransportName());
      String transportName = Configuration.getString(protNames[i]+"."+PAR_TRANSPORT, null);  // null = default transport

      // If the transport defined for this protocol is not in our list yet, add it
      if (!transportNames.contains(transportName))
        transportNames.add(transportName);

      // Set the mapping from this protocol to this transport
      int j = transportNames.indexOf(transportName);
      assert j >= 0;
      mappingProtTrans[i] = j;
    }

    // Instantiate the transports
    transports = new Transport[transportNames.size()];
    for (int i=0; i<transports.length; i++)
    {
      if (transportNames.get(i) == null) // default transport
        transports[i] = Transport.defaultTransportInstance();
      else
        transports[i] = (Transport) Configuration.getInstance(PAR_TRANSPORT+"."+transportNames.get(i));
    }

    // Instantiate the protocols
    for (int i = 0; i<protNames.length; i++)
    {
      // CommonState.setPid(i);
      Protocol p = (Protocol) Configuration.getInstance(protNames[i]);
      protocols[i] = p;
    }
  }



  /**
   * Clones the node. It is defined as part of the interface to change the
   * access right to public and to get rid of the <code>throws</code> clause.
   */
  public Object clone()
  {
    Node node = null;
    try
    {
      node = (Node) super.clone();
    }
    catch (CloneNotSupportedException e)
    {} // never happens

    node.protocols = protocols.clone();
    // CommonState.setNode(result);
    node.ID = nextID();
    for (int i=0; i<protocols.length; i++)
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



  /**
   * Returns the number of protocols included in this node.
   */
  public int protocolSize()
  {
    return protocols.length;
  }



  /**
   * Sets the index of this node in the internal representation of the node
   * list. Applications should not use this method. It is defined as public
   * simply because it is not possible to define it otherwise. Using this method
   * will result in undefined behavior. It is provided for the core system.
   */
  void setIndex(int index)
  {
    this.index = index;
  }



  /**
   * Returns the index of this node. It is such that
   * <code>Network.get(n.getIndex())</code> returns n. This index can change
   * during a simulation, it is not a fixed id. If you need that, use
   * {@link #getID}.
   * 
   * @see Network#get
   */
  public int getIndex()
  {
    return index;
  }



  /**
   * Returns the unique ID of the node. It is guaranteed that the ID is unique
   * during the entire simulation, that is, there will be no different Node
   * objects with the same ID in the system during one invocation of the JVM.
   * Preferably nodes should implement <code>hashCode()</code> based on this
   * ID.
   */
//  public long getID();



  /** returns the next unique ID */
  protected long nextID()
  {
    return counterID++;
  }



  /** Returns the number of different transports used by this node */
  public int getTransports()
  {
    return transports.length;
  }



  /** Returns the transport used by a given protocol */
  public Transport getTransportByPid(int pid)
  {
    return transports[mappingProtTrans[pid]];
  }



  /** Returns transport <code>i</code> */
  public Transport getTransport(int i)
  {
    return transports[i];
  }



  /** Sets a node's fail state to <code>failState</code> */
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



  /** Implemented as <code>(int)getID()</code>. */
  public int hashCode()
  {
    return (int) getID();
  }



  public void setHeap(Heap heap)
  {
    this.heap = heap;
  }



  public Heap getHeap()
  {
    return heap;
  }



  public void initLock()
  {
    semaphore = new Semaphore(1);
  }



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



  public void releaseLock()
  {
    semaphore.release();
  }



  /**
   * Auxiliary private method finding the name of the default transport.
   * 
   * @return
   */
  private String defaultTransportName()
  {
    String transportName = Engine.getType().toString().toLowerCase();
    if (Configuration.contains(PAR_TRANSPORT+"."+transportName))
      return Configuration.getString(transportName);
    return null;
  }
}
