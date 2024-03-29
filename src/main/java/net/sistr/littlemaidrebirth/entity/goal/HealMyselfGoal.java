package net.sistr.littlemaidrebirth.entity.goal;

import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundEvents;
import net.sistr.littlemaidrebirth.entity.InventorySupplier;
import net.sistr.lmml.entity.compound.SoundPlayable;
import net.sistr.lmml.resource.util.LMSounds;

import java.util.EnumSet;
import java.util.Set;

public class HealMyselfGoal<T extends CreatureEntity & InventorySupplier> extends Goal {
    private final T mob;
    private final Set<Item> healItems;
    private final int healInterval;
    private final int healAmount;
    private int cool;
    private int cache;

    public HealMyselfGoal(T mob, Set<Item> healItems,
                          int healInterval, int healAmount) {
        this.mob = mob;
        this.healItems = healItems;
        this.healInterval = healInterval;
        this.healAmount = healAmount;
        setMutexFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean shouldExecute() {
        assert mob.getMaxHealth() != 0;
        return (mob.hurtTime <= 0 && mob.getHealth() <= mob.getMaxHealth() - 1
                || mob.getHealth() / mob.getMaxHealth() < 0.75F)
                && hasHealItem();
    }

    @Override
    public boolean shouldContinueExecuting() {
        return mob.getHealth() < mob.getMaxHealth() && hasHealItem();
    }

    @Override
    public void startExecuting() {
        super.startExecuting();
        this.mob.getNavigator().clearPath();
        cool = healInterval;
    }

    @Override
    public void tick() {
        if (0 < cool--) {
            return;
        }
        cool = healInterval;

        int slot = getHealItemSlot();
        ItemStack healItem = getHealItem(slot);
        if (healItem.isEmpty()) return;
        consumeHealItem(slot, healItem);

        mob.heal(healAmount);
        mob.playSound(SoundEvents.ENTITY_ITEM_PICKUP, 1.0F, mob.getRNG().nextFloat() * 0.1F + 1.0F);
        mob.swingArm(Hand.MAIN_HAND);
        if (this.mob instanceof SoundPlayable) {
            if (mob.getHealth() < mob.getMaxHealth()) {
                ((SoundPlayable) mob).play(LMSounds.EAT_SUGAR);
            } else {
                ((SoundPlayable) mob).play(LMSounds.EAT_SUGAR_MAX_POWER);
            }
        }
    }

    public boolean hasHealItem() {
        return getHealItemSlot() != -1;
    }

    public int getHealItemSlot() {
        IInventory inventory = this.mob.getInventory();
        if (cache != -1) {
            ItemStack slotStack = inventory.getStackInSlot(cache);
            if (healItems.contains(slotStack.getItem())) {
                return cache;
            } else {
                cache = -1;
            }
        }
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack slotStack = inventory.getStackInSlot(i);
            if (healItems.contains(slotStack.getItem())) {
                cache = i;
                return i;
            }
        }
        return -1;
    }

    public ItemStack getHealItem(int slot) {
        if (slot == -1) return ItemStack.EMPTY;
        return this.mob.getInventory().getStackInSlot(slot);
    }

    public void consumeHealItem(int slot, ItemStack healItem) {
        healItem.shrink(1);
        if (healItem.isEmpty()) {
            this.mob.getInventory().removeStackFromSlot(slot);
        }
    }

}
