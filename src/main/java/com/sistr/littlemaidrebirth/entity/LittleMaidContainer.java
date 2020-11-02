package com.sistr.littlemaidrebirth.entity;

import com.sistr.littlemaidrebirth.setup.Registration;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;

import javax.annotation.Nullable;

public class LittleMaidContainer extends Container implements HasGuiEntitySupplier<LittleMaidEntity> {
    private final InvWrapper playerInventory;
    private final InvWrapper maidInventory;
    private final IInventory handsInventory = new Inventory(2) {
        @Override
        public void setInventorySlotContents(int index, ItemStack stack) {
            super.setInventorySlotContents(index, stack);
            if (maid == null) return;
            maid.setItemStackToSlot(EquipmentSlotType.fromSlotTypeAndIndex(EquipmentSlotType.Group.HAND, index), stack);
        }
    };
    private final IInventory armorsInventory = new Inventory(4) {
        @Override
        public void setInventorySlotContents(int index, ItemStack stack) {
            super.setInventorySlotContents(index, stack);
            if (maid == null) return;
            maid.setItemStackToSlot(EquipmentSlotType.fromSlotTypeAndIndex(EquipmentSlotType.Group.ARMOR, index), stack);
        }
    };
    @Nullable
    private final LittleMaidEntity maid;

    public LittleMaidContainer(int windowId, PlayerInventory inv, LittleMaidEntity maid) {
        this(windowId, inv, maid.getEntityId());
    }

    public LittleMaidContainer(int windowId, PlayerInventory inv, PacketBuffer data) {
        this(windowId, inv, data.readVarInt());
    }

    public LittleMaidContainer(int windowId, PlayerInventory inv, int entityId) {
        super(Registration.LITTLE_MAID_CONTAINER.get(), windowId);
        this.playerInventory = new InvWrapper(inv);
        LittleMaidEntity maid = (LittleMaidEntity) inv.player.world.getEntityByID(entityId);
        this.maid = maid;
        if (maid == null) {
            maidInventory = new InvWrapper(new Inventory(18));
        } else {
            maidInventory = new InvWrapper(maid.getInventory());
            for (EquipmentSlotType type : EquipmentSlotType.values()) {
                if (type.getSlotType() == EquipmentSlotType.Group.HAND) {
                    this.handsInventory.setInventorySlotContents(type.getIndex(), maid.getItemStackFromSlot(type));
                }
                if (type.getSlotType() == EquipmentSlotType.Group.ARMOR) {
                    this.armorsInventory.setInventorySlotContents(type.getIndex(), maid.getItemStackFromSlot(type));
                }
            }
        }
        layoutMaidInventorySlots();
        layoutPlayerInventorySlots(8, 126);
    }

    public LittleMaidEntity getGuiEntity() {
        return maid;
    }

    //18 + 2 + 4 = 24、24 + 4 * 9 = 60
    //0~17メイドインベントリ、18~19メインサブ、20~23防具、24~59プレイヤーインベントリ
    @Override
    public ItemStack transferStackInSlot(PlayerEntity playerIn, int index) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) {
            return newStack;
        }
        ItemStack originalStack = slot.getStack();
        newStack = originalStack.copy();
        if (index < 18) {//メイド->プレイヤー
            if (!this.mergeItemStack(originalStack, 24, 60, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < 24) {//ハンド、防具->メイド
            if (!this.mergeItemStack(originalStack, 0, 18, true)) {
                return ItemStack.EMPTY;
            }
        } else {//プレイヤー->メイド
            if (!this.mergeItemStack(originalStack, 0, 18, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (originalStack.isEmpty()) {
            slot.putStack(ItemStack.EMPTY);
        } else {
            slot.onSlotChanged();
        }

        return newStack;
    }

    @Override
    public boolean canInteractWith(PlayerEntity playerIn) {
        return this.maid != null && this.maid.isAlive() && this.maid.getDistance(playerIn) < 8.0F;
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
        //メイドインベントリ
        addSlotBox(maidInventory, 0, 8, 76, 9, 18, 2, 18);

        //main/off
        addSlot(new Slot(handsInventory, 0, 116, 44));
        addSlot(new Slot(handsInventory, 1, 152, 44));

        //head/chest/legs/feet
        addSlot(new Slot(armorsInventory, EquipmentSlotType.HEAD.getIndex(), 8, 8) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return MobEntity.getSlotForItemStack(stack) == EquipmentSlotType.HEAD;
            }
        });
        addSlot(new Slot(armorsInventory, EquipmentSlotType.CHEST.getIndex(), 8, 44) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return MobEntity.getSlotForItemStack(stack) == EquipmentSlotType.CHEST;
            }
        });
        addSlot(new Slot(armorsInventory, EquipmentSlotType.LEGS.getIndex(), 80, 8) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return MobEntity.getSlotForItemStack(stack) == EquipmentSlotType.LEGS;
            }
        });
        addSlot(new Slot(armorsInventory, EquipmentSlotType.FEET.getIndex(), 80, 44) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return MobEntity.getSlotForItemStack(stack) == EquipmentSlotType.FEET;
            }
        });
    }

}
