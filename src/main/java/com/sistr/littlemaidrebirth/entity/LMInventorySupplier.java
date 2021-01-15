package com.sistr.littlemaidrebirth.entity;

import com.sistr.littlemaidrebirth.util.DefaultedListLimiter;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraftforge.common.util.FakePlayer;

public class LMInventorySupplier implements InventorySupplier {
    private final IInventory inventory;

    public LMInventorySupplier(LivingEntity owner, FakePlayerSupplier player) {
        if (!owner.world.isRemote) {
            FakePlayer fakePlayer = player.getFakePlayer();
            inventory = new LMInventory(fakePlayer, 19);
            fakePlayer.inventory = (PlayerInventory) inventory;
        } else {
            inventory = new Inventory(18 + 4 + 2);
        }
    }

    @Override
    public IInventory getInventory() {
        return inventory;
    }

    @Override
    public void writeInventory(CompoundNBT tag) {
        tag.put("Inventory", ((LMInventory) this.inventory).write(new ListNBT()));
    }

    @Override
    public void readInventory(CompoundNBT tag) {
        ((LMInventory) this.inventory).read(tag.getList("Inventory", 10));
    }

    public static class LMInventory extends PlayerInventory {

        public LMInventory(PlayerEntity player, int size) {
            super(player);
            ((DefaultedListLimiter) this.mainInventory).setSizeLimit_LM(size);
        }

        @Override
        public boolean addItemStackToInventory(ItemStack stack) {
            boolean isMainEmpty = mainInventory.get(0).isEmpty();
            if (isMainEmpty) {
                mainInventory.set(0, Items.GOLDEN_SWORD.getDefaultInstance());
            }
            boolean temp = super.addItemStackToInventory(stack);
            if (isMainEmpty) {
                mainInventory.set(0, ItemStack.EMPTY);
            }
            return temp;
        }

    }

}
