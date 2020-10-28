package com.sistr.littlemaidrebirth.entity.mode;

import com.sistr.littlemaidrebirth.entity.FakePlayerSupplier;
import com.sistr.littlemaidrebirth.entity.Tameable;
import com.sistr.littlemaidrebirth.util.ModeManager;
import net.minecraft.block.Block;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.pathfinding.Path;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.util.FakePlayer;
import net.sistr.lmml.entity.compound.SoundPlayable;
import net.sistr.lmml.resource.util.LMSounds;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

//暗所発見->移動->設置
//ライトエンジンが別スレ化しているので置いてすぐはライトレベルに変化が無い点に注意
public class TorcherMode implements Mode {
    protected final CreatureEntity mob;
    protected final FakePlayerSupplier hasFakePlayer;
    protected final Tameable tameable;
    protected final float distance;
    protected BlockPos placePos;
    protected int timeToRecalcPath;
    protected int timeToIgnore;
    protected int cool;

    public TorcherMode(CreatureEntity mob, FakePlayerSupplier hasFakePlayer, Tameable tameable, float distance) {
        this.mob = mob;
        this.hasFakePlayer = hasFakePlayer;
        this.tameable = tameable;
        this.distance = distance;
    }

    @Override
    public void startModeTask() {

    }

    @Override
    public boolean shouldExecute() {
        if (0 < --cool) {
            return false;
        }
        cool = 10;
        int ownerLight = this.tameable.getTameOwner()
                .map(owner -> mob.world.getLight(owner.getPosition()))
                .orElse(0);
        if (8 < ownerLight) {
            return false;
        }
        placePos = findSpawnablePoint().orElse(null);
        return placePos != null;
    }

    //湧けるブロックを探索
    public Optional<BlockPos> findSpawnablePoint() {
        Entity owner = tameable.getTameOwner().orElse(null);
        if (owner == null) {
            return Optional.empty();
        }
        BlockPos ownerPos = owner.getPosition();
        BlockPos start = ownerPos.add(-distance, -1, -distance);
        BlockPos end = ownerPos.add(distance, 1, distance);
        List<BlockPos> points = new ArrayList<>();
        BlockPos.getAllInBox(start, end).forEach(pos -> points.add(pos.toImmutable()));
        return points.stream()
                .sorted(Comparator.comparingDouble(ownerPos::manhattanDistance))
                .filter(this::isSpawnable)
                .filter(this::isReachable)
                .findFirst();
    }

    public boolean isSpawnable(BlockPos pos) {
        BlockPos posUp = pos.up();
        return mob.world.getBlockState(pos).isOpaqueCube(mob.world, pos) && mob.world.isAirBlock(posUp.up())
                && mob.world.getLight(posUp) <= 8
                && mob.world.getBlockState(pos).canEntitySpawn(mob.world, pos, EntityType.ZOMBIE);
    }

    public boolean isReachable(BlockPos pos) {
        Path path = mob.getNavigator().getPathToPos(pos, 4);
        return path != null && path.reachesTarget();
    }

    @Override
    public boolean shouldContinueExecuting() {
        return placePos != null;
    }

    @Override
    public void startExecuting() {
        this.mob.getNavigator().clearPath();
        Path path = this.mob.getNavigator().getPathToPos(placePos.getX(), placePos.getY(), placePos.getZ(), 3);
        this.mob.getNavigator().setPath(path, 1);
        if (mob instanceof SoundPlayable) {
            ((SoundPlayable)mob).play(LMSounds.FIND_TARGET_D);
        }
    }

    @Override
    public void tick() {
        //5秒経過しても置けないまたは明るい地点を無視
        if (100 < ++this.timeToIgnore || 8 < mob.world.getLight(placePos.up())) {
            this.placePos = null;
            this.timeToIgnore = 0;
            return;
        }
        //距離が遠すぎる場合は無視
        if (distance * distance < placePos.distanceSq(mob.getPosition())) {
            this.placePos = null;
            return;
        }
        Item item = mob.getHeldItemMainhand().getItem();
        if (!(item instanceof BlockItem)) {
            return;
        }
        if (4 * 4 < this.mob.getDistanceSq(placePos.getX(), placePos.getY(), placePos.getZ())) {
            if (--timeToRecalcPath < 0) {
                timeToRecalcPath = 20;
                Path path = this.mob.getNavigator().getPathToPos(placePos.getX(), placePos.getY(), placePos.getZ(), 3);
                this.mob.getNavigator().setPath(path, 1);
            }
            return;
        }
        Vec3d start = mob.getEyePosition(1F);
        //終端はブロックの上面
        Vec3d end = new Vec3d(
                placePos.getX() + 0.5D, placePos.getY() + 1D, placePos.getZ() + 0.5D);
        BlockRayTraceResult result = mob.world.rayTraceBlocks(new RayTraceContext(
                start, end, RayTraceContext.BlockMode.OUTLINE, RayTraceContext.FluidMode.NONE, this.mob));
        FakePlayer fakePlayer = hasFakePlayer.getFakePlayer();
        if (((BlockItem) item).tryPlace(new BlockItemUseContext(
                new ItemUseContext(fakePlayer, Hand.MAIN_HAND, result))).isSuccess()) {
            mob.swingArm(Hand.MAIN_HAND);
            if (mob instanceof SoundPlayable) {
                ((SoundPlayable)mob).play(LMSounds.INSTALLATION);
            }
        }
        this.placePos = null;
    }

    @Override
    public void resetTask() {
        this.timeToIgnore = 0;
        this.timeToRecalcPath = 0;
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
        return "Torcher";
    }

    static {
        ModeManager.ModeItems items = new ModeManager.ModeItems();
        items.add(new TorcherModeItem());
        ModeManager.INSTANCE.register(TorcherMode.class, items);
    }

    public static class TorcherModeItem implements ModeManager.CheckModeItem {

        @Override
        public boolean checkModeItem(ItemStack stack) {
            Item item = stack.getItem();
            if (!(item instanceof BlockItem)) {
                return false;
            }
            Block block = ((BlockItem) item).getBlock();
            int lightValue = block.getDefaultState().getLightValue();
            return 13 <= lightValue;
        }

    }

}
