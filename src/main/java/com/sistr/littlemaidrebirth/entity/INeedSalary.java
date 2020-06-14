package com.sistr.littlemaidrebirth.entity;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;

public interface INeedSalary {

    boolean receiveSalary(int num);

    boolean consumeSalary(int num);

    int getSalary();

    boolean isSalary(ItemStack stack);

    boolean isStrike();

    void writeSalary(CompoundNBT nbt);

    void readSalary(CompoundNBT nbt);

}
