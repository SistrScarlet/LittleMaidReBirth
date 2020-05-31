package com.sistr.littlemaidrebirth.entity.mode;

import com.sistr.littlemaidrebirth.util.ModeManager;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.item.SwordItem;

//実質MeleeAttackGoalのラッパー
public class FencerMode implements IMode {
    protected final CreatureEntity owner;
    protected final MeleeAttackGoal melee;

    public FencerMode(CreatureEntity owner, double speed, boolean memory) {
        this.owner = owner;
        this.melee = new MeleeAttackGoal(owner, speed, memory) {
            @Override
            public void tick() {
                attackTick = Math.min(attackTick, (int) (1D / owner.getAttribute(SharedMonsterAttributes.ATTACK_SPEED).getValue() * 20D));
                super.tick();
            }

            @Override
            protected double getAttackReachSqr(LivingEntity attackTarget) {
                return 2 * 2;
            }
        };
    }

    @Override
    public void startModeTask() {
    }

    //敵が生きていたら発動
    @Override
    public boolean shouldExecute() {
        return melee.shouldExecute();
    }

    @Override
    public boolean shouldContinueExecuting() {
        return melee.shouldContinueExecuting();
    }

    @Override
    public void startExecuting() {
        melee.startExecuting();
    }

    @Override
    public void tick() {
        melee.tick();
    }

    @Override
    public void resetTask() {
        melee.resetTask();
    }

    @Override
    public void endModeTask() {

    }

    @Override
    public String getName() {
        return "Fencer";
    }

    static {
        ModeManager.ModeItems items = new ModeManager.ModeItems();
        items.add(SwordItem.class);
        ModeManager.INSTANCE.register(FencerMode.class, items);
    }

}
