package com.sistr.littlemaidrebirth.entity;

import net.minecraft.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

public interface ITameable {

    String NONE = "None";
    String WAIT = "Wait";
    String ESCORT = "Escort";
    String FREEDOM = "Freedom";

    Optional<LivingEntity> getTameOwner();

    void setTameOwnerUuid(UUID id);

    Optional<UUID> getTameOwnerUuid();

    boolean hasTameOwner();

    String getMovingState();

    void setMovingState(String movingState);

}
