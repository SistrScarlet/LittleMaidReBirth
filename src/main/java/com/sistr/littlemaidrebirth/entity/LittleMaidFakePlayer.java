package com.sistr.littlemaidrebirth.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.common.util.FakePlayer;

import javax.annotation.Nullable;
import java.util.List;

public class LittleMaidFakePlayer implements IHasFakePlayer {
    private final LivingEntity origin;
    @Nullable
    private FakePlayer fakePlayer;
    private CompoundNBT syncNBT = new CompoundNBT();

    public LittleMaidFakePlayer(LivingEntity origin) {
        this.origin = origin;
    }

    @Override
    public FakePlayer getFakePlayer() {
        if (this.fakePlayer == null) {
            this.fakePlayer = new PlayerWrapperEntity(this.origin);
            syncToFakePlayer();
        }
        return this.fakePlayer;
    }

    @Override
    public void syncToFakePlayer() {
        CompoundNBT nbt = new CompoundNBT();
        this.origin.writeWithoutTypeId(nbt);
        this.syncNBT = nbt;
        getFakePlayer().read(nbt);
    }

    @Override
    public void syncToOrigin() {
        CompoundNBT nbt = this.syncNBT;
        getFakePlayer().writeWithoutTypeId(nbt);
        this.origin.read(nbt);
    }

    public void livingTick() {
        if (!this.origin.world.isRemote && this.origin.getHealth() > 0.0F && !this.origin.isSpectator()) {
            AxisAlignedBB axisalignedbb;
            if (this.origin.isPassenger() && this.origin.getRidingEntity().isAlive()) {
                axisalignedbb = this.origin.getBoundingBox().union(this.origin.getRidingEntity().getBoundingBox()).grow(1.0D, 0.0D, 1.0D);
            } else {
                axisalignedbb = this.origin.getBoundingBox().grow(1.0D, 0.5D, 1.0D);
            }

            List<Entity> list = this.origin.world.getEntitiesWithinAABBExcludingEntity(this.origin, axisalignedbb);

            boolean alreadySync = false;
            for (Entity entity : list) {
                if (entity.isAlive()) {
                    if (!alreadySync) {
                        alreadySync = true;
                        syncToFakePlayer();
                    }
                    entity.onCollideWithPlayer(getFakePlayer());
                }
            }
            if (alreadySync) {
                syncToOrigin();
            }
        }
    }

    public void onDeath(DamageSource cause) {
        if (this.origin.world.isRemote) {
            return;
        }
        syncToFakePlayer();
        getFakePlayer().onDeath(cause);
    }
}
