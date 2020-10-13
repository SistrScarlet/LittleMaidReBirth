package com.sistr.littlemaidrebirth.entity;

import net.minecraft.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.UUID;

//ぶっちゃけ必要意義は微妙
public interface ITameable {

    String NONE = "None";
    String WAIT = "Wait";
    String ESCORT = "Escort";
    String FREEDOM = "Freedom";

    @Nullable
    LivingEntity getOwner();

    void setOwnerId(UUID id);

    @Nullable
    UUID getOwnerId();

    String getMovingState();

    void setMovingState(String movingState);

}
