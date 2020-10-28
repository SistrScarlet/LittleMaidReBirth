package com.sistr.littlemaidrebirth.entity.goal;

import com.sistr.littlemaidrebirth.entity.Tameable;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.item.Item;

import java.util.EnumSet;
import java.util.Set;

public class FollowAtHeldItemGoal extends TameableStareAtHeldItemGoal {
    protected int reCalcCool;

    public FollowAtHeldItemGoal(CreatureEntity mob, Tameable tameable, boolean isTamed, Set<Item> items) {
        super(mob, tameable, isTamed, items);
        setMutexFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public void tick() {
        super.tick();
        if (mob.getDistanceSq(stareAt) < 2 * 2) {
            mob.getNavigator().clearPath();
            return;
        }
        if (0 < reCalcCool--) {
            return;
        }
        reCalcCool = 10;
        mob.getNavigator().tryMoveToEntityLiving(stareAt, 1);
    }
}
