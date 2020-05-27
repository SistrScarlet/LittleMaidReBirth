package com.sistr.lmrb.entity.mode;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.sistr.lmrb.util.ModeManager;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipe;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.tileentity.AbstractFurnaceTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.*;
import net.minecraftforge.common.ForgeHooks;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class CookingMode implements IMode {
    private final CreatureEntity owner;
    private final IInventory inventory;
    private BlockPos furnacePos;

    public CookingMode(CreatureEntity owner, IInventory inventory) {
        this.owner = owner;
        this.inventory = inventory;
    }

    @Override
    public void startModeTask() {

    }

    @Override
    public boolean shouldExecute() {
        return hasCookableItem() && hasFuel();
    }

    public boolean hasFuel() {
        for (int i = 0; i < inventory.getSizeInventory(); ++i) {
            ItemStack itemstack = inventory.getStackInSlot(i);
            if (0 < ForgeHooks.getBurnTime(itemstack)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasCookableItem() {
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

    @Override
    public boolean shouldContinueExecuting() {
        return shouldExecute();
    }

    @Override
    public void startExecuting() {

    }

    @Override
    public void tick() {
        if (!knownFurnacePos()) {
            Optional<BlockPos> furnacePos = findFurnacePos();
            furnacePos.ifPresent(blockPos -> this.furnacePos = furnacePos.get());
            if (this.furnacePos == null) {
                return;
            }
        }
        if (!this.owner.getPosition().withinDistance(furnacePos, 2)) {
            this.owner.getNavigator().tryMoveToXYZ(furnacePos.getX() + 0.5D, furnacePos.getY() + 0.5D, furnacePos.getZ() + 0.5D, 1);
        }
        this.owner.getNavigator().clearPath();

    }

    public boolean knownFurnacePos() {
        if (furnacePos == null) return false;
        return checkFurnace(furnacePos);
    }

    public Optional<BlockPos> findFurnacePos() {
        BlockPos ownerPos = owner.getPosition();
        for (int l = 0; l < 5; l++) {
            BlockPos center;
            if (l % 2 == 0) {
                center = ownerPos.down(MathHelper.floor(l / 2F + 0.5F));
            } else {
                center = ownerPos.up(MathHelper.floor(l / 2F + 0.5F));
            }
            Set<BlockPos> nothingPosSet = Sets.newHashSet();
            Set<BlockPos> prevNothingPosSet = Sets.newHashSet(center);
            for (int k = 0; k < 16; k++) {
                Set<BlockPos> nowNothingPosSet = Sets.newHashSet();
                for (BlockPos pos : prevNothingPosSet) {
                    for (int i = 0; i < 4; i++) {
                        Direction d = Direction.byHorizontalIndex(i);
                        BlockPos tempPos = pos.offset(d);
                        if (nothingPosSet.contains(tempPos)) {
                            continue;
                        }
                        if (!checkFurnace(tempPos)
                                || !canUseFurnace((AbstractFurnaceTileEntity) owner.world.getTileEntity(tempPos))) {
                            nowNothingPosSet.add(tempPos);
                            continue;
                        }
                        BlockRayTraceResult result = owner.world.rayTraceBlocks(new RayTraceContext(
                                owner.getEyePosition(1F),
                                new Vec3d(tempPos.getX() + 0.5F, tempPos.getY() + 0.5F, tempPos.getZ() + 0.5F),
                                RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, owner));
                        if (result.getType() != RayTraceResult.Type.MISS && !result.getPos().equals(tempPos)) {
                            nowNothingPosSet.add(tempPos);
                            continue;
                        }
                        return Optional.of(tempPos);
                    }
                }
                nothingPosSet.addAll(nowNothingPosSet);
                prevNothingPosSet.clear();
                prevNothingPosSet.addAll(nowNothingPosSet);
            }
        }
        return Optional.empty();
    }

    public boolean checkFurnace(BlockPos pos) {
        TileEntity tile = owner.world.getTileEntity(pos);
        return tile instanceof AbstractFurnaceTileEntity
                && ((AbstractFurnaceTileEntity) tile).recipeType == IRecipeType.SMELTING;
    }

    public List<ItemStack> getAllCoockable() {
        List<ItemStack> coockableList = Lists.newArrayList();
        for (int i = 0; i < inventory.getSizeInventory(); ++i) {
            coockableList.add(inventory.getStackInSlot(i));
        }
        return coockableList;
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

    @Override
    public void resetTask() {

    }

    @Override
    public void endModeTask() {

    }

    @Override
    public String getName() {
        return null;
    }

    static {
        ModeManager.ModeItems items = new ModeManager.ModeItems();
        //todo クッキングアイテム
        ModeManager.INSTANCE.register(CookingMode.class, items);
    }
}
