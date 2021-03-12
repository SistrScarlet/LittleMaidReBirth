package net.sistr.littlemaidrebirth.entity.mode;

import com.google.common.collect.Sets;
import net.sistr.littlemaidrebirth.api.mode.Mode;
import net.sistr.littlemaidrebirth.api.mode.ModeManager;
import net.sistr.littlemaidrebirth.entity.InventorySupplier;
import net.sistr.littlemaidrebirth.util.AbstractFurnaceAccessor;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.AbstractCookingRecipe;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.AbstractFurnaceTileEntity;
import net.minecraft.tileentity.HopperTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.sistr.lmml.entity.compound.SoundPlayable;
import net.sistr.lmml.resource.util.LMSounds;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Stream;

public class CookingMode<T extends CreatureEntity & InventorySupplier> implements Mode {
    private final T mob;
    private final int inventoryStart;
    private final int inventoryEnd;
    private BlockPos furnacePos;
    private int timeToRecalcPath;
    private int findCool;

    public CookingMode(T mob, int inventoryStart, int inventoryEnd) {
        this.mob = mob;
        this.inventoryStart = inventoryStart;
        this.inventoryEnd = inventoryEnd;
    }

    @Override
    public void startModeTask() {

    }

    @Override
    public boolean shouldExecute() {
        if (canUseFurnace()) {
            return true;
        }
        if (--findCool < 0) {
            findCool = 60;
            if (getFuel().isPresent()) {
                furnacePos = findFurnacePos().orElse(null);
                return furnacePos != null;
            }
        }
        return false;
    }

    public boolean canUseFurnace() {
        //かまどがなければfalse
        if (furnacePos == null) {
            return false;
        }
        AbstractFurnaceTileEntity furnace = getFurnace(furnacePos).orElse(null);
        if (furnace == null) {
            return false;
        }
        //結果スロットが埋まってる場合はtrue
        for (int availableSlot : furnace.getSlotsForFace(Direction.DOWN)) {
            ItemStack result = furnace.getStackInSlot(availableSlot);
            if (!result.isEmpty() && furnace.canExtractItem(availableSlot, result, Direction.DOWN)) {
                return true;
            }
        }
        //何か焼いている場合はtrue
        if (((AbstractFurnaceAccessor) furnace).isBurningFire_LM()) {
            for (int availableSlot : furnace.getSlotsForFace(Direction.UP)) {
                if (!furnace.getStackInSlot(availableSlot).isEmpty()) {
                    return true;
                }
            }
        }
        //焼くものがあり、燃料もある場合はtrue
        //待つ必要が無く、焼きたいわけでもない場合はfalse
        return getAllCoockable(((AbstractFurnaceAccessor) furnace).getRecipeType_LM()).findAny().isPresent();
    }

    public Optional<AbstractFurnaceTileEntity> getFurnace(BlockPos pos) {
        if (pos == null) {
            return Optional.empty();
        }
        TileEntity tile = mob.world.getTileEntity(pos);
        if (tile instanceof AbstractFurnaceTileEntity) {
            return Optional.of((AbstractFurnaceTileEntity) tile);
        }
        return Optional.empty();
    }

    public Stream<ItemStack> getAllCoockable(IRecipeType<? extends AbstractCookingRecipe> recipeType) {
        IInventory inventory = this.mob.getInventory();
        Stream.Builder<ItemStack> builder = Stream.builder();
        for (int i = 0; i < inventory.getSizeInventory(); ++i) {
            ItemStack slotStack = inventory.getStackInSlot(i);
            if (getRecipe(slotStack, recipeType).isPresent()) {
                builder.accept(slotStack);
            }
        }
        return builder.build();
    }

    public Optional<? extends AbstractCookingRecipe> getRecipe(ItemStack stack, IRecipeType<? extends AbstractCookingRecipe> recipeType) {
        return mob.world.getRecipeManager().getRecipe(recipeType, new Inventory(stack), mob.world);
    }

    public OptionalInt getFuel() {
        IInventory inventory = this.mob.getInventory();
        for (int i = inventoryStart; i < inventoryEnd; ++i) {
            ItemStack itemstack = inventory.getStackInSlot(i);
            if (isFuel(itemstack)) {
                return OptionalInt.of(i);
            }
        }
        return OptionalInt.empty();
    }

    public boolean isFuel(ItemStack stack) {
        return 0 < ForgeHooks.getBurnTime(stack);
    }

    /**
     * 使用可能なかまどを探索する。
     * ここで言う使用可能なかまどとは、手持ちのアイテムを焼けるかどうかで判定する
     */
    public Optional<BlockPos> findFurnacePos() {
        BlockPos ownerPos = mob.getPosition();
        //垂直方向に5ブロック調査
        for (int l = 0; l < 5; l++) {
            Optional<BlockPos> optional = findLayer(l, ownerPos);
            if (optional.isPresent()) {
                return optional;
            }
        }
        return Optional.empty();
    }

    public Optional<BlockPos> findLayer(int layer, BlockPos basePos) {
        BlockPos center;
        //原点高さ、一個上、一個下、二個上、二個下の順にcenterをズラす
        if (layer % 2 == 0) {
            center = basePos.down(MathHelper.floor(layer / 2F + 0.5F));
        } else {
            center = basePos.up(MathHelper.floor(layer / 2F + 0.5F));
        }
        Set<BlockPos> prevSearched = Sets.newHashSet(center);
        Set<BlockPos> allSearched = Sets.newHashSet();
        //水平方向に16ブロック調査
        for (int k = 0; k < 16; k++) {
            Optional<BlockPos> optional = findHorizon(prevSearched, allSearched);
            if (optional.isPresent()) {
                return optional;
            }
        }
        return Optional.empty();
    }

    public Optional<BlockPos> findHorizon(Set<BlockPos> prevSearched, Set<BlockPos> allSearched) {
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
                if (!canUseFurnace(getFurnace(checkPos).orElse(null))) {
                    nowSearched.add(checkPos);
                    continue;
                }
                //六面埋まったブロックは除外し、これを起点とした調査も打ち切る
                if (!isTouchAir(mob.world, checkPos)) {
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
        return Optional.empty();
    }

    public boolean canUseFurnace(AbstractFurnaceTileEntity tile) {
        if (tile == null) {
            return false;
        }
        for (int slot : tile.getSlotsForFace(Direction.UP)) {
            ItemStack stack = tile.getStackInSlot(slot);
            if (!stack.isEmpty()) continue;
            IRecipeType<? extends AbstractCookingRecipe> recipeType = ((AbstractFurnaceAccessor) tile).getRecipeType_LM();
            if (getAllCoockable(recipeType)
                    .anyMatch(cookable -> tile.canInsertItem(slot, cookable, Direction.UP))) {
                return true;
            }
        }
        return false;
    }

    public boolean isTouchAir(World world, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            if (isAir(world, pos, dir)) {
                return true;
            }
        }
        return false;
    }

    public boolean isAir(World world, BlockPos pos, Direction dir) {
        return world.isAirBlock(pos.offset(dir));
    }

    @Override
    public void startExecuting() {
        findCool = 0;
    }

    @Override
    public boolean shouldContinueExecuting() {
        return canUseFurnace();
    }

    @Override
    public void tick() {
        AbstractFurnaceTileEntity furnace = getFurnace(furnacePos)
                .orElse(getFurnace(findFurnacePos().orElse(null))
                        .orElse(null));
        if (furnace == null) {
            furnacePos = null;
            return;
        }

        this.mob.getLookController().setLookPosition(
                furnacePos.getX() + 0.5,
                furnacePos.getY() + 0.5,
                furnacePos.getZ() + 0.5);

        if (!this.mob.getPosition().withinDistance(furnacePos, 2)) {
            if (this.mob.isSneaking()) {
                this.mob.setSneaking(false);
            }
            if (--this.timeToRecalcPath <= 0) {
                this.timeToRecalcPath = 10;
                this.mob.getNavigator().tryMoveToXYZ(furnacePos.getX() + 0.5D, furnacePos.getY() + 0.5D, furnacePos.getZ() + 0.5D, 1);
            }
            return;
        }
        this.mob.getNavigator().clearPath();

        if (!this.mob.isSneaking()) {
            this.mob.setSneaking(true);
        }

        IInventory inventory = this.mob.getInventory();

        IRecipeType<? extends AbstractCookingRecipe> recipeType = ((AbstractFurnaceAccessor) furnace).getRecipeType_LM();

        getCookable(recipeType).ifPresent(cookableIndex -> tryInsertCookable(furnace, inventory, cookableIndex));
        getFuel().ifPresent(fuelIndex -> tryInsertFuel(furnace, inventory, fuelIndex));
        tryExtractItem(furnace, inventory);

    }

    public OptionalInt getCookable(IRecipeType<? extends AbstractCookingRecipe> recipeType) {
        IInventory inventory = this.mob.getInventory();
        for (int i = inventoryStart; i < inventoryEnd; ++i) {
            ItemStack slotStack = inventory.getStackInSlot(i);
            if (getRecipe(slotStack, recipeType).isPresent()) {
                return OptionalInt.of(i);
            }
        }
        return OptionalInt.empty();
    }

    private void tryInsertCookable(AbstractFurnaceTileEntity furnace, IInventory inventory, int cookableIndex) {
        int[] materialSlots = furnace.getSlotsForFace(Direction.UP);
        for (int materialSlot : materialSlots) {
            ItemStack materialSlotStack = furnace.getStackInSlot(materialSlot);
            if (!materialSlotStack.isEmpty()) {
                continue;
            }
            ItemStack material = inventory.getStackInSlot(cookableIndex);
            if (!furnace.canInsertItem(materialSlot, material, Direction.UP)) {
                continue;
            }
            furnace.setInventorySlotContents(materialSlot, material);
            inventory.removeStackFromSlot(cookableIndex);
            furnace.markDirty();
            this.mob.swingArm(Hand.MAIN_HAND);
            this.mob.playSound(SoundEvents.ENTITY_ITEM_PICKUP, 1.0F, this.mob.getRNG().nextFloat() * 0.1F + 1.0F);
            if (mob instanceof SoundPlayable) {
                ((SoundPlayable) mob).play(LMSounds.COOKING_START);
            }
            break;
        }
    }

    private void tryInsertFuel(AbstractFurnaceTileEntity furnace, IInventory inventory, int fuelIndex) {
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
            this.mob.swingArm(Hand.MAIN_HAND);
            this.mob.playSound(SoundEvents.ENTITY_ITEM_PICKUP, 1.0F, this.mob.getRNG().nextFloat() * 0.1F + 1.0F);
            if (mob instanceof SoundPlayable) {
                ((SoundPlayable) mob).play(LMSounds.ADD_FUEL);
            }
            break;
        }
    }

    private void tryExtractItem(AbstractFurnaceTileEntity furnace, IInventory inventory) {
        int[] resultSlots = furnace.getSlotsForFace(Direction.DOWN);
        for (int resultSlot : resultSlots) {
            ItemStack resultStack = furnace.getStackInSlot(resultSlot);
            if (resultStack.isEmpty()) {
                continue;
            }
            if (!furnace.canExtractItem(resultSlot, resultStack, Direction.DOWN)) {
                continue;
            }
            this.mob.swingArm(Hand.MAIN_HAND);
            this.mob.playSound(SoundEvents.ENTITY_ITEM_PICKUP, 1.0F, this.mob.getRNG().nextFloat() * 0.1F + 1.0F);
            if (mob instanceof SoundPlayable) {
                ((SoundPlayable) mob).play(LMSounds.COOKING_OVER);
            }
            ItemStack copy = resultStack.copy();
            ItemStack leftover = HopperTileEntity.putStackInInventoryAllSlots(furnace, inventory, furnace.decrStackSize(resultSlot, 1), null);
            if (leftover.isEmpty()) {
                furnace.markDirty();
                continue;
            }

            furnace.setInventorySlotContents(resultSlot, copy);
        }
    }

    @Override
    public void resetTask() {
        this.mob.setSneaking(false);
    }

    @Override
    public void endModeTask() {

    }

    @Override
    public void writeModeData(CompoundNBT tag) {
        if (furnacePos != null)
            tag.put("FurnacePos", NBTUtil.writeBlockPos(furnacePos));
    }

    @Override
    public void readModeData(CompoundNBT tag) {
        if (tag.contains("FurnacePos"))
            furnacePos = NBTUtil.readBlockPos(tag.getCompound("FurnacePos"));
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
