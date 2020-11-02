package com.sistr.littlemaidrebirth.entity.goal;

import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;

import java.util.EnumSet;
import java.util.Set;

public class StareAtHeldItemGoal extends Goal {
    protected final CreatureEntity mob;
    protected final Set<Item> items;
    protected PlayerEntity stareAt;

    public StareAtHeldItemGoal(CreatureEntity mob, Set<Item> items) {
        this.mob = mob;
        this.items = items;
        setMutexFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean shouldExecute() {
        stareAt = mob.world.getClosestPlayer(mob, 4);
        return stareAt != null && isHeldTargetItem(stareAt);
    }

    @Override
    public boolean shouldContinueExecuting() {
        return isHeldTargetItem(stareAt);
    }

    public boolean isHeldTargetItem(PlayerEntity player) {
        return items.contains(player.getHeldItemMainhand().getItem()) || items.contains(player.getHeldItemOffhand().getItem());
    }

    @Override
    public void tick() {
        mob.getLookController().setLookPositionWithEntity(stareAt, 30F, 30F);
    }
}
