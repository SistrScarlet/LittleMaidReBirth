package com.sistr.littlemaidrebirth.entity.goal;

import com.sistr.littlemaidrebirth.entity.ITameable;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.ai.goal.WaterAvoidingRandomWalkingGoal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;

import javax.annotation.Nullable;
import java.util.EnumSet;

//todo 自由移動
public class FreedomGoal extends WaterAvoidingRandomWalkingGoal {
    private BlockPos centerPos;
    private final ITameable tameable;

    public FreedomGoal(CreatureEntity creature, ITameable tameable, double speedIn) {
        super(creature, speedIn);
        this.tameable = tameable;
        setMutexFlags(EnumSet.of(Flag.MOVE));
    }

    public FreedomGoal(CreatureEntity creature, double speedIn, float probabilityIn, ITameable tameable) {
        super(creature, speedIn, probabilityIn);
        this.tameable = tameable;
    }

    public void setCenterPos() {
        centerPos = creature.func_233580_cy_();
    }

    @Override
    public boolean shouldExecute() {
        return centerPos != null && tameable.getMovingState().equals(ITameable.FREEDOM) && super.shouldExecute();
    }

    @Override
    public void startExecuting() {
        super.startExecuting();
        if (!centerPos.withinDistance(creature.func_233580_cy_(), 16)) {
            creature.attemptTeleport(
                    centerPos.getX() + 0.5F,
                    centerPos.getY() + 0.5F,
                    centerPos.getZ() + 0.5F,
                    true);
        }
    }

    @Override
    public void resetTask() {
        super.resetTask();
        centerPos = null;
    }

    @Nullable
    @Override
    protected Vector3d getPosition() {
        Vector3d superPos = super.getPosition();
        for (int i = 0; i < 3; i++) {
            if (superPos == null) {
                return null;
            }
            if (!centerPos.withinDistance(superPos, 16)) {
                superPos = super.getPosition();
                continue;
            }
            return superPos;
        }
        return creature.getPositionVec();
    }
}
