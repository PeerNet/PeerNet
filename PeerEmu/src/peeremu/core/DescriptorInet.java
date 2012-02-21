/*
 * Created on Jul 16, 2007 by Spyros Voulgaris
 *
 */
package peeremu.core;

import java.io.Serializable;
import java.net.InetAddress;

import peeremu.config.FastConfig;
import peeremu.config.IllegalParameterException;
import peeremu.transport.TransportInet;

public class DescriptorInet implements Descriptor, Serializable
{
	/**
   * 
   */
  private static final long serialVersionUID = 6822929577673644051L;


  private static final String PAR_TRANSPORT = "transport";


	/**
	 * The (unique) ID of the node referenced by this Descriptor.
	 */
	private final long ID;

	/**
	 * The IP Address and Port of the referenced node.
	 */
	private InetAddress address;
	private int port;


	public DescriptorInet(Node node, int pid)
	{
		/*
		 * Store node ID.
		 */
		ID = node.getID();

		/*
		 * Retrieve and store IP address and port.
		 */
		int tid = FastConfig.getTransport(pid);
		if (!(node.getProtocol(tid) instanceof TransportInet))
		{
			throw new IllegalParameterException("",
				this.getClass().getCanonicalName()+
				" expected transport that implements the "+
				TransportInet.class.getCanonicalName()+
				" interface.");
		}
		TransportInet trans = (TransportInet) node.getProtocol(tid);
		address = trans.getAddress();
		port = trans.getPort();
	}


	public long getID()
	{
		return ID;
	}


	public boolean equals(Object otherDescriptor)
	{
		return getID() == ((DescriptorSim)otherDescriptor).getID();
	}


	public InetAddress getAddress()
	{
		return address;
	}


	public int getPort()
	{
		return port;
	}


	public String toString()
	{
	  return ""+getID();
	}

	public Object clone() throws CloneNotSupportedException
	{
	  throw new CloneNotSupportedException();
	}
}
