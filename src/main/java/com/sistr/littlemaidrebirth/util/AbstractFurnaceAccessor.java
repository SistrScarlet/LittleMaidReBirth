package com.sistr.littlemaidrebirth.util;

import net.minecraft.item.crafting.AbstractCookingRecipe;
import net.minecraft.item.crafting.IRecipeType;

public interface AbstractFurnaceAccessor {

    IRecipeType<? extends AbstractCookingRecipe> getRecipeType_LM();

    boolean isBurningFire_LM();

}
