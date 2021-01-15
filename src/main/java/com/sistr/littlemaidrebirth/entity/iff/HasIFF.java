package com.sistr.littlemaidrebirth.entity.iff;

import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;

import java.util.List;

public interface HasIFF {

    IFFTag identify(LivingEntity target);

    void setIFFs(List<IFF> iffs);

    List<IFF> getIFFs();

    void writeIFF(CompoundNBT tag);

    void readIFF(CompoundNBT tag);

}
