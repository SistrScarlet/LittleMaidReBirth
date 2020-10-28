package com.sistr.littlemaidrebirth.entity;

import com.google.common.collect.Lists;
import net.minecraft.advancements.PlayerAdvancements;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.common.util.FakePlayer;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
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
