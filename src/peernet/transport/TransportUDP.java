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
import java.net.UnknownHostException;

import peernet.config.Configuration;
import peernet.core.Node;





public class TransportUDP extends TransportNet
{
  /**
   * The port to listen to.
   */
  private static final String PAR_PORT = "port";

  /**
   * Stores the UDP socket used by this Transport. Note that each node
   * running on a single JVM uses its own exclusive socket.
   */
  private DatagramSocket socket = null;
  private DatagramPacket dgram = null;
  private byte[] recvBuffer = null;

  /**
   * The next available port to try to bind to. Define as object rather than
   * primitive int, so we can synchronize on it, which makes the point of
   * synchronization cleaner.
   */
  private static Integer nextPort = -1;
  private static int initPort = -1;

  /**
   * Default constructor.
   */
  public TransportUDP(String prefix)
  {
    initPort = Configuration.getInt(prefix+"."+PAR_PORT, -1);
    if (initPort != -1)
      nextPort = initPort;

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
      // No problem failing to send a packet.
      // It is most likely due to full networks buffers.
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

      assert srcAddr!=null : "TransportUDP.receive().srcAddr is null!";
      assert event!=null : "TransportUDP.receive().event is null!";

      Packet packet = new Packet(srcAddr, pid, event);

      assert packet!=null : "TransportUDP.receive().packet is null!";

      return packet;
    }
    catch (ClassNotFoundException e)
    {
      e.printStackTrace();
    }
    catch (IOException e)
    {
      try
      {
        System.out.println("myhost="+InetAddress.getLocalHost().getHostName());
      }
      catch (UnknownHostException e1)
      {
        // TODO Auto-generated catch block
        e1.printStackTrace();
      }
      e.printStackTrace();
      System.exit(-1);
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
      return socket.getLocalPort();
    else
      return -1;
  }



  private DatagramSocket bindNextLocalPort()
  {
    @SuppressWarnings("hiding")
    DatagramSocket socket = null;

    synchronized (nextPort)
    {
      if (initPort<0)
      {
        try
        {
          socket = new DatagramSocket();
          //socket.setReuseAddress(true); // TODO: should I use REUSEADDR?
        }
        catch (SocketException e)
        {
          socket = null;
        }
      }
      else
      {
        while (socket==null)
        {
          try
          {
            socket = new DatagramSocket(nextPort);
            //socket.setReuseAddress(true); // TODO: should I use REUSEADDR?
          }
          catch (SocketException e)
          {
            nextPort++;
            if (nextPort == 65536)
              nextPort = 1024;
            if (nextPort == initPort)
              return null;
          }
        }
        nextPort++;
        if (nextPort == 65536)
          nextPort = 1024;
      }
    }
    return socket;
  }



  @Override
  public Object clone()
  {
    TransportUDP trans = null;
    trans = (TransportUDP) super.clone();
    trans.socket = bindNextLocalPort();
    trans.recvBuffer = recvBuffer.clone();
    trans.dgram = new DatagramPacket(trans.recvBuffer, trans.recvBuffer.length);

    return trans;
  }
}
