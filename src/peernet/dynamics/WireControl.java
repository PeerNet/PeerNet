/*
 * Created on Nov 27, 2012 by Spyros Voulgaris
 *
 */
package peernet.dynamics;

import peernet.core.Control;
import peernet.graph.Graph;

public interface WireControl extends Control
{
  /**
   * If set (not null), then wire this graph. If null, the current overlay
   * is wired each time {@link #execute} is called, as specified by
   * {@value #PAR_PROT}.
   *
   * @param graph
   */
  void setGraph(Graph graph);
}
