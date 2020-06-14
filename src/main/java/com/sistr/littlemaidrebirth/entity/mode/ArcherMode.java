package com.sistr.littlemaidrebirth.entity.mode;

import com.sistr.littlemaidrebirth.entity.IArcher;
import com.sistr.littlemaidrebirth.entity.IHasFakePlayer;
import com.sistr.littlemaidrebirth.util.ModeManager;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.entity.projectile.ProjectileHelper;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.common.util.FakePlayer;

import java.util.List;

public class ArcherMode implements IMode {
    private final CreatureEntity owner;
    private final IArcher archer;
    private final double moveSpeedAmp;
    private final IHasFakePlayer hasFakePlayer;
    private int attackCooldown;
    private final float maxAttackDistance;
    private int seeTime;
    private boolean strafingClockwise;
    private boolean strafingBackwards;
    private int strafingTime = -1;

    public ArcherMode(CreatureEntity owner, IArcher archer, IHasFakePlayer hasFakePlayer, double moveSpeedAmpIn, int attackCooldownIn, float maxAttackDistanceIn) {
        this.owner = owner;
        this.archer = archer;
        this.hasFakePlayer = hasFakePlayer;
        this.moveSpeedAmp = moveSpeedAmpIn;
        this.attackCooldown = attackCooldownIn;
        this.maxAttackDistance = maxAttackDistanceIn * maxAttackDistanceIn;
    }

    @Override
    public void startModeTask() {

    }

    public boolean shouldExecute() {
        return this.owner.getAttackTarget() != null;
    }

    public boolean shouldContinueExecuting() {
        return this.shouldExecute() || !this.owner.getNavigator().noPath();
    }

    public void startExecuting() {
        this.owner.setAggroed(true);
        this.archer.setAimingBow(true);
    }

    //todo 遠すぎる場合に近づかせる
    public void tick() {
        LivingEntity target = this.owner.getAttackTarget();
        if (target == null) {
            return;
        }
        double distanceSq = this.owner.getDistanceSq(target.getPosX(), target.getPosY(), target.getPosZ());
        boolean canSee = this.owner.getEntitySenses().canSee(target);
        boolean hasSeeTime = 0 < this.seeTime;
        if (canSee != hasSeeTime) {
            this.seeTime = 0;
        }

        if (canSee) {
            ++this.seeTime;
        } else {
            --this.seeTime;
        }

        if (distanceSq < this.maxAttackDistance && 20 <= this.seeTime) {
            this.owner.getNavigator().clearPath();
            ++this.strafingTime;
        } else {
            this.owner.getNavigator().tryMoveToEntityLiving(target, this.moveSpeedAmp);
            this.strafingTime = -1;
        }

        if (20 <= this.strafingTime) {
            if ((double) this.owner.getRNG().nextFloat() < 0.3D) {
                this.strafingClockwise = !this.strafingClockwise;
            }

            if ((double) this.owner.getRNG().nextFloat() < 0.3D) {
                this.strafingBackwards = !this.strafingBackwards;
            }

            this.strafingTime = 0;
        }

        if (this.strafingTime < 0) {
            this.owner.getLookController().setLookPositionWithEntity(target, 30.0F, 30.0F);
        } else {
            if (distanceSq > (double) (this.maxAttackDistance * 0.75F)) {
                this.strafingBackwards = false;
            } else if (distanceSq < (double) (this.maxAttackDistance * 0.25F)) {
                this.strafingBackwards = true;
            }

            this.owner.getMoveHelper().strafe(this.strafingBackwards ? -0.5F : 0.5F, this.strafingClockwise ? 0.5F : -0.5F);
            this.owner.faceEntity(target, 30.0F, 30.0F);
        }

        if (!this.owner.isHandActive()) {
            if (this.seeTime >= -60) {
                this.archer.setAimingBow(true);

                hasFakePlayer.syncToFakePlayer();
                FakePlayer fakePlayer = this.hasFakePlayer.getFakePlayer();
                ItemStack stack = fakePlayer.getHeldItemMainhand();
                stack.useItemRightClick(owner.world, fakePlayer, Hand.MAIN_HAND);
                hasFakePlayer.syncToOrigin();
            }
            return;
        }

        if (!canSee) {
            if (this.seeTime < -60) {
                this.archer.setAimingBow(false);

                hasFakePlayer.syncToFakePlayer();
                FakePlayer fakePlayer = this.hasFakePlayer.getFakePlayer();
                fakePlayer.resetActiveHand();
                hasFakePlayer.syncToOrigin();
            }
            return;
        }

        int useCount = this.owner.getItemInUseMaxCount();
        if (20 <= useCount) {
            //簡易誤射チェック、射線にターゲット以外が居る場合撃たない、まだ甘い
            float distance = MathHelper.sqrt(distanceSq);
            EntityRayTraceResult result = ProjectileHelper.rayTraceEntities(this.owner,
                    this.owner.getEyePosition(1F), target.getEyePosition(1F),
                    this.owner.getBoundingBox().grow(distance), entity ->
                    !entity.isSpectator() && entity.isAlive() && entity.canBeCollidedWith(), distanceSq);
            if (result != null && result.getType() == RayTraceResult.Type.ENTITY) {
                Entity entity = result.getEntity();
                if (entity != target) {
                    return;
                }
            }

            this.archer.setAimingBow(false);

            hasFakePlayer.syncToFakePlayer();
            FakePlayer fakePlayer = this.hasFakePlayer.getFakePlayer();
            fakePlayer.stopActiveHand();
            hasFakePlayer.syncToOrigin();
        }

    }

    public void resetTask() {
        this.owner.setAggroed(false);
        this.seeTime = 0;
        this.archer.setAimingBow(false);

        hasFakePlayer.syncToFakePlayer();
        FakePlayer fakePlayer = this.hasFakePlayer.getFakePlayer();
        fakePlayer.resetActiveHand();
        hasFakePlayer.syncToOrigin();
    }

    @Override
    public void endModeTask() {

    }

    @Override
    public String getName() {
        return "Archer";
    }

    static {
        ModeManager.ModeItems items = new ModeManager.ModeItems();
        items.add(BowItem.class);
        ModeManager.INSTANCE.register(ArcherMode.class, items);
    }
}
