package com.sistr.littlemaidrebirth.entity;

import com.google.common.collect.Lists;
import com.sistr.littlemaidrebirth.entity.IHasInventory;
import net.minecraft.entity.LivingEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;

import java.util.List;

public class LittleMaidInventory implements IHasInventory {
    private final LivingEntity owner;
    private final Inventory inventory = new Inventory(18);

    public LittleMaidInventory(LivingEntity owner) {
        this.owner = owner;
    }

    @Override
    public IInventory getInventory() {
        return inventory;
    }

    @Override
    public void writeInventory(CompoundNBT nbt) {
        ListNBT listnbt = new ListNBT();

        for(int index = 0; index < this.inventory.getSizeInventory(); ++index) {
            if (!this.inventory.getStackInSlot(index).isEmpty()) {
                CompoundNBT compoundnbt = new CompoundNBT();
                compoundnbt.putByte("Slot", (byte)index);
                this.inventory.getStackInSlot(index).write(compoundnbt);
                listnbt.add(compoundnbt);
            }
        }

        List<ItemStack> armorInventory = Lists.newArrayList(this.owner.getArmorInventoryList());
        for(int index = 0; index < armorInventory.size(); ++index) {
            if (!armorInventory.get(index).isEmpty()) {
                CompoundNBT compoundnbt1 = new CompoundNBT();
                compoundnbt1.putByte("Slot", (byte)(index + 100));
                armorInventory.get(index).write(compoundnbt1);
                listnbt.add(compoundnbt1);
            }
        }

        List<ItemStack> offHandInventory = Lists.newArrayList(this.owner.getHeldItemOffhand());
        for(int index = 0; index < offHandInventory.size(); ++index) {
            if (!offHandInventory.get(index).isEmpty()) {
                CompoundNBT compoundnbt2 = new CompoundNBT();
                compoundnbt2.putByte("Slot", (byte)(index + 150));
                offHandInventory.get(index).write(compoundnbt2);
                listnbt.add(compoundnbt2);
            }
        }
        nbt.put("Inventory", listnbt);
    }

    @Override
    public void readInventory(CompoundNBT nbt) {
        ListNBT listNBT = nbt.getList("Inventory", 10);
        for(int i = 0; i < listNBT.size(); ++i) {
            CompoundNBT compoundnbt = listNBT.getCompound(i);
            int slot = compoundnbt.getByte("Slot") & 255;
            ItemStack itemstack = ItemStack.read(compoundnbt);
            if (!itemstack.isEmpty()) {
                if (slot < this.inventory.getSizeInventory()) {
                    this.inventory.setInventorySlotContents(slot, itemstack);
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
}
