package net.sistr.littlemaidrebirth.mixin;

import net.sistr.littlemaidrebirth.api.mode.IRangedWeapon;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShootableItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ShootableItem.class)
public abstract class MixinRangedWeaponItem implements IRangedWeapon {

    @Shadow public abstract int func_230305_d_();

    @Override
    public float getMaxRange_LMRB(ItemStack stack, LivingEntity user) {
        return func_230305_d_();
    }

    @Override
    public int getInterval_LMRB(ItemStack stack, LivingEntity user) {
        return 25;
    }
}
