package com.sistr.littlemaidrebirth.entity.mode;

import com.google.common.collect.Sets;
import com.sistr.littlemaidrebirth.entity.InventorySupplier;
import com.sistr.littlemaidrebirth.util.ModeManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Hand;

import java.util.Optional;
import java.util.Set;

public class ModeController implements ModeSupplier {
    private final LivingEntity owner;
    private final InventorySupplier hasInventory;
    private final Set<Mode> modes = Sets.newHashSet();
    private Mode nowMode;
    private Item prevItem = Items.AIR;
    private CompoundNBT tempModeData;

    public ModeController(LivingEntity owner, InventorySupplier hasInventory, Set<Mode> modes) {
        this.owner = owner;
        this.hasInventory = hasInventory;
        this.modes.addAll(modes);
    }

    public void addMode(Mode mode) {
        modes.add(mode);
    }

    @Override
    public Optional<Mode> getMode() {
        return Optional.ofNullable(this.nowMode);
    }

    @Override
    public void writeModeData(CompoundNBT tag) {
        getMode().ifPresent(mode -> {
            CompoundNBT modeData = new CompoundNBT();
            mode.writeModeData(modeData);
            tag.put("ModeData", modeData);
        });
    }

    @Override
    public void readModeData(CompoundNBT tag) {
        if (tag.contains("ModeData"))
            this.tempModeData = tag.getCompound("ModeData");
    }

    public void tick() {
        if (!isModeContinue()) {
            //手持ちアイテムに現在のモードで適用できるかチェック
            if (hasModeItem()) {
                return;
            }
            changeMode();
        }
        prevItem = owner.getHeldItemMainhand().getItem();
    }

    public boolean hasModeItem() {
        if (nowMode != null && owner.getHeldItemMainhand().isEmpty()) {
            IInventory inventory = hasInventory.getInventory();
            for (int index = 0; index < inventory.getSizeInventory(); index++) {
                ItemStack stack = inventory.getStackInSlot(index);
                if (ModeManager.INSTANCE.containModeItem(nowMode, stack)) {
                    owner.setHeldItem(Hand.MAIN_HAND, stack.copy());
                    inventory.removeStackFromSlot(index);
                    prevItem = owner.getHeldItemMainhand().getItem();
                    return true;
                }
            }
        }
        return false;
    }

    //アイテムが同じ場合は継続
    public boolean isModeContinue() {
        return prevItem == owner.getHeldItemMainhand().getItem();
    }

    public void changeMode() {
        Mode newMode = getNewMode();
        if (nowMode != newMode) {
            if (nowMode != null) {
                nowMode.resetTask();
                nowMode.endModeTask();
            }
            if (newMode != null) {
                if (tempModeData != null) {
                    newMode.readModeData(tempModeData);
                    tempModeData = null;
                }
                newMode.startModeTask();
            }
            nowMode = newMode;
        }
    }

    public Mode getNewMode() {
        for (Mode mode : modes) {
            if (ModeManager.INSTANCE.containModeItem(mode, owner.getHeldItemMainhand())) {
                return mode;
            }
        }
        return null;
    }
}
