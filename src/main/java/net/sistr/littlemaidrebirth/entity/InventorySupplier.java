package net.sistr.littlemaidrebirth.entity;

import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.CompoundNBT;

public interface InventorySupplier {

    IInventory getInventory();

    void writeInventory(CompoundNBT tag);

    void readInventory(CompoundNBT tag);

}
