/*
 * Created on Jul 18, 2007 by Spyros Voulgaris
 *
 */
package peersim.trans;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import peersim.config.Configuration;
import peersim.core.Descriptor;
import peersim.core.DescriptorInet;

public class TransportUDP implements TransportInet
{
	/**
	 * The port to listen to.
	 */
	private static final String PAR_PORT = "port";

	/**
	 * Stores the UDP socket used by this Transport.
	 * Note that a single socket is shared by all nodes running on a JVM.
	 * This decision was made to constrain network resources (e.g., ports)
	 * keeping in mind environments such as PlanetLab. 
	 */
	private DatagramSocket socket = null;


	/**
	 * Default constructor.
	 */
	public TransportUDP(String prefix)
	{
		int port = Configuration.getInt(prefix+"."+PAR_PORT, -1);
		try
		{
			if (port<0)
				socket = new DatagramSocket();
			else
				socket = new DatagramSocket(port);
		}
		catch (SocketException e)
		{
			e.printStackTrace();
		}
	}


	public void send(Descriptor src, Descriptor dest, int pid,
			Object payload)
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos;
		try
		{
			oos = new ObjectOutputStream(baos);
			oos.writeObject(src);
			oos.writeObject(dest);
			oos.writeObject(pid);
			oos.writeObject(payload);
		}
		catch (IOException e) {e.printStackTrace();}

		DatagramPacket datagramPacket;
		try
		{
			InetAddress address = ((DescriptorInet)dest).getAddress();
			int port = ((DescriptorInet)dest).getPort();
			datagramPacket = new DatagramPacket(baos.toByteArray(),
					baos.size(), address, port);
			socket.send(datagramPacket);
		}
		catch (SocketException e) {e.printStackTrace();}
		catch (IOException e) {e.printStackTrace();}
	}


	private void receivePacket(DatagramPacket dgramPacket)
	{
		// First, wait for a packet to be received.
		try {socket.receive(dgramPacket);}
		catch (IOException e) {e.printStackTrace();}

		// Now read the received packet.
		try
		{
			ByteArrayInputStream bais = new ByteArrayInputStream(dgramPacket.getData());
			ObjectInputStream ois = new ObjectInputStream(bais);
			Descriptor src = (Descriptor)ois.readObject();
			Descriptor dest = (Descriptor)ois.readObject();
			int pid = (Integer)ois.readObject();
			Object payload = ois.readObject();
		}
		catch (IOException e) {e.printStackTrace();}
		catch (ClassNotFoundException e) {e.printStackTrace();}

		// Now invoke the Scheduler
		
	}

	public Object clone()
	{
		return this;
	}

	public InetAddress getAddress()
	{
		if (socket != null)
			return socket.getInetAddress();
		else
			return null;
	}

	public int getPort()
	{
		if (socket != null)
			return socket.getPort();
		else
			return -1;
	}
}
