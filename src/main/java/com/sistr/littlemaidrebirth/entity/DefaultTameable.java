package com.sistr.littlemaidrebirth.entity;

import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.EntityDataManager;

import java.util.Optional;
import java.util.UUID;

//EntityDataManagerのregisterをこっちでやるとエラー吐いてダメなので個々のエンティティクラスでやってもらう形に
//なお本体のTameableEntity化に合わせgetTameOwner()系の処理は消した
public class DefaultTameable implements ITameable {
    private final DataParameter<String> moving_state;
    private final EntityDataManager dataManager;

    public DefaultTameable(EntityDataManager dataManager, DataParameter<String> moving_state) {
        this.dataManager = dataManager;
        this.moving_state = moving_state;
    }

    public void write(CompoundNBT nbt) {
        nbt.putString("MovingState", getMovingState());
    }

    public void read(CompoundNBT nbt) {
        setMovingState(nbt.getString("MovingState"));
    }

    @Override
    public Optional<LivingEntity> getTameOwner() {
        return Optional.empty();
    }

    @Override
    public void setTameOwnerUuid(UUID id) {
        
    }

    @Override
    public Optional<UUID> getTameOwnerUuid() {
        return Optional.empty();
    }

    @Override
    public boolean hasTameOwner() {
        return false;
    }

    @Override
    public String getMovingState() {
        return this.dataManager.get(moving_state);
    }

    public void setMovingState(String movingState) {
        this.dataManager.set(moving_state, movingState);
    }

}
