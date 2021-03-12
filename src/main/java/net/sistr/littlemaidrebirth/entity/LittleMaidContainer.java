package net.sistr.littlemaidrebirth.entity;

import net.sistr.littlemaidrebirth.setup.Registration;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.EquipmentSlotType;
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
    private final IItemHandler playerInventory;
    private final IItemHandler maidInventory;
    @Nullable
    private final LittleMaidEntity maid;

    public LittleMaidContainer(int windowId, PlayerInventory inv, PacketBuffer data) {
        this(windowId, inv, data.readVarInt());
    }

    public LittleMaidContainer(int windowId, PlayerInventory inv, int entityId) {
        super(Registration.LITTLE_MAID_CONTAINER.get(), windowId);
        this.playerInventory = new InvWrapper(inv);

        LittleMaidEntity maid = (LittleMaidEntity) inv.player.world.getEntityByID(entityId);
        this.maid = maid;
        if (maid == null)
            maidInventory = new InvWrapper(new Inventory(18 + 4 + 2));
        else
            maidInventory = new InvWrapper(maid.getInventory());

        layoutMaidInventorySlots();
        layoutPlayerInventorySlots(8, 126);
    }

    public LittleMaidEntity getGuiEntity() {
        return maid;
    }

    @Override
    public boolean canInteractWith(PlayerEntity playerIn) {
        return this.maid != null && this.maid.isAlive() && this.maid.getDistanceSq(playerIn) < 8.0F * 8.0F;
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
        //index 0~17
        addSlotBox(maidInventory, 1, 8, 76, 9, 18, 2, 18);

        //18~19
        addSlot(new SlotItemHandler(maidInventory, 0, 116, 44));
        addSlot(new SlotItemHandler(maidInventory, 1 + 18 + 4, 152, 44));

        //20~23
        addSlot(new SlotItemHandler(maidInventory, 1 + 18 + EquipmentSlotType.HEAD.getIndex(), 8, 8) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return MobEntity.getSlotForItemStack(stack) == EquipmentSlotType.HEAD;
            }
        });
        addSlot(new SlotItemHandler(maidInventory, 1 + 18 + EquipmentSlotType.CHEST.getIndex(), 8, 44) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return MobEntity.getSlotForItemStack(stack) == EquipmentSlotType.CHEST;
            }
        });
        addSlot(new SlotItemHandler(maidInventory, 1 + 18 + EquipmentSlotType.LEGS.getIndex(), 80, 8) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return MobEntity.getSlotForItemStack(stack) == EquipmentSlotType.LEGS;
            }
        });
        addSlot(new SlotItemHandler(maidInventory, 1 + 18 + EquipmentSlotType.FEET.getIndex(), 80, 44) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return MobEntity.getSlotForItemStack(stack) == EquipmentSlotType.FEET;
            }
        });
    }

}
