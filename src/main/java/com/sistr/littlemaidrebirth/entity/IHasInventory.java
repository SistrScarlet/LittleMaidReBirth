package com.sistr.littlemaidrebirth.entity;

import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.CompoundNBT;

public interface IHasInventory {

    IInventory getInventory();

    void writeInventories(CompoundNBT nbt);

    void readInventories(CompoundNBT nbt);

}
