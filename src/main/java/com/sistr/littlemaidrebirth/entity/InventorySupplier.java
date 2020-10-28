package com.sistr.littlemaidrebirth.entity;

import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.CompoundNBT;

public interface InventorySupplier {

    IInventory getInventory();

    void writeInventory(CompoundNBT nbt);

    void readInventory(CompoundNBT nbt);

}
