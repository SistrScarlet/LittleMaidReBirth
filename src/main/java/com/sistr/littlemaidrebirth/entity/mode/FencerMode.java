package com.sistr.littlemaidrebirth.entity.mode;

import com.sistr.littlemaidrebirth.entity.FakePlayerSupplier;
import com.sistr.littlemaidrebirth.util.ModeManager;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.SwordItem;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.util.FakePlayer;
import net.sistr.lmml.entity.compound.SoundPlayable;
import net.sistr.lmml.resource.util.LMSounds;

//基本的にはMeleeAttackGoalのラッパー
//ただしFakePlayerに殴らせるようにしている
public class FencerMode implements Mode {
    protected final CreatureEntity mob;
    protected final FakePlayerSupplier hasFakePlayer;
    protected final MeleeAttackGoal melee;

    public FencerMode(CreatureEntity mob, FakePlayerSupplier hasFakePlayer, double speed, boolean memory) {
        this.mob = mob;
        this.hasFakePlayer = hasFakePlayer;
        this.melee = new MeleeAttackGoal(mob, speed, memory) {

            @Override
            protected void checkAndPerformAttack(LivingEntity target, double squaredDistance) {
                double reachSq = this.getAttackReachSqr(target);
                if (reachSq < squaredDistance || 0 < attackTick || !attacker.canEntityBeSeen(target)) {
                    return;
                }
                this.attacker.getNavigator().clearPath();

                this.attacker.swingArm(Hand.MAIN_HAND);
                if (this.attacker instanceof SoundPlayable) {
                    ((SoundPlayable)attacker).play(LMSounds.ATTACK);
                }

                FakePlayer fake = hasFakePlayer.getFakePlayer();
                fake.attackTargetEntityWithCurrentItem(target);
                if (target instanceof MobEntity && ((MobEntity) target).getAttackTarget() == fake) {
                    ((MobEntity) target).setAttackTarget(attacker);
                }
                if (target.getRevengeTarget() == fake) {
                    target.setRevengeTarget(attacker);
                }
                attackTick = MathHelper.ceil(fake.getCooldownPeriod() + 0.5F) + 5;
            }

            @Override
            protected double getAttackReachSqr(LivingEntity attackTarget) {
                double reach = hasFakePlayer.getFakePlayer()
                        .getAttribute(PlayerEntity.REACH_DISTANCE).getValue() - 0.5D - 1D;
                reach = Math.max(reach, 0);
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
    public void writeModeData(CompoundNBT tag) {

    }

    @Override
    public void readModeData(CompoundNBT tag) {

    }

    @Override
    public String getName() {
        return "Fencer";
    }

    static {
        ModeManager.ModeItems items = new ModeManager.ModeItems();
        items.add(SwordItem.class);
        items.add(AxeItem.class);
        ModeManager.INSTANCE.register(FencerMode.class, items);
    }

}
