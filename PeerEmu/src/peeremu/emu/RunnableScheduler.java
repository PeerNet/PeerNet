/*
 * Copyright (c) 2003 The BISON Project
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
		
package peeremu.emu;

import peeremu.config.Configuration;

// XXX a quite primitive scheduler, should be able to be configured
// much more flexibly using a simlpe syntax for time ranges.
/**
* A binary function over the time points. That is,
* for each time point (cycle in the simulation) returns a boolean
* value.
*
* <p>The concept of time is understood as follows. Time point 0 refers to
* the time before cycle 0. In general, time point i refers to the time
* before cycle i. The special time index FINAL refers to the time
* after the last cycle. Note that the index of the last cycle is not known
* in advance, because the simulation can stop at any time, based on other
* components.
*
* <p>In this simple implementation the valid times will be
* <tt>from, from+step, from+2*step, etc,</tt>
* where the last element is strictly less than <tt>until</tt>. If FINAL is
* defined, it is also added to the set of active time points.
*/
public class RunnableScheduler implements Cloneable
{


// ========================= fields =================================
// ==================================================================


/** 
* Period of the observer, in milliseconds. Defaults to -1 (non-periodic).
*/
public static final String PAR_PERIOD = "period";

/**
* Moment for one-time execution, in milliseconds from experiment start.
* Defaults to -1.
*/
public static final String PAR_AT = "at";

/** 
* Defaults to 0.
*/
public static final String PAR_FROM = "from";

/** 
* Defaults to <tt>Integer.MAX_VALUE</tt>.
*/
public static final String PAR_UNTIL = "until";


protected final int period;

protected final int at;

protected final int from;

protected final int until;

protected Runnable command;

// ==================== initialization ==============================
// ==================================================================

public RunnableScheduler(String prefix)
{
  period = Configuration.getInt(prefix+"."+PAR_PERIOD, -1);
  at = Configuration.getInt(prefix+"."+PAR_AT,-1);
  from = Configuration.getInt(prefix+"."+PAR_FROM,0);
  until = Configuration.getInt(prefix+"."+PAR_UNTIL,Integer.MAX_VALUE);

  if (period==-1 && at==-1)
    throw new IllegalArgumentException(
        "One of \""+prefix+"."+PAR_PERIOD+
        "\" and \""+prefix+"."+PAR_AT+"\" should be defined");

  if ( at!=-1 && (at<from || at>until) )
    throw new IllegalArgumentException(
        "\"at\" ("+at+") should be between \"from\" ("+from+")");
  
}


// ===================== public methods ==============================
// ===================================================================


public boolean active()
{
  long time = System.currentTimeMillis() - MTSimulator.startTime;
  if (time<from || time>until)
    return false;
  else
    return true;
}

public boolean periodic() { return period >= 0; }
}


