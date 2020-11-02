package com.sistr.littlemaidrebirth.entity.goal;

import com.sistr.littlemaidrebirth.entity.Tameable;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.item.Item;

import java.util.Set;

public class TameableStareAtHeldItemGoal extends StareAtHeldItemGoal {
    protected final Tameable tameable;
    protected final boolean isTamed;

    public TameableStareAtHeldItemGoal(CreatureEntity mob, Tameable tameable, boolean isTamed, Set<Item> items) {
        super(mob, items);
        this.tameable = tameable;
        this.isTamed = isTamed;
    }

    @Override
    public boolean shouldExecute() {
        return tameable.getTameOwner().isPresent() == isTamed && super.shouldExecute();
    }
}
