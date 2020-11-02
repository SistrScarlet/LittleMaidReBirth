package com.sistr.littlemaidrebirth.mixin;

import com.sistr.littlemaidrebirth.util.AbstractFurnaceAccessor;
import net.minecraft.item.crafting.AbstractCookingRecipe;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.tileentity.AbstractFurnaceTileEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AbstractFurnaceTileEntity.class)
public abstract class MixinAbstractFurnaceTileEntity implements AbstractFurnaceAccessor {

    @Shadow
    @Final
    protected IRecipeType<? extends AbstractCookingRecipe> recipeType;

    @Shadow
    protected abstract boolean isBurning();

    @Override
    public IRecipeType<? extends AbstractCookingRecipe> getRecipeType_LM() {
        return this.recipeType;
    }

    @Override
    public boolean isBurningFire_LM() {
        return isBurning();
    }


}
