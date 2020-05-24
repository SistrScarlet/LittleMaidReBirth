package com.sistr.lmrb.entity;

import com.sistr.lmrb.setup.Registration;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;

import javax.annotation.Nullable;

public class LittleMaidContainer extends Container {
    private final InvWrapper playerInventory;
    private final InvWrapper maidInventory;
    private final IInventory handsInventory = new Inventory(2) {
        @Override
        public void setInventorySlotContents(int index, ItemStack stack) {
            super.setInventorySlotContents(index, stack);
            if (owner == null) return;
            owner.setItemStackToSlot(EquipmentSlotType.fromSlotTypeAndIndex(EquipmentSlotType.Group.HAND, index), stack);
        }
    };
    private final IInventory armorsInventory = new Inventory(4) {
        @Override
        public void setInventorySlotContents(int index, ItemStack stack) {
            super.setInventorySlotContents(index, stack);
            if (owner == null) return;
            owner.setItemStackToSlot(EquipmentSlotType.fromSlotTypeAndIndex(EquipmentSlotType.Group.ARMOR, index), stack);
        }
    };
    @Nullable
    private final LittleMaidEntity owner;

    public LittleMaidContainer(int id, PlayerInventory inventory) {
        this(id, inventory, null);
    }

    public LittleMaidContainer(int id, PlayerInventory playerInventory, @Nullable LittleMaidEntity owner) {
        super(Registration.LITTLE_MAID_CONTAINER.get(), id);
        this.playerInventory = new InvWrapper(playerInventory);
        this.owner = owner;
        if (owner == null) {
            maidInventory = new InvWrapper(new Inventory(18));
        } else {
            maidInventory = new InvWrapper(owner.getInventory());
            for (EquipmentSlotType type : EquipmentSlotType.values()) {
                if(type.getSlotType() == EquipmentSlotType.Group.HAND) {
                    this.handsInventory.setInventorySlotContents(type.getIndex(), owner.getItemStackFromSlot(type));
                }
                if(type.getSlotType() == EquipmentSlotType.Group.ARMOR) {
                    this.armorsInventory.setInventorySlotContents(type.getIndex(), owner.getItemStackFromSlot(type));
                }
            }
        }
        layoutMaidInventorySlots();
        layoutPlayerInventorySlots(8, 126);
    }

    @Override
    public boolean canInteractWith(PlayerEntity playerIn) {
        return this.owner != null && this.owner.isAlive() && this.owner.getDistance(playerIn) < 8.0F;
    }

    private int addSlotRange(IItemHandler handler, int index, int x, int y, int amount, int dx) {
        for (int i = 0; i < amount; i++) {
            addSlot(new SlotItemHandler(handler, index, x, y));
            x += dx;
            index++;
        }
        return index;
    }

    private int addSlotBox(IItemHandler handler, int index, int x, int y, int horAmount, int dx, int verAmount, int dy) {
        for (int j = 0; j < verAmount; j++) {
            index = addSlotRange(handler, index, x, y, horAmount, dx);
            y += dy;
        }
        return index;
    }

    private void layoutPlayerInventorySlots(int leftCol, int topRow) {
        // Player inventory
        addSlotBox(playerInventory, 9, leftCol, topRow, 9, 18, 3, 18);

        // Hotbar
        topRow += 58;
        addSlotRange(playerInventory, 0, leftCol, topRow, 9, 18);
    }

    private void layoutMaidInventorySlots() {
        //メイドサーン
        addSlotBox(maidInventory, 0, 8, 76, 9, 18, 2, 18);

        //main/off
        addSlot(new Slot(handsInventory, 0, 116, 44));
        addSlot(new Slot(handsInventory, 1, 152, 44));

        //head/chest/legs/feet
        addSlot(new Slot(armorsInventory, EquipmentSlotType.HEAD.getIndex(), 8, 8) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return stack.canEquip(EquipmentSlotType.HEAD, owner);
            }
        });
        addSlot(new Slot(armorsInventory, EquipmentSlotType.CHEST.getIndex(), 8, 44) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return stack.canEquip(EquipmentSlotType.CHEST, owner);
            }
        });
        addSlot(new Slot(armorsInventory, EquipmentSlotType.LEGS.getIndex(), 80, 8) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return stack.canEquip(EquipmentSlotType.LEGS, owner);
            }
        });
        addSlot(new Slot(armorsInventory, EquipmentSlotType.FEET.getIndex(), 80, 44) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return stack.canEquip(EquipmentSlotType.FEET, owner);
            }
        });
    }

}
