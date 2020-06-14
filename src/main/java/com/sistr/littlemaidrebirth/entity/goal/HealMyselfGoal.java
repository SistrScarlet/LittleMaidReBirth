package com.sistr.littlemaidrebirth.entity.goal;

import com.sistr.littlemaidrebirth.entity.IHasInventory;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundEvents;

import java.util.EnumSet;

public class HealMyselfGoal extends Goal {
    private final CreatureEntity owner;
    private final IHasInventory hasInventory;
    private int cool;

    public HealMyselfGoal(CreatureEntity owner, IHasInventory hasInventory) {
        this.owner = owner;
        this.hasInventory = hasInventory;
        setMutexFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean shouldExecute() {
        return owner.getHealth() < owner.getMaxHealth() - 1 && hasHealItem();
    }

    public boolean hasHealItem() {
        IInventory inventory = hasInventory.getInventory();
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack slotStack = inventory.getStackInSlot(i);
            if (slotStack.getItem() == Items.SUGAR) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void startExecuting() {
        this.owner.getNavigator().clearPath();
        cool = 0;
    }

    @Override
    public void tick() {
        if (cool++ < 2) {
            return;
        }
        cool = 0;
        int healItemSlot = getHealItemSlot();
        if (healItemSlot < 0) {
            return;
        }
        IInventory inventory = hasInventory.getInventory();
        ItemStack healItem = inventory.getStackInSlot(healItemSlot);
        healItem.shrink(1);
        if (healItem.isEmpty()) {
            inventory.removeStackFromSlot(healItemSlot);
        }
        owner.heal(1);
        owner.playSound(SoundEvents.ENTITY_ITEM_PICKUP, 1.0F, owner.getRNG().nextFloat() * 0.1F + 1.0F);
        owner.swingArm(Hand.MAIN_HAND);
    }

    public int getHealItemSlot() {
        IInventory inventory = hasInventory.getInventory();
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack slotStack = inventory.getStackInSlot(i);
            if (slotStack.getItem() == Items.SUGAR) {
                return i;
            }
        }
        return -1;
    }

}
