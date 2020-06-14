package com.sistr.littlemaidrebirth.entity;

import com.google.common.collect.Sets;
import net.minecraft.entity.LivingEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundEvents;

import java.util.Collection;
import java.util.Set;

public class TickTimeBaseNeedSalary implements INeedSalary {
    private final LivingEntity owner;
    private final IHasInventory hasInventory;
    private final int maxSalary;
    private final Set<Item> salaries = Sets.newHashSet();
    private int salary;
    private int nextSalaryTicks;
    private boolean isStrike;
    private int checkInventoryCool;

    public TickTimeBaseNeedSalary(LivingEntity owner, IHasInventory hasInventory, int maxSalary, Collection<Item> salaries) {
        this.owner = owner;
        this.salaries.addAll(salaries);
        this.maxSalary = maxSalary;
        this.hasInventory = hasInventory;
    }

    public void tick() {
        if (owner.world.isRemote || isStrike || 0 < --nextSalaryTicks) {
            return;
        }
        if (nextSalaryTicks == 0) {
            if (!consumeSalary(1)) {
                this.isStrike = true;
            }
        }
        if (0 < --checkInventoryCool) {
            return;
        }
        checkInventoryCool = 200;
        IInventory inventory = hasInventory.getInventory();
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (isSalary(stack)) {
                while (receiveSalary(1) && !stack.isEmpty()) {
                    stack.shrink(1);
                }
                this.nextSalaryTicks = 24000;
            }
        }
    }

    @Override
    public boolean receiveSalary(int num) {
        if (maxSalary < salary + num) {
            return false;
        }
        salary += num;
        return true;
    }

    @Override
    public boolean consumeSalary(int num) {
        if (salary < num) {
            return false;
        }
        owner.playSound(SoundEvents.ENTITY_ITEM_PICKUP, 1.0F, owner.getRNG().nextFloat() * 0.1F + 1.0F);
        owner.swingArm(Hand.MAIN_HAND);
        salary -= num;
        return true;
    }

    @Override
    public int getSalary() {
        return salary;
    }

    @Override
    public boolean isSalary(ItemStack stack) {
        return salaries.contains(stack.getItem());
    }

    @Override
    public boolean isStrike() {
        return isStrike;
    }

    @Override
    public void writeSalary(CompoundNBT nbt) {
        nbt.putInt("salary", salary);
        nbt.putInt("nextSalaryTicks", nextSalaryTicks);
        nbt.putBoolean("isStrike", isStrike);
    }

    @Override
    public void readSalary(CompoundNBT nbt) {
        salary = nbt.getInt("salary");
        nextSalaryTicks = nbt.getInt("nextSalaryTicks");
        isStrike = nbt.getBoolean("isStrike");
    }
}
