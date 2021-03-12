package net.sistr.littlemaidrebirth.entity.mode;

import net.sistr.littlemaidrebirth.api.mode.Mode;
import net.minecraft.entity.ai.goal.Goal;

import java.util.EnumSet;

//排他Goal
public class ModeWrapperGoal extends Goal {
    private final ModeSupplier owner;

    public ModeWrapperGoal(ModeSupplier owner) {
        this.owner = owner;
        setMutexFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean shouldExecute() {
        return owner.getMode().map(Mode::shouldExecute).orElse(false);
    }

    @Override
    public boolean shouldContinueExecuting() {
        return owner.getMode().map(Mode::shouldContinueExecuting).orElse(false);
    }

    @Override
    public void startExecuting() {
        owner.getMode().ifPresent(Mode::startExecuting);
    }

    @Override
    public void resetTask() {
        owner.getMode().ifPresent(Mode::resetTask);
    }

    @Override
    public void tick() {
        owner.getMode().ifPresent(Mode::tick);
    }
}
