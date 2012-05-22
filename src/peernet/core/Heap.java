/*
 * Copyright (c) 2001 The Anthill Team
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

import java.util.Arrays;
import java.util.PriorityQueue;
import java.util.Random;

import peernet.transport.Address;





/**
 * The Heap data structure used to maintain events "sorted" by scheduled time
 * and to obtain the next event to be executed.
 * 
 * @author Alberto Montresor
 * @version $Revision: 1.8 $
 */
public class Heap  // make "package"
{
  // --------------------------------------------------------------------------
  // Constants
  // --------------------------------------------------------------------------
  /** Initial size */
  private static final int SIZE = 1;
  // --------------------------------------------------------------------------
  // Fields
  // --------------------------------------------------------------------------
  // The following arrays are four heaps ordered by time. The alternative
  // approach (i.e. to store event objects) requires much more memory,
  // and based on some tests that I've done is not really much faster.
  /** Event component of the heap */
  private Object[] events;
  /** Time componenent of the heap */
  private long[] times;
  /** Node component of the heap */
  private Node[] nodes;
  /** Pid component of the heap */
  private byte[] pids;
  /** Src component of the heap */
  private Address[] srcs;
  /** Number of elements */
  private int size;
  /** Singleton event object used to return (event, time, node, pid) tuples */
  private final Event ev = new Event();



  // --------------------------------------------------------------------------
  // Constructor
  // --------------------------------------------------------------------------
  /**
   * Initializes a new heap with the default initial size.
   */
  public Heap()
  {
    this(SIZE);
  }



  // --------------------------------------------------------------------------
  /**
   * Initializes a new heap with the specified initial size.
   */
  public Heap(int size)
  {
    events = new Object[size];
    times = new long[size];
    nodes = new Node[size];
    pids = new byte[size];
    srcs = new Address[size];
  }



  // --------------------------------------------------------------------------
  // Methods
  // --------------------------------------------------------------------------
  /**
   * Returns the current number of events in the system.
   */
  public int size()
  {
    return size;
  }



  // --------------------------------------------------------------------------
  /**
   * Add a new event, to be scheduled at the specified time.
   * 
   * @param time the time at which this event should be scheduled
   * @param event the object decribing the event
   * @param node the node at which the event has to be delivered
   * @param pid the protocol that handles the event
   */
  public void add(long time, Address src, Node node, byte pid, Object event)
  {
    size++;
    int pos = size;
    put(pos, time, src, node, pid, event);

    while (pos>1 && getTime(pos/2)>time)
    {
      swap(pos, pos/2);
      pos = pos/2;
    }
  }



  // --------------------------------------------------------------------------
  /**
   * Removes the first event in the heap and returns it. Note that, to avoid
   * garbage collection, a singleton instance of the Event class is used. This
   * means that data contained in the returned event are overwritten when a new
   * invocation of this method is performed.
   * 
   * @return first event or null if size is zero
   */
  public Event removeFirst()
  {
    if (size==0)
      return null;
    ev.time = times[0];
    ev.event = events[0];
    ev.node = nodes[0];
    ev.pid = pids[0];
    ev.src = srcs[0];
    swap(1, size);
    size--;
    minHeapify(1);
    return ev;
  }

  public long getNextTime()
  {
    if (size() == 0)
      return Long.MAX_VALUE; // Wait indefinitely, till notified
    else
      return times[0];
  }


  // --------------------------------------------------------------------------
  /**
   * Prints the time values contained in the heap.
   */
  public String toString()
  {
    StringBuffer buffer = new StringBuffer();
    buffer.append("[Size: "+size+" Times: ");
    for (int i = 1; i<=size; i++)
    {
      buffer.append(getTime(i)+",");
    }
    buffer.append("]");
    return buffer.toString();
  }

  // --------------------------------------------------------------------------
  /**
   * Container class to be returned when invoking removeFirst.
   */
  public class Event
  {
    long time;
    Address src;
    Node node;
    byte pid;
    Object event;

    public String toString()
    {
      return event+" to node "+node+"prot "+pid+"at "+time;
    }
  }



  // --------------------------------------------------------------------------
  // Private methods
  // --------------------------------------------------------------------------
  /**
 * 
 */
  private void minHeapify(int index)
  {
    // The time to be placed of the current node
    long time = getTime(index);
    // Left, right children of the current index
    int l, r;
    // Their associated time
    long lt, rt;
    // The minimum time between val, lt, rt
    long mintime;
    // The index of the mininum time
    int minindex = index;
    do
    {
      index = minindex;
      mintime = time;
      l = index<<1;
      r = l+1;
      if (l<=size&&(lt = getTime(l))<mintime)
      {
        minindex = l;
        mintime = lt;
      }
      if (r<=size&&(rt = getTime(r))<mintime)
      {
        minindex = r;
        mintime = rt;
      }
      if (minindex!=index)
      {
        swap(minindex, index);
      }
    }
    while (minindex!=index);
  }



  // --------------------------------------------------------------------------
  /**
 * 
 */
  private void swap(int i1, int i2)
  {
    i1--;
    i2--;

    long tt = times[i1];
    times[i1] = times[i2];
    times[i2] = tt;

    Address ts = srcs[i1];
    srcs[i1] = srcs[i2];
    srcs[i2] = ts;

    Node tn = nodes[i1];
    nodes[i1] = nodes[i2];
    nodes[i2] = tn;

    byte tp = pids[i1];
    pids[i1] = pids[i2];
    pids[i2] = tp;

    Object te = events[i1];
    events[i1] = events[i2];
    events[i2] = te;
  }



  // --------------------------------------------------------------------------
  /**
 * 
 */
  private long getTime(int index)
  {
    /* Compute first and second index, and return the value */
    index--;
    return times[index];
  }



  // --------------------------------------------------------------------------
  /**
 * 
 */
  private void put(int index, long time, Address src, Node node, byte pid, Object event)
  {
    index--;
    if (index>=events.length)
    {
      doubleCapacity();
    }
    times[index] = time;
    nodes[index] = node;
    srcs[index] = src;
    pids[index] = pid;
    events[index] = event;
  }



  // --------------------------------------------------------------------------
  /**
 * 
 */
  private void doubleCapacity()
  {
    int oldsize = events.length;
    int newsize = oldsize*2;

    long[] tt = new long[newsize];
    System.arraycopy(times, 0, tt, 0, oldsize);
    times = tt;

    Address[] ts = new Address[newsize];
    System.arraycopy(srcs, 0, ts, 0, oldsize);
    srcs = ts;

    Node[] tn = new Node[newsize];
    System.arraycopy(nodes, 0, tn, 0, oldsize);
    nodes = tn;

    byte[] tp = new byte[newsize];
    System.arraycopy(pids, 0, tp, 0, oldsize);
    pids = tp;

    Object[] te = new Object[newsize];
    System.arraycopy(events, 0, te, 0, oldsize);
    events = te;
  }



  // --------------------------------------------------------------------------
  // Testing
  // --------------------------------------------------------------------------
  public static void main(String[] args)
  {
    Random random = new Random();
    Heap heap = new Heap();
    int rep = 1000000;
    if (args.length>0)
      rep = Integer.parseInt(args[0]);
    long[] values1 = new long[rep];
    long[] values2 = new long[rep];
    long[] values3 = new long[rep];
    for (int i = 0; i<rep; i++)
      values1[i] = random.nextInt(1000000000);
    long time1 = System.currentTimeMillis();
    for (int i = 0; i<rep; i++)
      heap.add(values1[i], null, null, (byte) 1, null);
    long time2 = System.currentTimeMillis();
    System.out.println("Inserting: "+(time2-time1));
    time1 = System.currentTimeMillis();
    for (int i = 0; i<rep; i++)
      values2[i] = heap.removeFirst().time;
    time2 = System.currentTimeMillis();
    System.out.println("Removing: "+(time2-time1));
    Event ev = heap.new Event();
    PriorityQueue<Event> pq = new PriorityQueue<Heap.Event>();
    time1 = System.currentTimeMillis();
    for (int i = 0; i<rep; i++)
    {
      ev.time = values1[i];
      pq.add(ev);
    }
    time2 = System.currentTimeMillis();
    System.out.println("PQ Inserting: "+(time2-time1));
    time1 = System.currentTimeMillis();
    for (int i = 0; i<rep; i++)
      values3[i] = pq.remove().time;
    time2 = System.currentTimeMillis();
    System.out.println("PQ Removing: "+(time2-time1));
    Arrays.sort(values1);
    for (int i = 0; i<rep; i++)
    {
      System.out.println(values1[i]+" "+values3[i]);
      if (values1[i]!=values2[i])
        System.out.print("+");
      if (values1[i]!=values3[i])
        System.out.print("-");
    }
    System.out.println("Done!");
  }
} // END Heap
