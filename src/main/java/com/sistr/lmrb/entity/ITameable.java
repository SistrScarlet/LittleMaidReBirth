package com.sistr.lmrb.entity;

import net.minecraft.entity.Entity;

import java.util.Optional;
import java.util.UUID;

public interface ITameable {

    String NONE = "None";
    String WAIT = "Wait";
    String ESCORT = "Escort";
    String FREEDOM = "Freedom";

    Optional<Entity> getOwner();

    void setOwnerId(UUID id);

    Optional<UUID> getOwnerId();

    String getMovingState();

}
