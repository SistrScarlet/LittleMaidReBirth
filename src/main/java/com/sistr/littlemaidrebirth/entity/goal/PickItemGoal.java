package com.sistr.littlemaidrebirth.entity.goal;

import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.item.ItemEntity;

//回収機能自体はエンティティ本体で行うため、正確に言うとこれはアイテムに向かうGoal
public class PickItemGoal extends Goal {
    private final CreatureEntity owner;

    public PickItemGoal(CreatureEntity owner) {
        this.owner = owner;
    }

    @Override
    public boolean shouldExecute() {
        return false;
    }

    public ItemEntity findAroundDropItem() {
        return null;
    }
}
