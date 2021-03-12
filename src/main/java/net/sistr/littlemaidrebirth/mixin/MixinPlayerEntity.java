package net.sistr.littlemaidrebirth.mixin;

import net.sistr.littlemaidrebirth.util.PlayerAccessor;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PlayerEntity.class)
public abstract class MixinPlayerEntity implements PlayerAccessor {

    @Shadow
    protected abstract void collideWithPlayer(net.minecraft.entity.Entity entityIn);

    @Override
    public void onCollideWithEntity_LM(net.minecraft.entity.Entity entity) {
        collideWithPlayer(entity);
    }
}
