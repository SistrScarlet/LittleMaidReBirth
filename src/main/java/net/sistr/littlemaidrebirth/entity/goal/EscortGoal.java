package net.sistr.littlemaidrebirth.entity.goal;

import net.sistr.littlemaidrebirth.entity.Tameable;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.pathfinding.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.EnumSet;

public class EscortGoal<T extends CreatureEntity & Tameable> extends Goal {
    private final T tameable;
    private final World world;
    private final double speed;
    private final PathNavigator navigation;
    private final float minDistance;
    private final float maxDistance;
    private final float tpDistance;
    private float oldWaterPathfindingPenalty;
    private final boolean leavesAllowed;
    private int updateCountdownTicks;
    private LivingEntity owner;

    public EscortGoal(T tameable, double speed, float minDistance, float maxDistance, float tpDistance, boolean leavesAllowed) {
        this.tameable = tameable;
        this.world = tameable.world;
        this.speed = speed;
        this.navigation = tameable.getNavigator();
        this.minDistance = minDistance;
        this.maxDistance = maxDistance;
        this.tpDistance = tpDistance;
        this.leavesAllowed = leavesAllowed;
        this.setMutexFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        if (!(tameable.getNavigator() instanceof GroundPathNavigator) && !(tameable.getNavigator() instanceof FlyingPathNavigator)) {
            throw new IllegalArgumentException("Unsupported mob type for FollowOwnerGoal");
        }
    }

    @Override
    public boolean shouldExecute() {
        LivingEntity livingEntity = this.tameable.getTameOwner().orElse(null);
        if (livingEntity == null) {
            return false;
        } else if (livingEntity.isSpectator()) {
            return false;
        } else if (this.tameable.getMovingState() != Tameable.MovingState.ESCORT) {
            return false;
        } else if (this.tameable.getDistanceSq(livingEntity) < this.maxDistance * this.maxDistance) {
            return false;
        } else {
            this.owner = livingEntity;
            return true;
        }
    }

    @Override
    public boolean shouldContinueExecuting() {
        if (this.navigation.noPath()) {
            return false;
        } else if (this.tameable.getMovingState() != Tameable.MovingState.ESCORT) {
            return false;
        } else {
            return this.minDistance * this.minDistance < this.tameable.getDistanceSq(this.owner);
        }
    }

    @Override
    public void startExecuting() {
        this.updateCountdownTicks = 0;
        this.oldWaterPathfindingPenalty = this.tameable.getPathPriority(PathNodeType.WATER);
        this.tameable.setPathPriority(PathNodeType.WATER, 0.0F);
    }

    @Override
    public void tick() {
        this.tameable.getLookController().setLookPositionWithEntity(this.owner, 10.0F, this.tameable.getVerticalFaceSpeed());
        if (--this.updateCountdownTicks <= 0) {
            this.updateCountdownTicks = 10;
            if (!this.tameable.getLeashed() && !this.tameable.isPassenger()) {
                if (tpDistance * tpDistance <= this.tameable.getDistanceSq(this.owner)) {
                    this.tryTeleport();
                } else {
                    this.navigation.tryMoveToEntityLiving(this.owner, this.speed);
                }

            }
        }
    }

    private void tryTeleport() {
        BlockPos blockPos = this.owner.getPosition();

        for (int i = 0; i < 10; ++i) {
            int x = this.getRandomInt(-3, 3);
            int y = this.getRandomInt(-1, 1);
            int z = this.getRandomInt(-3, 3);
            boolean teleported = this.tryTeleportTo(blockPos.getX() + x, blockPos.getY() + y, blockPos.getZ() + z);
            if (teleported) {
                return;
            }
        }

    }

    private int getRandomInt(int min, int max) {
        return this.tameable.getRNG().nextInt(max - min + 1) + min;
    }

    private boolean tryTeleportTo(int x, int y, int z) {
        if (Math.abs(x - this.owner.getPosX()) < 2.0D && Math.abs(z - this.owner.getPosZ()) < 2.0D) {
            return false;
        } else if (!this.canTeleportTo(new BlockPos(x, y, z))) {
            return false;
        } else {
            this.tameable.setPositionAndRotation(x + 0.5D, y, z + 0.5D, this.tameable.rotationYaw, this.tameable.rotationPitch);
            this.navigation.clearPath();
            return true;
        }
    }

    private boolean canTeleportTo(BlockPos pos) {
        PathNodeType pathNodeType = WalkNodeProcessor.func_237231_a_(this.world, pos.toMutable());
        if (pathNodeType != PathNodeType.WALKABLE) {
            return false;
        } else {
            BlockState blockState = this.world.getBlockState(pos.down());
            if (!this.leavesAllowed && blockState.getBlock() instanceof LeavesBlock) {
                return false;
            } else {
                BlockPos blockPos = pos.subtract(this.tameable.getPosition());
                return this.world.hasNoCollisions(this.tameable, this.tameable.getBoundingBox().offset(blockPos));
            }
        }
    }

    @Override
    public void resetTask() {
        this.owner = null;
        this.navigation.clearPath();
        this.tameable.setPathPriority(PathNodeType.WATER, this.oldWaterPathfindingPenalty);
    }
}
