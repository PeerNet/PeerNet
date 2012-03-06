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

package peeremu.config;

import java.lang.reflect.Constructor;

import peeremu.core.CommonState;
import peeremu.core.Descriptor;
import peeremu.core.Node;

/**
 * Reads configuration regarding relations between protocols.
 * 
 * Technically, this class is not necessary because protocols could
 * access the configuration directly. However, it provides much faster
 * access to "linkable" and "transport" information, enhancing runtime speed.
 *
 * This class is a static singleton and is initialized only when first accessed.
 * During initialization it reads and caches the configuration info it handles.
 */
public class FastConfig
{

// ======================= fields ===========================================
// ===========================================================================

/**
 * Parameter name in configuration that attaches a linkable protocol to a
 * protocol. The property can contain multiple protocol names, in one line,
 * separated by non-word characters (eg whitespace or ",").
 * @config
 */
private static final String PAR_LINKABLE = "linkable";

/**
 * Parameter name in configuration that attaches a transport layer protocol to a
 * protocol.
 * @config
 */
private static final String PAR_TRANSPORT = "transport";

/**
 * Parameter name in configuration that attaches a transport layer protocol to a
 * protocol.
 * @config
 */
private static final String PAR_SETTINGS = "settings";

/**
 * Parameter name in configuration that attaches a transport layer protocol to a
 * protocol.
 * @config
 */
private static final String PAR_DESCRIPTOR = "descriptor";

/**
 * This array stores the protocol ids of the {@link peeremu.core.Linkable}
 * protocols that are linked to the protocol given by the array index.
 */
protected static final int[][] links;

/**
 * This array stores the protocol id of the {@link peeremu.transport.TransportAlberto}
 * protocol that is linked to the protocol given by the array index.
 */
protected static final int[] transports;

/**
 * 
 */
protected static Object[] settings;

/**
 * 
 */
protected static Constructor<Descriptor>[] descriptorConstructors;


// ======================= initialization ===================================
// ==========================================================================


/**
 * This static initialization block reads the configuration for information that
 * it understands. Currently it understands property {@value #PAR_LINKABLE}
 * and {@value #PAR_TRANSPORT}.
 * 
 * Protocols' linkable and transport definitions are prefetched
 * and stored in arrays, to enable fast access during simulation.
 *
 * Note that this class does not perform any type checks. The purpose of the
 * class is purely to speed up access to linkable and transport information,
 * by providing a fast alternative to reading directly from the
 * <code>Configuration</code> class.
 */
static {
	String[] names = Configuration.getNames(Configuration.PAR_PROT);
	links = new int[names.length][];
	transports = new int[names.length];
	settings = new Object[names.length];
	descriptorConstructors = new Constructor[names.length];

  int keepCommonPid = CommonState.getPid();
	for (int pid = 0; pid < names.length; ++pid)
	{
	  CommonState.setPid(pid);

	  /*
	   * Setup linkables
	   */
		if (Configuration.contains(names[pid] + "." + PAR_LINKABLE))
		{
			// get string of linkables
			String str = Configuration.getString(names[pid] + "." + PAR_LINKABLE);
			// split around non-word characters
			String[] linkNames = str.split("\\W+");
			links[pid] = new int[linkNames.length];
			for (int j=0; j<linkNames.length; ++j)
				links[pid][j] = Configuration.lookupPid(linkNames[j]);
		}		
		else
			links[pid] = new int[0]; // empty set

		/*
		 * Setup transports
		 */
		if (Configuration.contains(names[pid] + "." + PAR_TRANSPORT))
			transports[pid] = 
			Configuration.getPid(names[pid] + "." + PAR_TRANSPORT);
		else
			transports[pid] = -1;
		
		/*
		 * Setup settings
		 */
		if (Configuration.contains(names[pid] + "." + PAR_SETTINGS))
		  settings[pid] = Configuration.getInstance(names[pid] + "." + PAR_SETTINGS);
		else
		  settings[pid] = null;

		/*
		 * Setup descriptor constructors
		 */
    if (Configuration.contains(names[pid] + "." + PAR_DESCRIPTOR))
    {
      Class cDescriptor = Configuration.getClass(names[pid] + "." + PAR_DESCRIPTOR);
      try
      {
        Class pars[] = {Node.class, int.class};
        descriptorConstructors[pid] = cDescriptor.getConstructor(pars);
      }
      catch (SecurityException e)
      {
        e.printStackTrace();
      }
      catch (NoSuchMethodException e)
      {
        e.printStackTrace();
      }
    }
    else
      descriptorConstructors[pid] = null;
  }
	CommonState.setPid(keepCommonPid);
}

// ---------------------------------------------------------------------

/** to prevent construction */
private FastConfig() {}

// ======================= methods ==========================================
// ==========================================================================


/**
 * Returns true if the given protocol has at least one linkable protocol
 * associated with it, otherwise false.
 */
public static boolean hasLinkable(int pid) { return numLinkables(pid) > 0; }

// ---------------------------------------------------------------------

/**
 * Returns the number of linkable protocols associated with a given protocol.
 */
public static int numLinkables(int pid) { return links[pid].length; }

// ---------------------------------------------------------------------

/**
 * Returns the protocol id of the <code>linkIndex</code>-th linkable used by
 * the protocol identified by pid. Throws an
 * IllegalParameterException if there is no linkable associated with the given
 * protocol: we assume here that this happens when the configuration is
 * incorrect.
 */
public static int getLinkable(int pid, int linkIndex)
{
	if (linkIndex >= numLinkables(pid)) {
		String[] names = Configuration.getNames(Configuration.PAR_PROT);
		throw new IllegalParameterException(names[pid],
			"Protocol " + pid + " has no "+PAR_LINKABLE+
			" parameter with index" + linkIndex);
	}
	return links[pid][linkIndex];
}

//---------------------------------------------------------------------

/**
 * Invokes <code>getLinkable(pid, 0)</code>.
 */
public static int getLinkable(int pid)
{
	return getLinkable(pid, 0);
}

// ---------------------------------------------------------------------

/**
 * Returns true if the given protocol has a transport protocol associated with
 * it, otherwise false.
 */
public static boolean hasTransport(int pid)
{
	return transports[pid] >= 0;
}

// ---------------------------------------------------------------------

/**
 * Returns the id of the transport protocol used by the protocol identified
 * by pid.
 * Throws an IllegalParameterException if there is no transport associated
 * with the given protocol: we assume here that his happens when the
 * configuration is incorrect.
 */
public static int getTransport(int pid)
{
	if (transports[pid] < 0) {
		String[] names = Configuration.getNames(Configuration.PAR_PROT);
		throw new IllegalParameterException(names[pid],
		"Protocol " + pid + " has no "+PAR_TRANSPORT + " parameter");
	}
	return transports[pid];
}


public static Object getSettings(int pid)
{
  return settings[pid];
}

/**
 * Returns a constructor for the Descriptor class defined for the given
 * protocol ID. If no Descriptor class has been defined for this pid, it
 * returns null.
 * 
 * The returned constructor takes as arguments (Node, int).
 */
public static Constructor getDescriptorConstructor(int pid)
{
  return descriptorConstructors[pid];
}

}
