package net.sistr.littlemaidrebirth.entity.goal;

import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.HurtByTargetGoal;

import java.util.function.Predicate;

public class PredicateRevengeGoal extends HurtByTargetGoal {
    protected final Predicate<LivingEntity> target;

    public PredicateRevengeGoal(CreatureEntity mob, Predicate<LivingEntity> target, Class<?>... noRevengeTypes) {
        super(mob, noRevengeTypes);
        this.target = target;
    }

    @Override
    public boolean shouldExecute() {
        return super.shouldExecute() && target.test(this.goalOwner.getRevengeTarget());
    }


}
