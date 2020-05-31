package com.sistr.littlemaidrebirth.entity.goal;

import com.sistr.littlemaidrebirth.entity.ITameable;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.ai.goal.Goal;

import java.util.EnumSet;

public class WaitGoal extends Goal {
    private final CreatureEntity owner;
    private final ITameable tameable;

    public WaitGoal(CreatureEntity owner, ITameable tameable) {
        this.owner = owner;
        this.tameable = tameable;
        setMutexFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean shouldExecute() {
        return tameable.getMovingState().equals(ITameable.WAIT);
    }

    @Override
    public void startExecuting() {
        super.startExecuting();
        this.owner.getNavigator().clearPath();
    }

}
