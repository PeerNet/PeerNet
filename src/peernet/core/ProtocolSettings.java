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

import java.lang.reflect.Constructor;

import peernet.config.Configuration;
import peernet.config.IllegalParameterException;
import peernet.transport.Address;





/**
 * Reads configuration regarding relations between protocols.
 * 
 * Technically, this class is not necessary because protocols could access the
 * configuration directly. However, it provides much faster access to "linkable"
 * and "transport" information, enhancing runtime speed.
 * 
 * This class is a static singleton and is initialized only when first accessed.
 * During initialization it reads and caches the configuration info it handles.
 */
public class ProtocolSettings
{
  /**
   * Parameter name in configuration that attaches a linkable protocol to a
   * protocol. The property can contain multiple protocol names, in one line,
   * separated by non-word characters (eg whitespace or ",").
   * 
   * @config
   */
  private static final String PAR_LINKABLE = "linkable";

  /**
   * Protocol prefix.
   */
  private static final String PAR_PROTOCOL = "protocol";

  /**
   * Parameter name in configuration that attaches a transport layer protocol to
   * a protocol.
   * 
   * @config
   */
  private static final String PAR_DESCRIPTOR = "descriptor";

  /**
   * This array stores the protocol ids of the {@link peernet.core.Linkable}
   * protocols that are linked to the protocol given by the array index.
   */
  private final int[] links;

  /**
   * 
   */
  private final Constructor<Descriptor> descriptorConstructor;

  /**
   * The pid of this protocol instance.
   */
  private final int pid;


  /**
   * This static initialization block reads the configuration for information
   * that it understands. Currently it understands property
   * {@value #PAR_LINKABLE} and {@value #PAR_TRANSPORT}.
   * 
   * Protocols' linkable and transport definitions are prefetched and stored in
   * arrays, to enable fast access during simulation.
   * 
   * Note that this class does not perform any type checks. The purpose of the
   * class is purely to speed up access to linkable and transport information,
   * by providing a fast alternative to reading directly from the
   * <code>Configuration</code> class.
   */
  public ProtocolSettings(String prefix)
  {
    // First remove the ".settings" from the prefix
    prefix = prefix.replace("."+Protocol.PAR_SETTINGS, "");

    // First find and store the pid for this protocol
    String protocolName = prefix.toLowerCase().replace(PAR_PROTOCOL+".", "");
    pid = Configuration.lookupPid(protocolName);

    // Setup linkables
    if (Configuration.contains(prefix+"."+PAR_LINKABLE))
    {
      // get string of linkables
      String str = Configuration.getString(prefix+"."+PAR_LINKABLE);

      // split around non-word characters
      String[] linkNames = str.split("\\W+");
      links = new int[linkNames.length];
      for (int i=0; i<linkNames.length; i++)
        links[i] = Configuration.lookupPid(linkNames[i]);
    }
    else
      links = new int[0]; // empty set



    // Setup descriptor constructors
    Class<Descriptor> cDescriptor = Configuration.getClass(prefix+"."+PAR_DESCRIPTOR);
    Class pars[] = { Node.class, int.class };
    Constructor<Descriptor> constr = null;
    Constructor<Descriptor> constrGeneric = null;
    try
    {
      constr = cDescriptor.getConstructor(pars);
    }
    catch (SecurityException e)
    {
      e.printStackTrace();
    }
    catch (NoSuchMethodException e)
    {
      e.printStackTrace();
    }
    descriptorConstructor = constr;
  }



  /**
   * Get the pid of this protocol instance.
   */
  public int getPid()
  {
    return pid;
  }



  // ======================= methods ==========================================
  // ==========================================================================
  /**
   * Returns true if the given protocol has at least one linkable protocol
   * associated with it, otherwise false.
   */
  public boolean hasLinkable()
  {
    return numLinkables()>0;
  }



  /**
   * Returns the number of linkable protocols associated with a given protocol.
   */
  public int numLinkables()
  {
    return links.length;
  }



  /**
   * Returns the protocol id of the <code>linkIndex</code>-th linkable used by
   * the protocol identified by pid. Throws an IllegalParameterException if
   * there is no linkable associated with the given protocol: we assume here
   * that this happens when the configuration is incorrect.
   */
  public int getLinkable(int linkIndex)
  {
    if (linkIndex >= numLinkables())
    {
      String[] names = Configuration.getNames(PAR_PROTOCOL);
      throw new IllegalParameterException(names[pid], "Protocol "+pid+
          " has no "+PAR_LINKABLE+" parameter with index"+linkIndex);
    }
    return links[linkIndex];
  }



  /**
   * Invokes <code>getLinkable(pid, 0)</code>.
   */
  public int getLinkable()
  {
    return getLinkable(0);
  }



  /**
   * Returns a constructor for the Descriptor class defined for the given
   * protocol ID. If no Descriptor class has been defined for this pid, it
   * returns null.
   * 
   * The returned constructor takes as arguments (Node, int).
   */
  public Constructor getDescriptorConstructor()
  {
    return descriptorConstructor;
  }
}
