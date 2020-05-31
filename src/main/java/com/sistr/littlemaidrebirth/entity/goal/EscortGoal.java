package com.sistr.littlemaidrebirth.entity.goal;

import com.sistr.littlemaidrebirth.entity.ITameable;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.pathfinding.PathNavigator;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.pathfinding.WalkNodeProcessor;
import net.minecraft.util.math.BlockPos;

import java.util.EnumSet;
import java.util.Optional;

public class EscortGoal extends Goal {
    private final CreatureEntity escort;
    private final ITameable tameable;
    private final PathNavigator navigator;
    private final float minDistanceSq;
    private final float maxDistanceSq;
    private final double speed;

    private int timeToRecalcPath;
    private float oldWaterCost;
    private Entity owner;

    public EscortGoal(CreatureEntity escort, ITameable tameable, float minDistance, float maxDistance, double speed) {
        this.escort = escort;
        this.tameable = tameable;
        this.navigator = escort.getNavigator();
        this.minDistanceSq = maxDistance * maxDistance;
        this.maxDistanceSq = minDistance * minDistance;
        this.speed = speed;
        setMutexFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean shouldExecute() {
        if (!this.tameable.getMovingState().equals(ITameable.ESCORT)) {
            return false;
        }
        Optional<Entity> optional = this.tameable.getOwner();
        if (!optional.isPresent()) {
            return false;
        }
        Entity owner = optional.get();
        if (owner.isSpectator()) {
            return false;
        }
        if (owner.getDistanceSq(this.escort) < this.maxDistanceSq) {
            return false;
        }
        this.owner = owner;
        return true;
    }

    public boolean shouldContinueExecuting() {
        if (this.navigator.noPath()) {
            return false;
        }
        if (!this.tameable.getMovingState().equals(ITameable.ESCORT)) {
            return false;
        }
        return this.minDistanceSq < owner.getDistanceSq(this.escort);

    }

    @Override
    public void startExecuting() {
        this.timeToRecalcPath = 0;
        this.oldWaterCost = this.escort.getPathPriority(PathNodeType.WATER);
        this.escort.setPathPriority(PathNodeType.WATER, 0.0F);
    }

    public void tick() {
        this.escort.getLookController().setLookPositionWithEntity(this.owner, 10.0F, (float) this.escort.getVerticalFaceSpeed());
        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = 10;
            if (!this.escort.getLeashed() && !this.escort.isPassenger()) {
                double distanceSq = this.escort.getDistanceSq(this.owner);
                if (distanceSq >= 16 * 16) {
                    this.tryTeleport();
                } else {
                    this.navigator.tryMoveToEntityLiving(this.owner, 6 * 6 < distanceSq ? speed + 0.5 : speed);
                }

            }
        }
    }

    private void tryTeleport() {
        BlockPos blockpos = this.owner.getPosition();

        for (int i = 0; i < 10; ++i) {
            int x = this.getRandomInt(-3, 3);
            int y = this.getRandomInt(-1, 1);
            int z = this.getRandomInt(-3, 3);
            if (this.tryTeleport(blockpos.getX() + x, blockpos.getY() + y, blockpos.getZ() + z)) {
                return;
            }
        }

    }

    private boolean tryTeleport(int x, int y, int z) {
        if (Math.abs(x - this.owner.getPosX()) < 2 && Math.abs(z - this.owner.getPosZ()) < 2) {
            return false;
        }
        if (!this.isTeleportable(new BlockPos(x, y, z))) {
            return false;
        }
        this.escort.setLocationAndAngles(x + 0.5, y, z + 0.5, this.escort.rotationYaw, this.escort.rotationPitch);
        this.navigator.clearPath();
        return true;
    }

    private boolean isTeleportable(BlockPos pos) {
        PathNodeType type = WalkNodeProcessor.func_227480_b_(this.escort.world, pos.getX(), pos.getY(), pos.getZ());
        if (type != PathNodeType.WALKABLE) {
            return false;
        }
        BlockState blockstate = this.escort.world.getBlockState(pos.down());
        if (blockstate.getBlock() instanceof LeavesBlock) {
            return false;
        }
        BlockPos blockpos = pos.subtract(new BlockPos(this.escort));
        return this.escort.world.hasNoCollisions(this.escort, this.escort.getBoundingBox().offset(blockpos));
    }

    private int getRandomInt(int min, int max) {
        return this.escort.getRNG().nextInt(max - min + 1) + min;
    }

    public void resetTask() {
        this.owner = null;
        this.navigator.clearPath();
        this.escort.setPathPriority(PathNodeType.WATER, this.oldWaterCost);
    }
}
