/*
 * Created on Feb 27, 2012 by Spyros Voulgaris
 *
 */
package peeremu.core;

import peeremu.config.Configuration;
import peeremu.config.IllegalParameterException;

public class Engine
{
  private static final String PREFIX = "engine";
  private static final String PAR_TYPE = "type";
  
  private static final Type type;
  private static final AddressType addressType;

  public enum Type
  {
    SIM, EMU, REAL;
  }

  public enum AddressType
  {
    SIM, REAL;
  }

  static
  {
    String typeStr = Configuration.getString(PREFIX+"."+PAR_TYPE, "");
    if (typeStr.equals("sim"))
    {
      type = Type.SIM;
      addressType = AddressType.SIM;
    }
    else if (typeStr.equals("emu"))
    {
      type = Type.EMU;
      addressType = AddressType.SIM;
    }
    else if (typeStr.equals("real"))
    {
      type = Type.REAL;
      addressType = AddressType.REAL;
    }
    else
      throw new IllegalParameterException(PREFIX+"."+PAR_TYPE, "Possible types: sim, emu, real");
  }

  public static Type getType()
  {
    return type;
  }

  public static AddressType getAddressType()
  {
    return addressType;
  }

  public static boolean isAddressTypeReal()
  {
    return addressType == AddressType.REAL;
  }

  public static boolean isAddressTypeSim()
  {
    return addressType == AddressType.SIM;
  }
}
