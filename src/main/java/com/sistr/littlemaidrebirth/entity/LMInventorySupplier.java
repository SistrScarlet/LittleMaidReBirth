package com.sistr.littlemaidrebirth.entity;

import com.google.common.collect.Lists;
import com.sistr.littlemaidrebirth.util.DefaultedListLimiter;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraftforge.common.util.FakePlayer;

import java.util.List;

public class LMInventorySupplier implements InventorySupplier {
    private final LivingEntity owner;
    private final FakePlayerSupplier player;
    private final int size = 18;
    private IInventory inventory;
    private ListNBT inventoryTag;

    public LMInventorySupplier(LivingEntity owner, FakePlayerSupplier player) {
        this.owner = owner;
        this.player = player;
    }

    @Override
    public IInventory getInventory() {
        if (inventory == null) {
            if (!owner.world.isRemote) {
                FakePlayer fakePlayer = player.getFakePlayer();
                inventory = new LMInventory(fakePlayer, owner, size);
                fakePlayer.inventory = (PlayerInventory) inventory;
                if (inventoryTag != null) readInventory(inventoryTag);
            } else {
                inventory = new Inventory(size);
            }
        }
        return inventory;
    }

    //デフォのserializeとdeserializeは使うとエラー吐く
    public void writeInventory(CompoundNBT nbt) {
        if (inventory == null) {
            if (inventoryTag != null)
                nbt.put("Inventory", inventoryTag);
            return;
        }

        ListNBT listnbt = new ListNBT();

        for(int index = 0; index < this.size; ++index) {
            if (!inventory.getStackInSlot(index).isEmpty()) {
                CompoundNBT compoundnbt = new CompoundNBT();
                compoundnbt.putByte("Slot", (byte)index);
                inventory.getStackInSlot(index).write(compoundnbt);
                listnbt.add(compoundnbt);
            }
        }

        List<ItemStack> armorInventory = Lists.newArrayList(this.owner.getArmorInventoryList());
        for(int index = 0; index < armorInventory.size(); ++index) {
            if (!armorInventory.get(index).isEmpty()) {
                CompoundNBT armor = new CompoundNBT();
                armor.putByte("Slot", (byte)(index + 100));
                armorInventory.get(index).write(armor);
                listnbt.add(armor);
            }
        }

        List<ItemStack> offHandInventory = Lists.newArrayList(this.owner.getHeldItemOffhand());
        for(int index = 0; index < offHandInventory.size(); ++index) {
            if (!offHandInventory.get(index).isEmpty()) {
                CompoundNBT offhand = new CompoundNBT();
                offhand.putByte("Slot", (byte)(index + 150));
                offHandInventory.get(index).write(offhand);
                listnbt.add(offhand);
            }
        }
        nbt.put("Inventory", listnbt);
    }

    public void readInventory(CompoundNBT nbt) {
        this.inventoryTag = nbt.getList("Inventory", 10);
        if (inventory != null) readInventory(inventoryTag);
    }

    private void readInventory(ListNBT inventoryTag) {
        for(int i = 0; i < inventoryTag.size(); ++i) {
            CompoundNBT tag = inventoryTag.getCompound(i);
            int slot = tag.getByte("Slot") & 255;
            ItemStack itemstack = ItemStack.read(tag);
            if (!itemstack.isEmpty()) {
                if (slot < this.size) {
                    inventory.setInventorySlotContents(slot, itemstack);
                } else if (slot < 100) {
                    this.owner.entityDropItem(itemstack);
                } else if (slot < 4 + 100) {
                    this.owner.setItemStackToSlot(EquipmentSlotType
                            .fromSlotTypeAndIndex(EquipmentSlotType.Group.ARMOR, slot - 100), itemstack);
                } else if (slot >= 150 && slot < 1 + 150) {
                    this.owner.setItemStackToSlot(EquipmentSlotType.OFFHAND, itemstack);
                }
            }
        }
    }

    private static class LMInventory extends PlayerInventory {
        private final LivingEntity owner;

        public LMInventory(PlayerEntity player, LivingEntity owner, int size) {
            super(player);
            this.owner = owner;
            ((DefaultedListLimiter)this.mainInventory).lm_setSizeLimit(size);
        }

        @Override
        public ItemStack getCurrentItem() {
            return owner.getHeldItemMainhand();
        }

    }
}
