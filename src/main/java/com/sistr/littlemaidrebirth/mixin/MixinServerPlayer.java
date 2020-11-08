package com.sistr.littlemaidrebirth.mixin;

import com.mojang.authlib.GameProfile;
import com.sistr.littlemaidrebirth.entity.FakePlayerWrapperEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class MixinServerPlayer extends PlayerEntity {

    public MixinServerPlayer(World worldIn, GameProfile gameProfileIn) {
        super(worldIn, gameProfileIn);
    }

    @Inject(at = @At("HEAD"), method = "func_205734_a", cancellable = true)
    public void onMoveSpawnPoint(ServerWorld worldIn, CallbackInfo ci) {
        if (getEntity() instanceof FakePlayerWrapperEntity) {
            ci.cancel();
        }
    }

}
