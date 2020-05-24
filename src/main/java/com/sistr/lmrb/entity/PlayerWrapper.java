package com.sistr.lmrb.entity;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.FakePlayer;

import java.util.UUID;

public class PlayerWrapper extends FakePlayer {

    public PlayerWrapper(LivingEntity origin) {
        super((ServerWorld) origin.world, new GameProfile(UUID.randomUUID(), origin.getType().getRegistryName().getPath() + "test"));
        CompoundNBT nbt = new CompoundNBT();
        origin.writeWithoutTypeId(nbt);
        this.read(nbt);
    }

}
