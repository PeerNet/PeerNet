/*
 * Copyright (c) 2003-2005 The BISON Project
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License version 2 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

package peeremu.transport;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

import peeremu.config.Configuration;
import peeremu.config.IllegalParameterException;
import peeremu.core.Control;


/**
 * Initializes static singleton {@link RouterNetwork} by reading a king data set.
 * 
 * @author Spyros Voulgaris
 * @version $Revision: 1.0$
 */
public class MeridianParser implements Control
{

// ---------------------------------------------------------------------
// Parameters
// ---------------------------------------------------------------------

/**
 * The file containing the King measurements.
 * @config
 */
private static final String PAR_FILE = "file";

/**
 * The ratio between the time units used in the configuration file and the
 * time units used in the Peersim simulator.
 * @config
 */
private static final String PAR_RATIO = "ratio";

/**
 * The number of nodes included in the data matrix.
 */
private static final String PAR_SIZE = "size";


// ---------------------------------------------------------------------
// Fields
// ---------------------------------------------------------------------

/** Name of the file containing the King measurements. */
private String filename;

/**
 * Ratio between the time units used in the configuration file and the time
 * units used in the Peersim simulator.
 */
private double ratio;

/** Prefix for reading parameters */
private String prefix;

/**
 * Number of nodes in data matrix.
 */
private int size = 0;

// ---------------------------------------------------------------------
// Initialization
// ---------------------------------------------------------------------

/**
 * Read the configuration parameters.
 */
public MeridianParser(String prefix)
{
	this.prefix = prefix;
	ratio = Configuration.getDouble(prefix + "." + PAR_RATIO, 1);
	filename = Configuration.getString(prefix + "." + PAR_FILE, null);
  size = Configuration.getInt(prefix + "." + PAR_SIZE);
}

// ---------------------------------------------------------------------
// Methods
// ---------------------------------------------------------------------

/**
 * Initializes static singleton {@link RouterNetwork} by reading a king data set.
* @return  always false
*/
public boolean execute()
{
	BufferedReader in = null;
	if (filename != null) {
		try {
			in = new BufferedReader(new FileReader(filename));
		} catch (FileNotFoundException e) {
			throw new IllegalParameterException(prefix + "." + PAR_FILE, filename
					+ " does not exist");
		}
	} else {
		in = new BufferedReader( new InputStreamReader(
						ClassLoader.getSystemResourceAsStream("t-king.map")
					)	);
	}
		
	// XXX If the file format is not correct, we will get quite obscure
	// exceptions. To be improved.


	RouterNetwork.reset(size, true);
	System.err.println("MeridianParser: going to read " + size + " entries");

  try
  {
	  String line = null;
		while ((line=in.readLine()) != null)
    {
			StringTokenizer tok = new StringTokenizer(line, "\t");
			int n1 = Integer.parseInt(tok.nextToken()) - 1;
			int n2 = Integer.parseInt(tok.nextToken()) - 1;
			int latency = (int) (Double.parseDouble(tok.nextToken()) * ratio);
			int mod = latency % 1000;
			latency = 1000*(latency/1000);
			if (mod>=500)
				latency += 1000;
			System.err.println(n1+"\t"+n2+"\t"+latency);
			RouterNetwork.setLatency(n1, n2, latency);
		}
	}
  catch (IOException e) {}

	return false;
}

}
