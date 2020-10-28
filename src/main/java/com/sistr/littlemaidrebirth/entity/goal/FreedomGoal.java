package com.sistr.littlemaidrebirth.entity.goal;

import com.sistr.littlemaidrebirth.entity.Tameable;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.ai.goal.WaterAvoidingRandomWalkingGoal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;

//todo 自由移動
public class FreedomGoal extends WaterAvoidingRandomWalkingGoal {
    private BlockPos centerPos;
    private final Tameable tameable;
    private final double distanceSq;

    public FreedomGoal(CreatureEntity creature, Tameable tameable, double speedIn, double distance) {
        super(creature, speedIn);
        this.tameable = tameable;
        this.distanceSq = distance * distance;
        setMutexFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean shouldExecute() {
        centerPos = null;
        if (tameable.getTameOwnerUuid().isPresent()) {
            if (!tameable.getMovingState().equals(Tameable.FREEDOM)) {
                return false;
            }
            centerPos = tameable.getFollowPos().orElse(null);
            if (centerPos == null) centerPos = this.creature.getPosition();
        }
        return super.shouldExecute();
    }

    @Override
    public void tick() {
        super.tick();
        if (centerPos == null) {
            return;
        }
        if (centerPos.distanceSq(creature.getPositionVec(), true) < distanceSq) {
            return;
        }
        creature.getNavigator().clearPath();
        Vec3d pos = RandomPositionGenerator.findRandomTargetBlockTowards(creature, 5, 5,
                new Vec3d(centerPos.getX(), centerPos.getY(), centerPos.getZ()));
        if (pos != null) {
            creature.getNavigator().tryMoveToXYZ(centerPos.getX(), centerPos.getY(), centerPos.getZ(), speed);
            return;
        }
        if (creature.world.hasNoCollisions(creature.getBoundingBox().offset(creature.getPositionVec().scale(-1)).offset(centerPos))) {
            creature.teleportKeepLoaded(centerPos.getX() + 0.5D, centerPos.getY(), centerPos.getZ() + 0.5D);
        }

    }

    @Override
    public void resetTask() {
        super.resetTask();
        centerPos = null;
    }

}
