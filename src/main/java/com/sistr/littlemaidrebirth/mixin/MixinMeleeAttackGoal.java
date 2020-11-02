package com.sistr.littlemaidrebirth.mixin;

import com.sistr.littlemaidrebirth.util.MeleeAttackAccessor;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(MeleeAttackGoal.class)
public class MixinMeleeAttackGoal implements MeleeAttackAccessor {

    @Shadow private int field_234037_i_;

    @Override
    public void setCool_LM(int time) {
        this.field_234037_i_ = time;
    }
}
