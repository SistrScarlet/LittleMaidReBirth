package net.sistr.littlemaidrebirth.mixin;

import net.sistr.littlemaidrebirth.util.LivingAccessor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity extends Entity implements LivingAccessor {

    public MixinLivingEntity(EntityType<?> entityTypeIn, net.minecraft.world.World worldIn) {
        super(entityTypeIn, worldIn);
    }

    @Shadow
    protected abstract void updateActiveHand();

    @Shadow
    protected abstract boolean canBlockDamageSource(net.minecraft.util.DamageSource damageSourceIn);

    @Shadow protected abstract void func_241353_q_();

    @Override
    public void applyEquipmentAttributes_LM() {
        func_241353_q_();
    }

    @Override
    public void tickActiveItemStack_LM() {
        updateActiveHand();
    }

    @Override
    public boolean blockedByShield_LM(net.minecraft.util.DamageSource source) {
        return canBlockDamageSource(source);
    }
}
