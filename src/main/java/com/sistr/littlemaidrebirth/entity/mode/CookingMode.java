package com.sistr.littlemaidrebirth.entity.mode;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.sistr.littlemaidrebirth.entity.IHasInventory;
import com.sistr.littlemaidrebirth.util.ModeManager;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.FurnaceRecipe;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.tileentity.AbstractFurnaceTileEntity;
import net.minecraft.tileentity.HopperTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;

import java.util.List;
import java.util.Optional;
import java.util.Set;

//todo インベントリ直接参照ではなく、燃料と材料のセットを数秒ごとに取得する方式にする
public class CookingMode implements IMode {
    private final CreatureEntity owner;
    private final IHasInventory hasInventory;
    private BlockPos furnacePos;
    private int timeToRecalcPath;

    //IInventoryを直接渡すとなぜかnullが帰ってくることがあるのでIHasInventoryを渡している
    //多分同時に初期化してるのが原因だろうなー
    public CookingMode(CreatureEntity owner, IHasInventory hasInventory) {
        this.owner = owner;
        this.hasInventory = hasInventory;
    }

    @Override
    public void startModeTask() {

    }

    @Override
    public boolean shouldExecute() {
        return hasCookableItem();
    }

    public boolean hasCookableItem() {
        IInventory inventory = this.hasInventory.getInventory();
        for (int i = 0; i < inventory.getSizeInventory(); ++i) {
            ItemStack slotStack = inventory.getStackInSlot(i);
            if (getRecipe(slotStack).isPresent()) {
                return true;
            }
        }
        return false;
    }

    public Optional<FurnaceRecipe> getRecipe(ItemStack stack) {
        return owner.world.getRecipeManager().getRecipe(IRecipeType.SMELTING, new Inventory(stack), owner.world);
    }

    public boolean hasFuel() {
        IInventory inventory = this.hasInventory.getInventory();
        for (int i = 0; i < inventory.getSizeInventory(); ++i) {
            ItemStack itemstack = inventory.getStackInSlot(i);
            if (isFuel(itemstack)) {
                return true;
            }
        }
        return false;
    }

    public boolean isFuel(ItemStack stack) {
        return AbstractFurnaceTileEntity.isFuel(stack) && !getRecipe(stack).isPresent();
    }

    @Override
    public boolean shouldContinueExecuting() {
        if (furnacePos == null) {
            return false;
        }
        if (hasFuel()) {
            return true;
        }
        TileEntity tile = this.owner.world.getTileEntity(furnacePos);
        if (tile instanceof AbstractFurnaceTileEntity) {
            int[] materialSlots = ((AbstractFurnaceTileEntity) tile).getSlotsForFace(Direction.UP);
            for (int materialSlot : materialSlots) {
                ItemStack stack = ((AbstractFurnaceTileEntity) tile).getStackInSlot(materialSlot);
                if (!stack.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void startExecuting() {

    }

    @Override
    public void tick() {
        if (!knownFurnacePos()) {
            Optional<BlockPos> furnacePos = findFurnacePos();
            furnacePos.ifPresent(blockPos -> this.furnacePos = furnacePos.get());
        }

        if (this.furnacePos == null) {
            return;
        }

        this.owner.getLookController().setLookPosition(
                furnacePos.getX() + 0.5,
                furnacePos.getY() + 0.5,
                furnacePos.getZ() + 0.5);

        if (this.owner.isSneaking()) {
            this.owner.setSneaking(false);
        }
        if (!this.owner.func_233580_cy_().withinDistance(furnacePos, 2)) {
            if (--this.timeToRecalcPath <= 0) {
                this.timeToRecalcPath = 10;
                this.owner.getNavigator().tryMoveToXYZ(furnacePos.getX() + 0.5D, furnacePos.getY() + 0.5D, furnacePos.getZ() + 0.5D, 1);
            }
            return;
        }
        this.owner.getNavigator().clearPath();
        if (!this.owner.isSneaking()) {
            this.owner.setSneaking(true);
        }

        TileEntity tile = this.owner.world.getTileEntity(furnacePos);
        if (!checkFurnace(furnacePos)) {
            this.furnacePos = null;
            return;
        }

        IInventory inventory = this.hasInventory.getInventory();

        AbstractFurnaceTileEntity furnace = (AbstractFurnaceTileEntity) tile;

        if (hasCookableItem()) {
            int coockableIndex = getCookable();
            if (0 <= coockableIndex) {
                int[] materialSlots = furnace.getSlotsForFace(Direction.UP);
                for (int materialSlot : materialSlots) {
                    ItemStack materialSlotStack = furnace.getStackInSlot(materialSlot);
                    if (!materialSlotStack.isEmpty()) {
                        continue;
                    }
                    ItemStack material = inventory.getStackInSlot(coockableIndex);
                    if (!furnace.canInsertItem(materialSlot, material, Direction.UP)) {
                        continue;
                    }
                    furnace.setInventorySlotContents(materialSlot, material);
                    inventory.removeStackFromSlot(coockableIndex);
                    furnace.markDirty();
                    this.owner.swingArm(Hand.MAIN_HAND);
                    this.owner.playSound(SoundEvents.ENTITY_ITEM_PICKUP, 1.0F, this.owner.getRNG().nextFloat() * 0.1F + 1.0F);
                    break;
                }
            }
        }

        if (hasFuel()) {
            int fuelIndex = getFuel();
            if (0 <= fuelIndex) {
                int[] fuelSlots = furnace.getSlotsForFace(Direction.NORTH);
                for (int fuelSlot : fuelSlots) {
                    ItemStack fuelSlotStack = furnace.getStackInSlot(fuelSlot);
                    if (!fuelSlotStack.isEmpty()) {
                        continue;
                    }
                    ItemStack fuel = inventory.getStackInSlot(fuelIndex);
                    if (!furnace.canInsertItem(fuelSlot, fuel, Direction.NORTH)) {
                        continue;
                    }
                    furnace.setInventorySlotContents(fuelSlot, fuel);
                    inventory.removeStackFromSlot(fuelIndex);
                    furnace.markDirty();
                    this.owner.swingArm(Hand.MAIN_HAND);
                    this.owner.playSound(SoundEvents.ENTITY_ITEM_PICKUP, 1.0F, this.owner.getRNG().nextFloat() * 0.1F + 1.0F);
                    break;
                }
            }
        }

        int[] resultSlots = furnace.getSlotsForFace(Direction.DOWN);
        for (int resultSlot : resultSlots) {
            ItemStack resultStack = furnace.getStackInSlot(resultSlot);
            if (resultStack.isEmpty()) {
                continue;
            }
            if (!furnace.canExtractItem(resultSlot, resultStack, Direction.DOWN)) {
                continue;
            }
            this.owner.swingArm(Hand.MAIN_HAND);
            this.owner.playSound(SoundEvents.ENTITY_ITEM_PICKUP, 1.0F, this.owner.getRNG().nextFloat() * 0.1F + 1.0F);
            ItemStack copy = resultStack.copy();
            ItemStack leftover = HopperTileEntity.putStackInInventoryAllSlots(furnace, inventory, furnace.decrStackSize(resultSlot, 1), null);
            if (leftover.isEmpty()) {
                furnace.markDirty();
                continue;
            }

            furnace.setInventorySlotContents(resultSlot, copy);
        }

    }

    public boolean knownFurnacePos() {
        if (furnacePos == null) return false;
        return checkFurnace(furnacePos);
    }

    public Optional<BlockPos> findFurnacePos() {
        BlockPos ownerPos = owner.func_233580_cy_();
        //垂直方向に5ブロック調査
        for (int l = 0; l < 5; l++) {
            BlockPos center;
            //原点高さ、一個上、一個下、二個上、二個下の順にcenterをズラす
            if (l % 2 == 0) {
                center = ownerPos.down(MathHelper.floor(l / 2F + 0.5F));
            } else {
                center = ownerPos.up(MathHelper.floor(l / 2F + 0.5F));
            }
            Set<BlockPos> allSearched = Sets.newHashSet();
            Set<BlockPos> prevSearched = Sets.newHashSet(center);
            //水平方向に16ブロック調査
            for (int k = 0; k < 16; k++) {
                Set<BlockPos> nowSearched = Sets.newHashSet();
                //前回調査地点を起点にする
                for (BlockPos pos : prevSearched) {
                    //起点に隣接する水平四ブロックを調査
                    for (int i = 0; i < 4; i++) {
                        Direction d = Direction.byHorizontalIndex(i);
                        BlockPos checkPos = pos.offset(d);
                        //既に調査済みの地点は除外
                        if (allSearched.contains(checkPos)) {
                            continue;
                        }
                        //使用不能なかまどは除外し、次回検索時の起点に加える
                        if (!checkFurnace(checkPos)
                                || !canUseFurnace((AbstractFurnaceTileEntity) owner.world.getTileEntity(checkPos))) {
                            nowSearched.add(checkPos);
                            continue;
                        }
                        //見えないとこのブロックは除外し、これを起点とした調査も打ち切る
                        BlockRayTraceResult result = owner.world.rayTraceBlocks(new RayTraceContext(
                                owner.getEyePosition(1F),
                                new Vector3d(checkPos.getX() + 0.5F, checkPos.getY() + 0.5F, checkPos.getZ() + 0.5F),
                                RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, owner));
                        if (result.getType() != RayTraceResult.Type.MISS && !result.getPos().equals(checkPos)) {
                            allSearched.add(checkPos);
                            nowSearched.remove(checkPos);
                            continue;
                        }
                        //除外されなければ値を返す
                        return Optional.of(checkPos);
                    }
                }
                //次回調査用
                allSearched.addAll(nowSearched);
                prevSearched.clear();
                prevSearched.addAll(nowSearched);
            }
        }
        return Optional.empty();
    }

    public boolean checkFurnace(BlockPos pos) {
        TileEntity tile = owner.world.getTileEntity(pos);
        return tile instanceof AbstractFurnaceTileEntity
                && ((AbstractFurnaceTileEntity) tile).recipeType == IRecipeType.SMELTING;
    }

    public boolean canUseFurnace(AbstractFurnaceTileEntity tile) {
        ItemStack stack = tile.getStackInSlot(0);
        if (stack.isEmpty()) {
            return true;
        }
        for (ItemStack coockable : getAllCoockable()) {
            if (stack.getItem() == coockable.getItem()) {
                return true;
            }
        }
        return false;
    }

    public List<ItemStack> getAllCoockable() {
        IInventory inventory = this.hasInventory.getInventory();
        List<ItemStack> coockableList = Lists.newArrayList();
        for (int i = 0; i < inventory.getSizeInventory(); ++i) {
            ItemStack slotStack = inventory.getStackInSlot(i);
            if (getRecipe(slotStack).isPresent()) {
                coockableList.add(slotStack);
            }
        }
        return coockableList;
    }

    public int getCookable() {
        IInventory inventory = this.hasInventory.getInventory();
        for (int i = 0; i < inventory.getSizeInventory(); ++i) {
            ItemStack slotStack = inventory.getStackInSlot(i);
            if (getRecipe(slotStack).isPresent()) {
                return i;
            }
        }
        return -1;
    }

    public int getFuel() {
        IInventory inventory = this.hasInventory.getInventory();
        for (int i = 0; i < inventory.getSizeInventory(); ++i) {
            ItemStack itemstack = inventory.getStackInSlot(i);
            if (isFuel(itemstack)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void resetTask() {
        this.furnacePos = null;
        this.owner.setSneaking(false);
    }

    @Override
    public void endModeTask() {

    }

    @Override
    public String getName() {
        return "Cooking";
    }

    static {
        ModeManager.ModeItems items = new ModeManager.ModeItems();
        items.add(Items.BOWL);
        ModeManager.INSTANCE.register(CookingMode.class, items);
    }
}
