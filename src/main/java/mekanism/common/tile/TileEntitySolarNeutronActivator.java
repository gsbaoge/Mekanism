package mekanism.common.tile;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;

import mekanism.api.Coord4D;
import mekanism.api.Range4D;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasRegistry;
import mekanism.api.gas.GasStack;
import mekanism.api.gas.GasTank;
import mekanism.api.gas.IGasHandler;
import mekanism.api.gas.ITubeConnection;
import mekanism.common.Mekanism;
import mekanism.common.base.IActiveState;
import mekanism.common.base.IBoundingBlock;
import mekanism.common.base.IDropperHandler;
import mekanism.common.base.IRedstoneControl;
import mekanism.common.base.ISustainedData;
import mekanism.common.network.PacketTileEntity.TileEntityMessage;
import mekanism.common.recipe.RecipeHandler;
import mekanism.common.recipe.inputs.GasInput;
import mekanism.common.recipe.machines.SolarNeutronRecipe;
import mekanism.common.util.MekanismUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntitySolarNeutronActivator extends TileEntityContainerBlock implements IRedstoneControl, IBoundingBlock, IGasHandler, ITubeConnection, IActiveState, ISustainedData, IDropperHandler
{
	public GasTank inputTank = new GasTank(MAX_GAS);
	public GasTank outputTank = new GasTank(MAX_GAS);
	
	public static final int MAX_GAS = 10000;
	
	public int updateDelay;
	
	public boolean isActive;

	public boolean clientActive;
	
	public int gasOutput = 256;
	
	public SolarNeutronRecipe cachedRecipe;
	
	/** This machine's current RedstoneControl type. */
	public RedstoneControl controlType = RedstoneControl.DISABLED;
	
	public TileEntitySolarNeutronActivator()
	{
		super("SolarNeutronActivator");
		inventory = new ItemStack[3];
	}

	@Override
	public void onUpdate() 
	{
		if(worldObj.isRemote && updateDelay > 0)
		{
			updateDelay--;

			if(updateDelay == 0 && clientActive != isActive)
			{
				isActive = clientActive;
				MekanismUtils.updateBlock(worldObj, xCoord, yCoord, zCoord);
			}
		}
		
		if(!worldObj.isRemote)
		{
			if(updateDelay > 0)
			{
				updateDelay--;

				if(updateDelay == 0 && clientActive != isActive)
				{
					Mekanism.packetHandler.sendToReceivers(new TileEntityMessage(Coord4D.get(this), getNetworkedData(new ArrayList())), new Range4D(Coord4D.get(this)));
				}
			}
			
			//Buckets
			
			SolarNeutronRecipe recipe = getRecipe();

			if(canOperate(recipe) && MekanismUtils.canFunction(this))
			{
				setActive(true);
				
				operate(recipe);
			}
			else {
				setActive(false);
			}

			if(outputTank.getGas() != null)
			{
				GasStack toSend = new GasStack(outputTank.getGas().getGas(), Math.min(outputTank.getStored(), gasOutput));

				TileEntity tileEntity = Coord4D.get(this).getFromSide(ForgeDirection.getOrientation(facing)).getTileEntity(worldObj);

				if(tileEntity instanceof IGasHandler)
				{
					if(((IGasHandler)tileEntity).canReceiveGas(ForgeDirection.getOrientation(facing).getOpposite(), outputTank.getGas().getGas()))
					{
						outputTank.draw(((IGasHandler)tileEntity).receiveGas(ForgeDirection.getOrientation(facing).getOpposite(), toSend, true), true);
					}
				}
			}
		}
	}
	
	public SolarNeutronRecipe getRecipe()
	{
		GasInput input = getInput();
		
		if(cachedRecipe == null || !input.testEquality(cachedRecipe.getInput()))
		{
			cachedRecipe = RecipeHandler.getSolarNeutronRecipe(getInput());
		}
		
		return cachedRecipe;
	}

	public GasInput getInput()
	{
		return new GasInput(inputTank.getGas());
	}

	public boolean canOperate(SolarNeutronRecipe recipe)
	{
		return recipe != null && recipe.canOperate(inputTank, outputTank);
	}

	public void operate(SolarNeutronRecipe recipe)
	{
		recipe.operate(inputTank, outputTank);
	}
	
	@Override
	public void handlePacketData(ByteBuf dataStream)
	{
		super.handlePacketData(dataStream);

		isActive = dataStream.readBoolean();
		controlType = RedstoneControl.values()[dataStream.readInt()];

		if(dataStream.readBoolean())
		{
			inputTank.setGas(new GasStack(GasRegistry.getGas(dataStream.readInt()), dataStream.readInt()));
		}
		else {
			inputTank.setGas(null);
		}

		if(dataStream.readBoolean())
		{
			outputTank.setGas(new GasStack(GasRegistry.getGas(dataStream.readInt()), dataStream.readInt()));
		}
		else {
			outputTank.setGas(null);
		}

		MekanismUtils.updateBlock(worldObj, xCoord, yCoord, zCoord);
	}

	@Override
	public ArrayList getNetworkedData(ArrayList data)
	{
		super.getNetworkedData(data);

		data.add(isActive);
		data.add(controlType.ordinal());

		if(inputTank.getGas() != null)
		{
			data.add(true);
			data.add(inputTank.getGas().getGas().getID());
			data.add(inputTank.getStored());
		}
		else {
			data.add(false);
		}
		
		if(outputTank.getGas() != null)
		{
			data.add(true);
			data.add(outputTank.getGas().getGas().getID());
			data.add(outputTank.getStored());
		}
		else {
			data.add(false);
		}

		return data;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbtTags)
	{
		super.readFromNBT(nbtTags);

		isActive = nbtTags.getBoolean("isActive");
		controlType = RedstoneControl.values()[nbtTags.getInteger("controlType")];

		inputTank.read(nbtTags.getCompoundTag("inputTank"));
		outputTank.read(nbtTags.getCompoundTag("outputTank"));
	}

	@Override
	public void writeToNBT(NBTTagCompound nbtTags)
	{
		super.writeToNBT(nbtTags);

		nbtTags.setBoolean("isActive", isActive);
		nbtTags.setInteger("controlType", controlType.ordinal());
		
		nbtTags.setTag("inputTank", inputTank.write(new NBTTagCompound()));
		nbtTags.setTag("outputTank", outputTank.write(new NBTTagCompound()));
	}

	@Override
	public boolean canSetFacing(int i)
	{
		return i != 0 && i != 1;
	}

	@Override
	public void onPlace() 
	{
		MekanismUtils.makeBoundingBlock(worldObj, xCoord, yCoord+1, zCoord, Coord4D.get(this));
	}

	@Override
	public void onBreak() 
	{
		worldObj.setBlockToAir(xCoord, yCoord+1, zCoord);
		worldObj.setBlockToAir(xCoord, yCoord, zCoord);
	}

	@Override
	public int receiveGas(ForgeDirection side, GasStack stack, boolean doTransfer) 
	{
		return 0;
	}

	@Override
	public GasStack drawGas(ForgeDirection side, int amount, boolean doTransfer) 
	{
		return null;
	}

	@Override
	public boolean canReceiveGas(ForgeDirection side, Gas type) 
	{
		return false;
	}

	@Override
	public boolean canDrawGas(ForgeDirection side, Gas type) 
	{
		return false;
	}
	
	@Override
	public boolean canTubeConnect(ForgeDirection side) 
	{
		return side == ForgeDirection.getOrientation(facing) || side == ForgeDirection.DOWN;
	}

	@Override
	public void writeSustainedData(ItemStack itemStack)
	{
		if(inputTank.getGas() != null)
		{
			itemStack.stackTagCompound.setTag("inputTank", inputTank.getGas().write(new NBTTagCompound()));
		}
		
		if(outputTank.getGas() != null)
		{
			itemStack.stackTagCompound.setTag("outputTank", outputTank.getGas().write(new NBTTagCompound()));
		}
	}
	
	@Override
	public void readSustainedData(ItemStack itemStack)
	{
		inputTank.setGas(GasStack.readFromNBT(itemStack.stackTagCompound.getCompoundTag("inputTank")));
		outputTank.setGas(GasStack.readFromNBT(itemStack.stackTagCompound.getCompoundTag("outputTank")));
	}

	@Override
	public RedstoneControl getControlType()
	{
		return controlType;
	}

	@Override
	public void setControlType(RedstoneControl type)
	{
		controlType = type;
		MekanismUtils.saveChunk(this);
	}

	@Override
	public boolean canPulse() 
	{
		return false;
	}

	@Override
	public boolean getActive()
	{
		return isActive;
	}
	
	@Override
	public void setActive(boolean active)
	{
		isActive = active;

		if(clientActive != active && updateDelay == 0)
		{
			Mekanism.packetHandler.sendToReceivers(new TileEntityMessage(Coord4D.get(this), getNetworkedData(new ArrayList())), new Range4D(Coord4D.get(this)));

			updateDelay = 10;
			clientActive = active;
		}
	}

	@Override
	public boolean renderUpdate() 
	{
		return false;
	}

	@Override
	public boolean lightUpdate() 
	{
		return true;
	}
	
	@Override
	public Object[] getTanks() 
	{
		return new Object[] {inputTank, outputTank};
	}
}