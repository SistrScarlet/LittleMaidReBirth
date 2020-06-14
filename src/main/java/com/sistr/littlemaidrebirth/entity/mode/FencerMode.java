package com.sistr.littlemaidrebirth.entity.mode;

import com.sistr.littlemaidrebirth.entity.IHasFakePlayer;
import com.sistr.littlemaidrebirth.util.ModeManager;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.SwordItem;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.util.FakePlayer;

//基本的にはMeleeAttackGoalのラッパー
//ただしFakePlayerに殴らせるようにしている
//…要るかこれ？
public class FencerMode implements IMode {
    protected final CreatureEntity owner;
    protected final IHasFakePlayer hasFakePlayer;
    protected final MeleeAttackGoal melee;

    public FencerMode(CreatureEntity owner, IHasFakePlayer hasFakePlayer, double speed, boolean memory) {
        this.owner = owner;
        this.hasFakePlayer = hasFakePlayer;
        this.melee = new MeleeAttackGoal(owner, speed, memory) {

            @Override
            protected void checkAndPerformAttack(LivingEntity enemy, double distToEnemySqr) {
                double reachSq = this.getAttackReachSqr(enemy);
                if (reachSq < distToEnemySqr || 0 < this.attackTick || !attacker.canEntityBeSeen(enemy)) {
                    return;
                }
                this.attacker.swingArm(Hand.MAIN_HAND);

                hasFakePlayer.syncToFakePlayer();
                FakePlayer fake = hasFakePlayer.getFakePlayer();
                fake.attackTargetEntityWithCurrentItem(enemy);
                if (enemy instanceof MobEntity && ((MobEntity) enemy).getAttackTarget() == fake) {
                    ((MobEntity) enemy).setAttackTarget(attacker);
                }
                if (enemy.getRevengeTarget() == fake) {
                    enemy.setRevengeTarget(attacker);
                }
                this.attackTick = MathHelper.ceil(fake.getCooldownPeriod() + 0.5F);
                hasFakePlayer.syncToOrigin();

            }

            @Override
            protected double getAttackReachSqr(LivingEntity attackTarget) {
                double reach = owner.getAttribute(PlayerEntity.REACH_DISTANCE).getValue() - 0.5D;
                return reach * reach;
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
