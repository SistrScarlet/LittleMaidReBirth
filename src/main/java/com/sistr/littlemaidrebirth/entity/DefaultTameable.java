package com.sistr.littlemaidrebirth.entity;

import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Util;
import net.minecraft.world.GameRules;
import net.minecraft.world.server.ServerWorld;

import java.util.Optional;
import java.util.UUID;

//EntityDataManagerのregisterをこっちでやるとエラー吐いてダメなので個々のエンティティクラスでやってもらう形に
public class DefaultTameable implements ITameable {
    private final DataParameter<String> moving_state;
    private final DataParameter<Optional<UUID>> ownerId;
    private final CreatureEntity tameable;
    private final EntityDataManager dataManager;

    public DefaultTameable(CreatureEntity tameable, EntityDataManager dataManager,
                           DataParameter<String> moving_state, DataParameter<Optional<UUID>> ownerId) {
        this.tameable = tameable;
        this.dataManager = dataManager;
        this.moving_state = moving_state;
        this.ownerId = ownerId;
    }

    @Override
    public void writeTameable(CompoundNBT nbt) {
        if (this.getOwnerId().isPresent()) {
            nbt.putUniqueId("OwnerId", this.getOwnerId().get());
        }

        nbt.putString("MovingState", getMovingState());
    }

    @Override
    public void readTameable(CompoundNBT nbt) {
        if (nbt.hasUniqueId("OwnerId")) {
            this.setOwnerId(nbt.getUniqueId("OwnerId"));
        }

        setMovingState(nbt.getString("MovingState"));
    }

    @Override
    public Optional<Entity> getOwner() {
        Optional<UUID> optional = this.getOwnerId();
        if (!optional.isPresent()) {
            return Optional.empty();
        }
        UUID ownerId = optional.get();
        PlayerEntity player = this.tameable.world.getPlayerByUuid(ownerId);
        if (player != null) {
            return Optional.of(player);
        }
        if (this.tameable.world instanceof ServerWorld) {
            return Optional.ofNullable(((ServerWorld) this.tameable.world).getEntityByUuid(ownerId));
        }
        return Optional.empty();
    }

    @Override
    public void setOwnerId(UUID id) {
        this.dataManager.set(ownerId, Optional.of(id));
    }

    @Override
    public Optional<UUID> getOwnerId() {
        return this.dataManager.get(ownerId);
    }

    @Override
    public String getMovingState() {
        return this.dataManager.get(moving_state);
    }

    public void setMovingState(String movingState) {
        this.dataManager.set(moving_state, movingState);
    }

    @Override
    public void onDeath(DamageSource cause) {
        if (!this.tameable.world.isRemote && this.tameable.world.getGameRules().getBoolean(GameRules.SHOW_DEATH_MESSAGES)
                && this.getOwner().isPresent() && this.getOwner().get() instanceof ServerPlayerEntity) {
            this.getOwner().get().sendMessage(this.tameable.getCombatTracker().getDeathMessage(), Util.field_240973_b_);
        }
    }

    @Override
    public boolean isFriend(Entity entity) {
        if (this.getOwnerId().isPresent()) {
            UUID ownerId = this.getOwnerId().get();
            //主はフレンド
            if(ownerId.equals(entity.getUniqueID())) {
                return true;
            }
            //同じ主を持つ者はフレンド
            if (entity instanceof ITameable && ((ITameable) entity).getOwnerId().isPresent()
                    && ((ITameable) entity).getOwnerId().get().equals(ownerId)) {
                return true;
            }
        }
        return false;
    }

}
