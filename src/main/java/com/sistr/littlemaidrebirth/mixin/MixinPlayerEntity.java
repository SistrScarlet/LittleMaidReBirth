package com.sistr.littlemaidrebirth.mixin;

import com.sistr.littlemaidrebirth.util.PlayerAccessor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PlayerEntity.class)
public abstract class MixinPlayerEntity implements PlayerAccessor {

    @Shadow protected abstract void collideWithPlayer(Entity entityIn);

    @Override
    public void onCollideWithEntity_LM(Entity entity) {
        collideWithPlayer(entity);
    }
}
