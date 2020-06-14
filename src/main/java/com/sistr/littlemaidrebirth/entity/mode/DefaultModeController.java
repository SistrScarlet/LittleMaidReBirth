package com.sistr.littlemaidrebirth.entity.mode;

import com.google.common.collect.Sets;
import com.sistr.littlemaidrebirth.entity.IHasInventory;
import com.sistr.littlemaidrebirth.util.ModeManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

import javax.annotation.Nullable;
import java.util.Set;

public class DefaultModeController implements IHasMode {
    private final LivingEntity owner;
    private final IHasInventory hasInventory;
    private final Set<IMode> modes = Sets.newHashSet();
    @Nullable
    private IMode nowMode;
    private Item prevItem = Items.AIR;

    public DefaultModeController(LivingEntity owner, IHasInventory hasInventory, Set<IMode> modes) {
        this.owner = owner;
        this.hasInventory = hasInventory;
        this.modes.addAll(modes);
    }

    @Override
    @Nullable
    public IMode getMode() {
        return this.nowMode;
    }

    public void tick() {
        if (!isModeContinue()) {
            //手持ちアイテムに現在のモードで適用できるかチェック
            if (nowMode != null && owner.getHeldItemMainhand().isEmpty()) {
                IInventory inventory = hasInventory.getInventory();
                for (int index = 0; index < inventory.getSizeInventory(); index++) {
                    ItemStack stack = inventory.getStackInSlot(index);
                    if (ModeManager.INSTANCE.containModeItem(nowMode, stack)) {
                        owner.setHeldItem(Hand.MAIN_HAND, stack.copy());
                        inventory.removeStackFromSlot(index);
                        prevItem = owner.getHeldItemMainhand().getItem();
                        return;
                    }
                }
            }

            changeMode();
        }
        prevItem = owner.getHeldItemMainhand().getItem();
    }

    //アイテムが同じ場合は継続
    public boolean isModeContinue() {
        return prevItem == owner.getHeldItemMainhand().getItem();
    }

    public void changeMode() {
        IMode newMode = getNewMode();
        if (nowMode != newMode) {
            if (nowMode != null) {
                nowMode.resetTask();
                nowMode.endModeTask();
            }
            if (newMode != null) {
                newMode.startModeTask();
            }
            nowMode = newMode;
        }
    }

    @Nullable
    public IMode getNewMode() {
        for (IMode mode : modes) {
            if (ModeManager.INSTANCE.containModeItem(mode, owner.getHeldItemMainhand())) {
                return mode;
            }
        }
        return null;
    }
}
