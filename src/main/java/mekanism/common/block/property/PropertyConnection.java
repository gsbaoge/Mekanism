package mekanism.common.block.property;

import java.util.Arrays;

import mekanism.common.tile.transmitter.TileEntitySidedPipe.ConnectionType;
import net.minecraftforge.common.property.IUnlistedProperty;

public class PropertyConnection implements IUnlistedProperty<PropertyConnection>
{
	public static PropertyConnection INSTANCE = new PropertyConnection();
	
	public byte connectionByte;
	public byte transmitterConnections;
	public ConnectionType[] connectionTypes;
	public boolean renderCenter;
	
	public PropertyConnection() {}
	
	public PropertyConnection(byte b, byte b1, ConnectionType[] types, boolean center)
	{
		connectionByte = b;
		transmitterConnections = b1;
		connectionTypes = types;
		renderCenter = center;
	}
	
	@Override
	public String getName() 
	{
		return "connection";
	}

	@Override
	public boolean isValid(PropertyConnection value) 
	{
		return true;
	}

	@Override
	public Class getType() 
	{
		return getClass();
	}

	@Override
	public String valueToString(PropertyConnection value) 
	{
		return Byte.toString(value.connectionByte) + "_" + Byte.toString(value.transmitterConnections) + "_" 
				+ Arrays.toString(value.connectionTypes) + "_" + value.renderCenter;
	}
}
