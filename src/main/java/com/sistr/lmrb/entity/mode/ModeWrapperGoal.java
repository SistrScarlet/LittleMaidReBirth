package com.sistr.lmrb.entity.mode;

import net.minecraft.entity.ai.goal.Goal;

import java.util.EnumSet;

public class ModeWrapperGoal extends Goal {
    private final IHasMode owner;

    public ModeWrapperGoal(IHasMode owner) {
        this.owner = owner;
        setMutexFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean shouldExecute() {
        if (owner.getMode() == null) return false;
        return owner.getMode().shouldExecute();
    }

    @Override
    public boolean shouldContinueExecuting() {
        if (owner.getMode() == null) return false;
        return owner.getMode().shouldContinueExecuting();
    }

    @Override
    public void startExecuting() {
        if (owner.getMode() == null) return;
        owner.getMode().startExecuting();
    }

    @Override
    public void resetTask() {
        if (owner.getMode() == null) return;
        owner.getMode().resetTask();
    }

    @Override
    public void tick() {
        if (owner.getMode() == null) return;
        owner.getMode().tick();
    }
}
