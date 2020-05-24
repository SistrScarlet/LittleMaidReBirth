package com.sistr.lmrb.entity.mode;

import com.google.common.collect.Sets;
import com.sistr.lmrb.util.ModeManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import javax.annotation.Nullable;
import java.util.Set;

public class DefaultModeController implements IHasMode {
    @Nullable
    private IMode nowMode;
    private final Set<IMode> modes = Sets.newHashSet();
    private final LivingEntity owner;
    private Item prevItem = Items.AIR;

    public DefaultModeController(LivingEntity owner, Set<IMode> modes) {
        this.owner = owner;
        this.modes.addAll(modes);
    }

    @Override
    @Nullable
    public IMode getMode() {
        return this.nowMode;
    }

    public void tick() {
        if (!isModeContinue()) {

            IMode newMode = getNewMode();
            if (nowMode != newMode) {
                if (nowMode != null) {
                    nowMode.endModeTask();
                }
                if (newMode != null) {
                    newMode.startModeTask();
                }
                nowMode = newMode;
            }
        }
        prevItem = owner.getHeldItemMainhand().getItem();
    }

    //アイテムが同じかアイテムからっぽだったら継続
    public boolean isModeContinue() {
        return prevItem == owner.getHeldItemMainhand().getItem() || owner.getHeldItemMainhand().isEmpty();
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
