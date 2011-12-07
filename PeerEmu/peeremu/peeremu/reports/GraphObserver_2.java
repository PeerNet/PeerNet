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
package peeremu.reports;

import peeremu.config.Configuration;
import peeremu.config.IllegalParameterException;
import peeremu.core.CommonState;
import peeremu.core.Control;
import peeremu.core.OverlayGraph;
import peeremu.graph.ConstUndirGraph;
import peeremu.graph.FastUndirGraph;
import peeremu.graph.Graph;
import peeremu.graph.GraphAlgorithms;
import peeremu.graph.ReverseGraph;





/**
 * Class that provides functionality for observing graphs. It can efficiently
 * create an undirected version of the graph, making sure it is updated only
 * when the simulation has advanced already, and provides some common
 * parameters.
 */
public abstract class GraphObserver_2 implements Control
{
  // ===================== fields =======================================
  // ====================================================================
  /**
   * The protocol to operate on.
   * 
   * @config
   */
  private static final String PAR_PROT = "protocol";
  /**
   * If defined, the undirected version of the graph will be analyzed. Not
   * defined by default.
   * 
   * @config
   */
  protected static final String PAR_UNDIR = "undir";
  /**
   * If defined, the reverse version of the graph will be analyzed. Not defined
   * by default. Cannot be defined along with {@value #PAR_UNDIR}.
   * 
   * @config
   */
  protected static final String PAR_REVERSE = "reverse";
  /**
   * Alias for {@value #PAR_UNDIR}.
   * 
   * @config
   */
  private static final String PAR_UNDIR_ALT = "undirected";
  /**
   * If defined, the undirected version of the graph will be stored using much
   * more memory but observers will be in general a few times faster. As a
   * consequence, it will not work with large graphs. Not defined by default. It
   * is a static property, that is, it affects all graph observers that are used
   * in a simulation. That is, it is not a parameter of any observer, the name
   * should be specified as a standalone property.
   * 
   * @config
   */
  private static final String PAR_FAST = "graphobserver.fast";
  /** The name of this observer in the configuration */
  protected final String name;
  protected final int pid;
  protected final boolean undir;
  protected final boolean reverse;
  protected final GraphAlgorithms ga = new GraphAlgorithms();
  protected Graph g;
  // ---------------------------------------------------------------------
  private static int lastpid = -1234;
  private static long time = -1234;
  private static int phase = -1234;
  private static int ctime = -1234;
  private static Graph dirg;
  private static Graph undirg;
  private static Graph reverseg;
  private static boolean fast;
  /** If any extending class defines undir we need to maintain an undir graph. */
  private static boolean needUndir = false;
  /**
   * If any extending class defines reverse we need to maintain a reverse graph.
   */
  private static boolean needReverse = false;



  // ===================== initialization ================================
  // =====================================================================
  /**
   * Standard constructor that reads the configuration parameters. Invoked by
   * the simulation engine.
   * 
   * @param name the configuration prefix for this class
   */
  protected GraphObserver_2(String name)
  {
    this.name = name;
    pid = Configuration.getPid(name+"."+PAR_PROT);
    undir = (Configuration.contains(name+"."+PAR_UNDIR)|Configuration.contains(name+"."+PAR_UNDIR_ALT));
    reverse = (Configuration.contains(name+"."+PAR_REVERSE));
    if (undir&&reverse)
    {
      throw new IllegalParameterException(name+"."+PAR_UNDIR+", "+name+"."+PAR_REVERSE,
          "Parameters must not be defined together.");
    }
    GraphObserver_2.needUndir = (GraphObserver_2.needUndir||undir);
    GraphObserver_2.needReverse = (GraphObserver_2.needReverse||reverse);
  }



  // ====================== methods ======================================
  // =====================================================================
  /**
   * Sets {@link #g}. It MUST be called by any implementation of
   * {@link #execute()} before doing anything else. Attempts to initialize
   * {@link #g} from a pre-calculated graph stored in a static field, but first
   * it checks whether it needs to be updated. If the simulation time has
   * progressed or it was calculated for a different protocol, then updates this
   * static graph as well. The purpose of this mechanism is to save the time of
   * constructing the graph if many observers are run on the same graph. Time
   * savings can be very significant if the undirected version of the same graph
   * is observed by many observers.
   */
  protected void updateGraph()
  {
    if (CommonState.getTime() != GraphObserver_2.time ||
        CommonState.getPhase() != GraphObserver_2.phase ||
        pid != GraphObserver_2.lastpid)
    {
      // we need to update the graphs
      GraphObserver_2.lastpid = pid;
      GraphObserver_2.time = CommonState.getTime();
      GraphObserver_2.phase = CommonState.getPhase();
      GraphObserver_2.dirg = new OverlayGraph(pid);
      if (GraphObserver_2.needUndir)
      {
        if (fast)
          GraphObserver_2.undirg = new FastUndirGraph(GraphObserver_2.dirg);
        else
          GraphObserver_2.undirg = new ConstUndirGraph(GraphObserver_2.dirg);
      }
      if (GraphObserver_2.needReverse)
      {
        GraphObserver_2.reverseg = new ReverseGraph(GraphObserver_2.dirg);
      }
    }
    if (undir)
      g = GraphObserver_2.undirg;
    else if (reverse)
      g = GraphObserver_2.reverseg;
    else
      g = GraphObserver_2.dirg;
  }
}
