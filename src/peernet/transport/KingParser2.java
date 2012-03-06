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

package peernet.transport;

import java.io.*;
import java.util.*;

import peernet.config.*;
import peernet.core.Control;

/**
 * Initializes static singleton {@link RouterNetwork} by reading a king data set.
 * 
 * @author Alberto Montresor
 * @version $Revision: 1.7 $
 */
public class KingParser2 implements Control
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

// ---------------------------------------------------------------------
// Initialization
// ---------------------------------------------------------------------

/**
 * Read the configuration parameters.
 */
public KingParser2(String prefix)
{
	this.prefix = prefix;
	ratio = Configuration.getDouble(prefix + "." + PAR_RATIO, 1);
	filename = Configuration.getString(prefix + "." + PAR_FILE, null);
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

	String line = null;
	int size = 1740;
//	try {
////		while ((line = in.readLine()) != null && !line.startsWith("node"));
////		while (line != null && line.startsWith("node")) {
//	  line = in.readLine();
//	  while (line != null) {
//			size++;
//			line = in.readLine();
//		}
//	} catch (IOException e) {
//	}
	RouterNetwork.reset(size, true);
	System.err.println("KingParser: read " + size + " entries");	
	try {
	  line = in.readLine();
	  int row = 0;
    int col = 0;
		do {
		  StringTokenizer tok = new StringTokenizer(line, " ");
		  col=0;
			do {
			  double latency = Double.parseDouble(tok.nextToken());
	      int lat;
	      if (latency < 0) {
	        lat = 100;
	      } else {
	        lat = (int) (latency * ratio);
	      }
	      if (row != col) {
	        RouterNetwork.setLatency(row, col, lat);
	      }
	      col++;
			} while (tok.hasMoreTokens());
	    row++;
			line = in.readLine();
		} while (line != null);
	} catch (IOException e) {
	  e.printStackTrace();
	}

	return false;
}

}

