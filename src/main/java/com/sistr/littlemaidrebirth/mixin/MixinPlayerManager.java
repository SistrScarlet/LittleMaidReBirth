package com.sistr.littlemaidrebirth.mixin;

import com.sistr.littlemaidrebirth.entity.FakePlayerWrapperEntity;
import net.minecraft.advancements.PlayerAdvancements;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.world.storage.FolderName;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;

@Mixin(PlayerList.class)
public class MixinPlayerManager {

    @Shadow
    @Final
    private MinecraftServer server;

    @Inject(method = "getPlayerAdvancements", at = @At("HEAD"), cancellable = true)
    public void onGetAdvancementTracker(ServerPlayerEntity player, CallbackInfoReturnable<PlayerAdvancements> cir) {
        if (!(player instanceof FakePlayerWrapperEntity)) return;
        cir.setReturnValue(FakePlayerWrapperEntity.getFPWEAdvancementTracker()
                .orElseGet(() -> {
                    File file = this.server.func_240776_a_(FolderName.ADVANCEMENTS).toFile();
                    File file2 = new File(file, FakePlayerWrapperEntity.getFPWEUuid() + ".json");
                    return FakePlayerWrapperEntity.initFPWEAdvancementTracker(this.server.getDataFixer(),
                            (PlayerList) (Object) this, this.server.getAdvancementManager(), file2, player);
                }));
    }

}
