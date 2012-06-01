/*
 * Created on Jul 18, 2007 by Spyros Voulgaris
 */
package peernet.transport;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import peernet.config.Configuration;
import peernet.core.Node;





public class TransportUDP extends TransportNet
{
  /**
   * The port to listen to.
   */
  private static final String PAR_PORT = "port";

  /**
   * Stores the UDP socket used by this Transport. Note that a single socket is
   * shared by all nodes running on a JVM. This decision was made to constrain
   * network resources (e.g., ports) keeping in mind environments such as the
   * PlanetLab.
   */
  private DatagramSocket socket = null;
  private DatagramPacket dgram = null;
  private byte[] recvBuffer = null;

  private static Integer port = -1;


  /**
   * Default constructor.
   */
  public TransportUDP(String prefix)
  {
    port = Configuration.getInt(prefix+"."+PAR_PORT, -1);
    recvBuffer = new byte[65536]; //TODO: parameterize
  }



  public void send(Node src, Address dest, int pid, Object payload)
  {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos;
    try
    {
      oos = new ObjectOutputStream(baos);
      oos.writeObject(pid);
      oos.writeObject(payload);
      DatagramPacket datagramPacket = new DatagramPacket(baos.toByteArray(),
          baos.size(), ((AddressNet) dest).ip, ((AddressNet) dest).port);
      socket.send(datagramPacket);
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
  }



  @Override
  public Packet receive()
  {
    try
    {
      // First, wait for a packet to be received.
      socket.receive(dgram);
      ByteArrayInputStream bais = new ByteArrayInputStream(dgram.getData());
      ObjectInputStream ois = new ObjectInputStream(bais);
      AddressNet srcAddr = new AddressNet(dgram.getAddress(), dgram.getPort());
      int pid = (Integer) ois.readObject();
      Object event = ois.readObject();
      Packet packet = new Packet(srcAddr, pid, event);
      return packet;
    }
    catch (ClassNotFoundException e)
    {
      e.printStackTrace();
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
    return null;
  }



  public InetAddress getAddress()
  {
    if (socket!=null)
      return socket.getInetAddress();
    else
      return null;
  }



  public int getPort()
  {
    if (socket!=null)
      return socket.getPort();
    else
      return -1;
  }



  @Override
  public Object clone()
  {
    TransportUDP trans = null;
    try
    {
      trans = (TransportUDP) super.clone();
      synchronized (port)
      {
        if (port<0)
          trans.socket = new DatagramSocket();
        else
        {
          trans.socket = new DatagramSocket(port); // allow a list of ports
          trans.socket.setReuseAddress(true);
          port = -1;
        }
      }
      trans.recvBuffer = recvBuffer.clone();
      trans.dgram = new DatagramPacket(trans.recvBuffer, trans.recvBuffer.length);
    }
    catch (SocketException e)
    {
      e.printStackTrace();
    }
    return trans;
  }
}
