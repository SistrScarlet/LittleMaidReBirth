package net.sistr.littlemaidrebirth.entity;

import net.minecraft.advancements.PlayerAdvancements;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.common.util.FakePlayer;

import java.util.Optional;

public class LMFakePlayerSupplier implements FakePlayerSupplier {
    private final LittleMaidEntity origin;
    private FakePlayer fakePlayer;

    public LMFakePlayerSupplier(LittleMaidEntity origin) {
        this.origin = origin;
    }

    @Override
    public FakePlayer getFakePlayer() {
        if (this.fakePlayer == null) {
            this.fakePlayer = new FakePlayerWrapperEntity(this.origin) {
                @Override
                public LivingEntity getOrigin() {
                    return origin;
                }

                @Override
                public Optional<PlayerAdvancements> getOriginAdvancementTracker() {
                    return origin.getTameOwner()
                            .map(owner -> ((ServerPlayerEntity)owner))
                            .map(ServerPlayerEntity::getAdvancements);
                }
            };
        }
        return this.fakePlayer;
    }

    void tick() {
        if (fakePlayer != null)
            fakePlayer.tick();
    }
}
