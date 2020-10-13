package com.sistr.littlemaidrebirth.entity;

import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.EntityDataManager;
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

    public void write(CompoundNBT nbt) {
        //tameableと統合したのでownerIdの読み込みは削除した

        nbt.putString("MovingState", getMovingState());
    }

    public void read(CompoundNBT nbt) {
        //tameableと統合したが、これを残しておかないと契約状態がリセットされる危険性がある
        if (nbt.hasUniqueId("OwnerId")) {
            this.setOwnerId(nbt.getUniqueId("OwnerId"));
        }

        setMovingState(nbt.getString("MovingState"));
    }

    @Override
    public LivingEntity getOwner() {
        UUID ownerId = this.getOwnerId();
        if (ownerId == null) return null;
        PlayerEntity player = this.tameable.world.getPlayerByUuid(ownerId);
        if (player != null) {
            return player;
        }
        if (this.tameable.world instanceof ServerWorld) {
            return (LivingEntity) ((ServerWorld) this.tameable.world).getEntityByUuid(ownerId);
        }
        return null;
    }

    @Override
    public void setOwnerId(UUID id) {
        this.dataManager.set(ownerId, Optional.of(id));
    }

    @Override
    public UUID getOwnerId() {
        return this.dataManager.get(ownerId).orElse(null);
    }

    @Override
    public String getMovingState() {
        return this.dataManager.get(moving_state);
    }

    public void setMovingState(String movingState) {
        this.dataManager.set(moving_state, movingState);
    }

}
