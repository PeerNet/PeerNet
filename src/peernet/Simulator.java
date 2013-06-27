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
package peernet;

import java.io.PrintStream;

import peernet.config.Configuration;
import peernet.config.IllegalParameterException;
import peernet.config.MissingParameterException;
import peernet.config.ParsedProperties;
import peernet.core.CommonState;
import peernet.core.Engine;
import peernet.core.Engine.Type;
import peernet.dynamics.BootstrapServer;





/**
 * This is the main entry point to peernet. This class loads configuration and
 * detects the simulation type. According to this, it invokes the appropriate
 * simulator. The known simulators at this moment, along with the way to detect
 * them are the following:
 * <ul>
 * <li>{@link CDSimulator}: if {@link CDSimulator#isConfigurationCycleDriven}
 * returns true</li>
 * <li>{@link Engine}: if {@link Engine#isConfigurationEventDriven}
 * returns true</li>
 * </ul>
 * This list represents the order in which these alternatives are checked. That
 * is, if more than one return true, then the first will be taken. Note that
 * this class checks only for these clues and does not check if the
 * configuration is consistent or valid.
 * 
 * @see #main
 */
public class Simulator
{
  // ========================== static constants ==========================
  // ======================================================================
  /** {@link CDSimulator} */
//  public static final int CDSIM = 0; //XXX remove!
  /** {@link Engine} */
  public static final int EDSIM = 1;
  /** Unknown simulator */
  public static final int UNKNOWN = -1;
  /** the class names of simulators used */
  protected static final String[] simName = { "peersim.cdsim.CDSimulator", "peersim.edsim.EDSimulator", };

  /**
   * If present, this parameter activates the redirection of the standard output
   * to a given PrintStream. This comes useful for processing the output of the
   * simulation from within the simulator.
   * 
   * @config
   */
  public static final String PAR_REDIRECT = "simulation.stdout";



  // ----------------------------------------------------------------------
  /**
   * Loads the configuration and executes the experiments. The number of
   * independent experiments is given by config parameter {@value #PAR_EXPS}. In
   * all experiments the configuration is the same, only the random seed is not
   * re-initialized between experiments.
   * <p>
   * Loading the configuration is currently done with the help of constructing
   * an instance of {@link ParsedProperties} using the constructor
   * {@link ParsedProperties#ParsedProperties(String[])}. The parameter
   * <code>args</code> is simply passed to this class. This class is then used
   * to initialize the configuration.
   * <p>
   * After loading the configuration, the experiments are run by invoking the
   * appropriate engine, which is identified as follows:
   * <ul>
   * <li>{@link CDSimulator}: if {@link CDSimulator#isConfigurationCycleDriven}
   * returns true</li>
   * <li>{@link Engine}: if {@link Engine#isConfigurationEventDriven}
   * returns true</li>
   * </ul>
   * <p>
   * This list represents the order in which these alternatives are checked.
   * That is, if more than one return true, then the first will be taken. Note
   * that this class checks only for these clues and does not check if the
   * configuration is consistent or valid.
   * 
   * @param args passed on to
   *          {@link ParsedProperties#ParsedProperties(String[])}
   * @see ParsedProperties
   * @see Configuration
   * @see CDSimulator
   * @see Engine
   */
  public static void main(String[] args)
  {
    long time = System.currentTimeMillis();
    System.err.println("Simulator: loading configuration");
    Configuration.setConfig(new ParsedProperties(args));
    PrintStream newout = (PrintStream) Configuration.getInstance(PAR_REDIRECT, System.out);
    if (newout!=System.out)
      System.setOut(newout);

    if (Engine.getType() == Type.COORDINATOR)
    {
      BootstrapServer bootstrapper = new BootstrapServer();
    }
    else
    {
      try
      {
        System.err.println("Random seed: "+CommonState.r.getLastSeed());
        System.out.println("\n\n");
        Engine engine = Engine.instance();
        engine.startExperiment();
      }
      catch (MissingParameterException e)
      {
        System.err.println(e+"");
        System.exit(1);
      }
      catch (IllegalParameterException e)
      {
        System.err.println(e+"");
        System.exit(1);
      }
      // undocumented testing capabilities
      if (Configuration.contains("__t"))
        System.out.println(System.currentTimeMillis()-time);
      // if(Configuration.contains("__x")) Network.test();
    }
  }
}
