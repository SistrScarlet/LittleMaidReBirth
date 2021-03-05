package com.sistr.littlemaidrebirth.mixin;

import com.sistr.littlemaidrebirth.api.item.IRangedWeapon;
import net.minecraft.item.ShootableItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ShootableItem.class)
public abstract class MixinRangedWeaponItem implements IRangedWeapon {

    @Shadow public abstract int func_230305_d_();

    @Override
    public float getMaxRange_LMRB() {
        return func_230305_d_();
    }

    @Override
    public int getInterval_LMRB() {
        return 25;
    }
}
