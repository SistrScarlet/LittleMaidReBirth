package net.sistr.littlemaidrebirth.entity.goal;

import net.sistr.littlemaidrebirth.entity.Tameable;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.ai.goal.WaterAvoidingRandomWalkingGoal;
import net.minecraft.pathfinding.Path;
import net.minecraft.util.math.BlockPos;

import java.util.EnumSet;

//雇い主が居ない場合も発動する
public class FreedomGoal<T extends CreatureEntity & Tameable> extends WaterAvoidingRandomWalkingGoal {
    private final T tameable;
    private final double distance;
    private final double distanceSq;
    private BlockPos freedomPos;
    private int reCalcCool;

    public FreedomGoal(T mob, double speedIn, double distance) {
        super(mob, speedIn);
        this.tameable = mob;
        this.distance = distance;
        this.distanceSq = distance * distance;
        setMutexFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean shouldExecute() {
        if (tameable.getMovingState() != Tameable.MovingState.FREEDOM) {
            return false;
        }
        return super.shouldExecute();
    }

    @Override
    public void startExecuting() {
        super.startExecuting();
        freedomPos = this.tameable.getFreedomPos();
    }

    @Override
    public void tick() {
        super.tick();
        if (freedomPos == null) {
            return;
        }
        if (freedomPos.distanceSq(creature.getPositionVec(), true) < distanceSq) {
            return;
        }
        if (0 < --reCalcCool) {
            return;
        }
        reCalcCool = 20;
        //freedomPosを目指して移動
        creature.getNavigator().tryMoveToXYZ(freedomPos.getX(), freedomPos.getY(), freedomPos.getZ(), speed);
        Path path = creature.getNavigator().getPath();
        if (path != null && path.getFinalPathPoint() != null && path.getFinalPathPoint().func_224758_c(freedomPos) < distance) {
            return;
        }
        creature.getNavigator().clearPath();
        //移動しても着きそうにない場合はTP
        if (creature.world.hasNoCollisions(creature.getBoundingBox().offset(creature.getPositionVec().scale(-1)).offset(freedomPos))) {
            creature.teleportKeepLoaded(freedomPos.getX() + 0.5D, freedomPos.getY(), freedomPos.getZ() + 0.5D);
        }

    }

    @Override
    public void resetTask() {
        super.resetTask();
        freedomPos = null;
        reCalcCool = 0;
    }

}
