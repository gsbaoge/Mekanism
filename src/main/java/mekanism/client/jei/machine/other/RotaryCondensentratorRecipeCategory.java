package mekanism.client.jei.machine.other;

import mekanism.api.gas.GasStack;
import mekanism.client.jei.BaseRecipeCategory;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiFluidStackGroup;
import mezz.jei.api.gui.IGuiIngredientGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;

public class RotaryCondensentratorRecipeCategory extends BaseRecipeCategory
{
	public IDrawable background;
	
	public RotaryCondensentratorRecipeWrapper tempRecipe;
	
	public RotaryCondensentratorRecipeCategory(IGuiHelper helper)
	{
		super(helper, "mekanism:gui/nei/GuiRotaryCondensentrator.png", "rotary_condensentrator", "nei.rotaryCondensentrator", null);

		xOffset = 3;
		yOffset = 12;
		
		background = guiHelper.createDrawable(new ResourceLocation(guiTexture), xOffset, yOffset, 170, 71);
	}
	
	@Override
	public void drawExtras(Minecraft minecraft)
	{
		super.drawExtras(minecraft);
		
		if(tempRecipe.condensentrating)
		{
			drawTexturedRect(64-xOffset, 39-yOffset, 176, 123, 48, 8);
		}
		else {
			drawTexturedRect(64-xOffset, 39-yOffset, 176, 115, 48, 8);
		}
	}
	
	@Override
	public IDrawable getBackground()
	{
		return background;
	}
	
	@Override
	public void setRecipe(IRecipeLayout recipeLayout, IRecipeWrapper recipeWrapper, IIngredients ingredients)
	{
		if(recipeWrapper instanceof RotaryCondensentratorRecipeWrapper)
		{
			tempRecipe = (RotaryCondensentratorRecipeWrapper)recipeWrapper;
		}
		
		IGuiFluidStackGroup fluidStacks = recipeLayout.getFluidStacks();
		IGuiIngredientGroup gasStacks = recipeLayout.getIngredientsGroup(GasStack.class);
		
		fluidStacks.init(0, !tempRecipe.condensentrating, 134-xOffset, 14-yOffset, 16, 58, 1000, false, fluidOverlayLarge);
		
		if(tempRecipe.condensentrating)
		{
			initGas(gasStacks, 0, true, 26-xOffset, 14-yOffset, 16, 58, new GasStack(tempRecipe.gasType, 1), true);
			fluidStacks.set(0, ingredients.getOutputs(FluidStack.class).get(0));
		}
		else {
			initGas(gasStacks, 0, false, 26-xOffset, 14-yOffset, 16, 58, new GasStack(tempRecipe.gasType, 1), true);
			fluidStacks.set(0, ingredients.getInputs(FluidStack.class).get(0));
		}
	}
}
