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
import peernet.graph.Graph;
import peernet.transport.OverlayGraph;





/**
 * This class is the superclass of classes that take a {@link Linkable} protocol
 * or a graph and add edges that define a certain topology. Note that no
 * connections are removed, they are only added. So it can be used in
 * combination with other initializers.
 */
public abstract class WireGraph implements WireControl
{
  // --------------------------------------------------------------------------
  // Parameters
  // --------------------------------------------------------------------------
  /**
   * The {@link Linkable} protocol to operate on. If it is not specified, then
   * operates on {@link #g}. If {@link #g} is null, {@link #execute} throws an
   * Exception. Note that if {@link #g} is set, it will be used irrespective of
   * the setting of the protocol in this field.
   * 
   * @config
   */
  private static final String PAR_PROT = "protocol";
  /**
   * If set, the generated graph is undirected. In other words, for each link
   * (i,j) a link (j,i) will also be added. Defaults to false.
   * 
   * @config
   */
  private static final String PAR_UNDIR = "undir";
  /**
   * Alias for {@value #PAR_UNDIR}.
   * 
   * @config
   */
  private static final String PAR_UNDIR_ALT = "undirected";
  // --------------------------------------------------------------------------
  // Fields
  // --------------------------------------------------------------------------
  /**
   * The protocol we want to wire.
   */
  private final int pid;
  /** If true, edges are added in an undirected fashion. */
  public final boolean undir;
  /**
   * If set (not null), this is the graph to wire. If null, the current overlay
   * is wired each time {@link #execute} is called, as specified by
   * {@value #PAR_PROT}.
   */
  private Graph g = null;



  // --------------------------------------------------------------------------
  // Initialization
  // --------------------------------------------------------------------------
  /**
   * Standard constructor that reads the configuration parameters. Normally
   * invoked by the simulation engine.
   * 
   * @param prefix the configuration prefix for this class
   */
  protected WireGraph(String prefix)
  {
    pid = Configuration.getPid(prefix+"."+PAR_PROT, -10);  // XXX Why -10? Ugly!
    undir = Configuration.contains(prefix+"."+PAR_UNDIR) | Configuration.contains(prefix+"."+PAR_UNDIR_ALT);
  }



  // --------------------------------------------------------------------------
  // Public methods
  // --------------------------------------------------------------------------
  /**
   * Calls method {@link #wire} with the graph {@link #g}, or if null, on the
   * overlay specified by the protocol given by config parameter
   * {@value #PAR_PROT}. If neither {@link #g}, nor {@value #PAR_PROT} is set,
   * throws a RuntimException.
   */
  public final boolean execute()
  {
    Graph gr;
    if (g==null&&pid==-10)
    {
      throw new RuntimeException("Neither a protocol, nor a graph is specified.");
    }
    if (g==null)
      gr = new OverlayGraph(pid, !undir);
    else
      gr = g;
    if (gr.size()==0)
      return false;
    wire(gr);
    return false;
  }



  public final void setGraph(Graph graph)
  {
    g = graph;
  }

  // --------------------------------------------------------------------------
  /**
   * The method that should wire (add edges to) the given graph. Has to be
   * implemented by extending classes
   */
  public abstract void wire(Graph g);
}
