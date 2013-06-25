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
package peernet.dynamics;

import peernet.config.Configuration;
import peernet.core.*;





/**
 * Initializes a node's neighbor list with a fixed center.
 */
public class StarNI implements NodeInitializer
{
  // ========================= fields =================================
  // ==================================================================
  /**
   * The protocol to operate on.
   * 
   * @config
   */
  private static final String PAR_PROT = "protocol";
  /**
   * The protocol we want to wire
   */
  protected final int pid;
  /**
   * Used as center: nodes will link to this node.
   */
  protected Node center = null;



  // ==================== initialization ==============================
  // ===================================================================
  public StarNI(String prefix)
  {
    pid = Configuration.getPid(prefix+"."+PAR_PROT);
  }



  // ===================== public methods ==============================
  // ===================================================================
  /**
   * Adds a link to a fixed node, the center. This fixed node remains the same
   * throughout consecutive calls to this method. If the center fails in the
   * meantime, a new one is chosen so care should be taken. The center is the
   * first node that is not down (starting from node 0, 1, etc) at the time of
   * the first call to the function. When a new center needs to be chosen (the
   * current one is down), the new center is again the first one which is up. If
   * no nodes are up, the node with the last index is set as center, and
   * selection of a new center is always attempted when this method is called
   * again.
   */
  public void initialize(Node n)
  {
    if (Network.size()==0)
      return;
    for (int i = 0; (center==null || !center.isUp()) && i<Network.size(); ++i)
      center = Network.get(i);
    Descriptor centerDescriptor = center.getProtocol(pid).createDescriptor();
    ((Linkable) n.getProtocol(pid)).addNeighbor(centerDescriptor);
  }
}
