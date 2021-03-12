package net.sistr.littlemaidrebirth.util;

import net.minecraft.util.DamageSource;

public interface LivingAccessor {

    void applyEquipmentAttributes_LM();

    void tickActiveItemStack_LM();

    boolean blockedByShield_LM(DamageSource source);
}
