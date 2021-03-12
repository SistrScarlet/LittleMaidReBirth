package net.sistr.littlemaidrebirth.mixin;

import net.sistr.littlemaidrebirth.util.DefaultedListLimiter;
import net.minecraft.util.NonNullList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NonNullList.class)
public abstract class MixinDefaultedList<E> implements DefaultedListLimiter {

    @Shadow
    @Final
    private java.util.List<E> delegate;
    private int fakeLimit;

    @Inject(at = @At("RETURN"), method = "size", cancellable = true)
    public void onSize(CallbackInfoReturnable<Integer> cir) {
        if (0 < fakeLimit) {
            cir.setReturnValue(Math.min(delegate.size(), fakeLimit));
        }
    }

    @Override
    public void setSizeLimit_LM(int limit) {
        fakeLimit = limit;
    }
}
