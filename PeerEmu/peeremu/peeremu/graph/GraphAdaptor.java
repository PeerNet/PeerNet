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

package peeremu.graph;

import java.util.ArrayList;
import java.util.BitSet;

/**
* Speeds up {@link ConstUndirGraph#isEdge} by storing the links in an
* adjecency matrix (in fact in a triangle).
* Its memory consumption is huge but it's much faster if the isEdge method
* of the original underlying graph is slow.
*/
public class GraphAdaptor extends ConstUndirGraph
{

private BitSet[] edgeMatrix;

private enum Dir
{
	NORMAL,
	REVERSE,
	BIDIRECTIONAL;
};

private Dir dir;


// ======================= initializarion ==========================
// =================================================================


/** Calls super constructor */
public GraphAdaptor(Graph graph)
{
	super(graph);
}

// -----------------------------------------------------------------

protected void initGraph()
{
	final int max = g.size();
	edgeMatrix = new BitSet[max];
	for (int i=0; i<max; ++i)
	{
		in[i] = new ArrayList<Integer>();
		edgeMatrix[i] = new BitSet(dir==Dir.BIDIRECTIONAL ? i : max);
	}

	for( int i = 0; i < max; ++i )
	{
		for( Integer out : g.getNeighbours(i) )
		{
			// Add out-neighbor of i as in-neighbor of j 
			int j = out.intValue();
			in[j].add(i);
			addEdge(i, j);
		}
	}
}


// ============================ Graph functions ====================
// =================================================================


/**
 * Add the link i->j to the edgeMatrix. If assuming an undirected graph,
 * if adds either the link i->j (if i>j) or the link j->i (if i<=j).
 * 
 */
private void addEdge(int i, int j)
{
//	if (undirected)
//	{
//		if( i > j )
//			edgeMatrix[i].set(j);
//		else
//			edgeMatrix[j].set(i);
//	}
//	else
//		edgeMatrix[i].set(j);
}


public boolean isEdge(int i, int j)
{
	// make sure i>j
	if (i<j)
	{
		int ii=i;
		i=j;
		j=ii;
	}
	return edgeMatrix[i].get(j);
}
}

