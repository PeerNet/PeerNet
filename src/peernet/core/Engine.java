/*
 * Created on Feb 27, 2012 by Spyros Voulgaris
 *
 */
package peernet.core;

import peernet.config.Configuration;
import peernet.config.IllegalParameterException;

public class Engine
{
  private static final String PREFIX = "engine";
  private static final String PAR_TYPE = "type";
  
  private static final Type type;
  private static final AddressType addressType;

  public enum Type
  {
    SIM, EMU, NET;
  }

  public enum AddressType
  {
    SIM, NET;
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
    else if (typeStr.equals("net"))
    {
      type = Type.NET;
      addressType = AddressType.NET;
    }
    else
      throw new IllegalParameterException(PREFIX+"."+PAR_TYPE, "Possible types: sim, emu, net");
  }

  public static Type getType()
  {
    return type;
  }

  public static AddressType getAddressType()
  {
    return addressType;
  }
}
