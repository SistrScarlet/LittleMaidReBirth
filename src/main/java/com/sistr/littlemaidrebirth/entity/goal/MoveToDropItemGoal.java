package com.sistr.littlemaidrebirth.entity.goal;

import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.pathfinding.Path;
import net.minecraft.util.math.BlockPos;
import net.sistr.lmml.entity.compound.SoundPlayable;
import net.sistr.lmml.resource.util.LMSounds;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

//ドロップアイテムに向かうGoal
public class MoveToDropItemGoal extends Goal {
    private final CreatureEntity mob;
    private final int range;
    private final double speed;
    private int cool;

    public MoveToDropItemGoal(CreatureEntity mob, int range, double speed) {
        this.mob = mob;
        this.range = range;
        this.speed = speed;
        setMutexFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean shouldExecute() {
        if (--cool < 0) {
            cool = 60;
            Stream<BlockPos> positions = findAroundDropItem().stream().map(Entity::getPosition);
            Path path = positions.map(pos -> mob.getNavigator().getPathToPos(pos, 0))
                    .filter(Objects::nonNull)
                    .filter(Path::reachesTarget)
                    .findAny().orElse(null);
            if (path != null) {
                mob.getNavigator().setPath(path, speed);
                return true;
            }

        }
        return false;
    }

    @Override
    public void startExecuting() {
        super.startExecuting();
        if (mob instanceof SoundPlayable) {
            ((SoundPlayable) mob).play(LMSounds.FIND_TARGET_I);
        }
    }

    @Override
    public boolean shouldContinueExecuting() {
        return !mob.getNavigator().noPath();
    }

    @Override
    public void tick() {
        super.tick();
    }

    public List<ItemEntity> findAroundDropItem() {
        return mob.world.getEntitiesWithinAABB(ItemEntity.class,
                mob.getBoundingBox().expand(range,range / 4F, range),
                item -> !item.cannotPickup() && item.getDistanceSq(mob) < range * range);
    }
}
