package com.sistr.lmrb.entity;

import net.minecraft.entity.Entity;

import java.util.Optional;
import java.util.UUID;

public interface ITameable {

    void setOwner(Entity owner);

    Optional<Entity> getOwner();

    Optional<UUID> getOwnerId();

}
