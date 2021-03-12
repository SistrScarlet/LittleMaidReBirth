package net.sistr.littlemaidrebirth.entity.mode;

import net.sistr.littlemaidrebirth.api.mode.Mode;
import net.sistr.littlemaidrebirth.api.mode.ModeManager;
import net.sistr.littlemaidrebirth.entity.InventorySupplier;
import net.sistr.littlemaidrebirth.entity.Tameable;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.sistr.lmml.entity.compound.SoundPlayable;
import net.sistr.lmml.resource.util.LMSounds;

import java.util.OptionalInt;

public class HealerMode<T extends CreatureEntity & Tameable & InventorySupplier> implements Mode {
    protected final T mob;
    protected final int inventoryStart;
    protected final int inventoryEnd;

    public HealerMode(T mob, int inventoryStart, int inventoryEnd) {
        this.mob = mob;
        this.inventoryStart = inventoryStart;
        this.inventoryEnd = inventoryEnd;
    }

    @Override
    public void startModeTask() {

    }

    @Override
    public boolean shouldExecute() {
        net.minecraft.entity.LivingEntity owner = mob.getTameOwner().orElse(null);
        if (!(owner instanceof PlayerEntity)) return false;
        if (!((PlayerEntity) owner).getFoodStats().needFood()) return false;
        return getFoodsIndex().isPresent();
    }

    public OptionalInt getFoodsIndex() {
        IInventory inventory = this.mob.getInventory();
        for (int i = inventoryStart; i < inventoryEnd; ++i) {
            ItemStack itemstack = inventory.getStackInSlot(i);
            if (isFoods(itemstack)) {
                return OptionalInt.of(i);
            }
        }
        return OptionalInt.empty();
    }

    public boolean isFoods(ItemStack stack) {
        return stack.getItem().isFood();
    }

    @Override
    public boolean shouldContinueExecuting() {
        return shouldExecute();
    }

    @Override
    public void startExecuting() {

    }

    @Override
    public void tick() {
        getFoodsIndex().ifPresent(index -> {
            IInventory inventory = mob.getInventory();
            ItemStack stack = inventory.getStackInSlot(index);
            mob.getTameOwner()
                    .ifPresent(owner -> {
                        owner.onFoodEaten(owner.world, stack);
                        inventory.decrStackSize(index, 0);
                        if (owner instanceof SoundPlayable)
                            ((SoundPlayable) owner).play(LMSounds.HEALING);
                    });
        });
    }

    @Override
    public void resetTask() {

    }

    @Override
    public void endModeTask() {

    }

    @Override
    public void writeModeData(CompoundNBT tag) {

    }

    @Override
    public void readModeData(CompoundNBT tag) {

    }

    @Override
    public String getName() {
        return "Healer";
    }

    static {
        ModeManager.ModeItems items = new ModeManager.ModeItems();
        items.add(stack -> stack.getItem().isFood());
        ModeManager.INSTANCE.register(HealerMode.class, items);
    }

}
