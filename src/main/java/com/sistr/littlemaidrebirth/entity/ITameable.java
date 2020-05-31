package com.sistr.littlemaidrebirth.entity;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.DamageSource;

import java.util.Optional;
import java.util.UUID;

public interface ITameable {

    String NONE = "None";
    String WAIT = "Wait";
    String ESCORT = "Escort";
    String FREEDOM = "Freedom";

    void writeTameable(CompoundNBT nbt);

    void readTameable(CompoundNBT nbt);

    Optional<Entity> getOwner();

    void setOwnerId(UUID id);

    Optional<UUID> getOwnerId();

    String getMovingState();

    void setMovingState(String movingState);

    void onDeath(DamageSource cause);

    boolean isFriend(Entity entity);

}
