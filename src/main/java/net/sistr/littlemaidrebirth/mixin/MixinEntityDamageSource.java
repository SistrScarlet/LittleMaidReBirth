package net.sistr.littlemaidrebirth.mixin;

import net.sistr.littlemaidrebirth.entity.FakePlayerWrapperEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.EntityDamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityDamageSource.class)
public class MixinEntityDamageSource {

    @Shadow
    protected Entity damageSourceEntity;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void onInit(String name, Entity source, CallbackInfo ci) {
        if (source instanceof FakePlayerWrapperEntity) {
            this.damageSourceEntity = ((FakePlayerWrapperEntity) source).getOrigin();
        }
    }

}
