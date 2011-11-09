/*
 * Created on Nov 17, 2007 by Spyros Voulgaris
 * 
 */
package peeremu.transport;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

import peeremu.config.Configuration;
import peeremu.core.Control;


/**
 * Initializes static singleton {@link RouterNetwork} by reading a trace file
 * containing the latency distance measured between a set of "virtual" routers.
 * Latency between two nodes is not necessarily symmetric.
 * 
 * The format of the file is as follows: all values are stored as integers. The
 * first value is the number of nodes considered. The rest of the values
 * correspond to a "strictly upper triangular matrix" (see this <a
 * href="http://en.wikipedia.org/w/index.php?title=Triangular_matrix&oldid=82411128">
 * link</a>), ordered first by row than by column.
 * 
 * @author Spyros Voulgaris
 */
public class MatrixParser implements Control
{
  /**
   * This configuration parameter identifies the filename of the file containing
   * the measurements. First, the file is used as a pathname in the local file
   * system. If no file can be identified in this way, the file is searched in the
   * local classpath. If the file cannot be identified again, an error message is
   * reported.
   * 
   * @config
   */
  private static final String PAR_FILE = "file";

  /**
   * The number of time units in which a second is subdivided, in simulation.
   * @config
   */
  private static final String PAR_SIM_TICKS_PER_SEC = "ticks_per_sec";

  /**
   * The number of time units in which a second is subdivided, in the trace.
   * @config
   */
  private static final String PAR_TRACE_TICKS_PER_SEC = "trace_ticks_per_sec";

  /** Name of the file containing the measurements. */
  private String filename;

  /** Prefix for reading parameters */
  private String prefix;

  /** Ratio read from PAR_RATIO */
  private double ratio;

  private boolean binary = false;



  /**
   * Read the configuration parameters.
   */
  public MatrixParser(String prefix)
  {
    this.prefix = prefix;
    filename = Configuration.getString(prefix + "." + PAR_FILE);

    int ticks_per_sec = Configuration.getInt(prefix+"."+PAR_SIM_TICKS_PER_SEC);
    int trace_ticks_per_sec = Configuration.getInt(prefix+"."+PAR_TRACE_TICKS_PER_SEC);
    ratio = ((double)ticks_per_sec) / ((double)trace_ticks_per_sec);
 }


  /**
   * Initializes static singleton {@link RouterNetwork} by reading a king data
   * set.
   * 
   * @return always false
   */
  public boolean execute()
  {
    try
    {
      if (binary)
        readBinaryFormat(filename, ratio);
      else
        readAsciiFormat(filename, ratio);
    }
    catch (IOException e)
    {
      throw new RuntimeException(e.getMessage());
    }
    return false;
  }


  protected static void readAsciiFormat(String filename, double ratio) throws IOException
  {
    BufferedReader in = new BufferedReader(new FileReader(filename));

    // Read the number of nodes in the file (first four bytes).
    String line = in.readLine();
    int size = Integer.parseInt(line);

    // Reset the E2E network
    RouterNetwork.reset(size, false);
    System.err.println("MatrixParser: reading latencies for "+size+" nodes");

    // Reading data
    int count = 0;
    for (int row=0; row<size; row++)
    {
      line=in.readLine();
      assert line!=null;

      StringTokenizer tok = new StringTokenizer(line, " ");
      for (int col=0; col<size; col++)
      {
        double latency = Double.parseDouble(tok.nextToken()) * ratio;
        RouterNetwork.setLatency(row, col, latency >= 0 ? (int)latency : -1);
        count++;
      }
    }

    System.err.println("MatrixParser: Read " + count + " entries");
  }


  protected static void readBinaryFormat(String filename, double ratio) throws IOException
  {
    DataInputStream in = null;
    try
    {
      in = new DataInputStream(new BufferedInputStream(new FileInputStream(filename)));
      System.err.println("MatrixParser: Reading file "+filename);
    }
    catch (FileNotFoundException e)
    {
      throw new RuntimeException(e.getMessage());
    }

    // Read the number of nodes in the file (first four bytes).
    int size = in.readInt();

    // Reset the E2E network
    RouterNetwork.reset(size, true);
    System.err.println("MatrixParser: reading "+size+" rows");

    // Reading data
    int count = 0;
    for (int row=0; row<size; row++)
    {
      for (int col=0; col<size; col++)
      {
        int x = (int) (ratio*in.readInt());
        count++;
        RouterNetwork.setLatency(row,col,x);
      }
    }
    System.err.println("MatrixParser: Read " + count + " entries");
  }


  public static void main(String[] args) throws IOException
  {
    readAsciiFormat("/home/spyros/Data/latency/king_matrix", 1);
  }
}
