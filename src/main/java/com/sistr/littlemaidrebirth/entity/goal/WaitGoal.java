package com.sistr.littlemaidrebirth.entity.goal;

import com.sistr.littlemaidrebirth.entity.Tameable;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.ai.goal.Goal;

import java.util.EnumSet;

import static com.sistr.littlemaidrebirth.entity.Tameable.MovingState.WAIT;

public class WaitGoal extends Goal {
    private final CreatureEntity owner;
    private final Tameable tameable;

    public WaitGoal(CreatureEntity owner, Tameable tameable) {
        this.owner = owner;
        this.tameable = tameable;
        setMutexFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean shouldExecute() {
        return tameable.getMovingState() == WAIT;
    }

    @Override
    public void startExecuting() {
        this.owner.getNavigator().clearPath();
    }

}
