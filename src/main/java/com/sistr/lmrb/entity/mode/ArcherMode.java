package com.sistr.lmrb.entity.mode;

import com.sistr.lmrb.entity.IArcher;
import com.sistr.lmrb.util.ModeManager;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.entity.projectile.ProjectileHelper;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.MathHelper;

public class ArcherMode implements IMode {
    private final CreatureEntity owner;
    private final IArcher archer;
    private final double moveSpeedAmp;
    private int attackCooldown;
    private final float maxAttackDistance;
    private int attackTime = -1;
    private int seeTime;
    private boolean strafingClockwise;
    private boolean strafingBackwards;
    private int strafingTime = -1;

    public ArcherMode(CreatureEntity owner, IArcher archer, double moveSpeedAmpIn, int attackCooldownIn, float maxAttackDistanceIn) {
        this.owner = owner;
        this.archer = archer;
        this.moveSpeedAmp = moveSpeedAmpIn;
        this.attackCooldown = attackCooldownIn;
        this.maxAttackDistance = maxAttackDistanceIn * maxAttackDistanceIn;
    }

    @Override
    public void startModeTask() {

    }

    public boolean shouldExecute() {
        return this.owner.getAttackTarget() != null && this.hasBow();
    }

    protected boolean hasBow() {
        return ModeManager.INSTANCE.containModeItem(this, this.owner.getHeldItemMainhand());
    }

    public boolean shouldContinueExecuting() {
        return this.shouldExecute() || !this.owner.getNavigator().noPath() && this.hasBow();
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
            if (--this.attackTime <= 0 && this.seeTime >= -60) {
                this.owner.setActiveHand(Hand.MAIN_HAND);
                this.archer.setAimingBow(true);
            }
            return;
        }

        if (!canSee) {
            if (this.seeTime < -60) {
                this.owner.resetActiveHand();
                this.archer.setAimingBow(false);
            }
            return;
        }

        int useCount = this.owner.getItemInUseMaxCount();
        if (20 <= useCount) {
            this.owner.resetActiveHand();
            this.archer.setAimingBow(false);
            attackEntityWithRangedAttack(target, BowItem.getArrowVelocity(useCount));
            this.attackTime = this.attackCooldown;
        }

    }

    public void attackEntityWithRangedAttack(LivingEntity target, float distanceFactor) {
        ItemStack itemstack = this.owner.findAmmo(this.owner.getHeldItem(ProjectileHelper.getHandWith(this.owner, Items.BOW)));
        AbstractArrowEntity abstractarrowentity = ProjectileHelper.fireArrow(this.owner, itemstack, distanceFactor);
        if (this.owner.getHeldItemMainhand().getItem() instanceof net.minecraft.item.BowItem)
            abstractarrowentity = ((net.minecraft.item.BowItem) this.owner.getHeldItemMainhand().getItem()).customeArrow(abstractarrowentity);
        double x = target.getPosX() - this.owner.getPosX();
        double y = target.getPosYHeight(1D / 3D) - abstractarrowentity.getPosY();
        double z = target.getPosZ() - this.owner.getPosZ();
        double horDist = MathHelper.sqrt(x * x + z * z);
        abstractarrowentity.shoot(x, y + horDist * 0.2D, z, 1.6F, 14 - this.owner.world.getDifficulty().getId() * 4);
        this.owner.playSound(SoundEvents.ENTITY_SKELETON_SHOOT, 1.0F, 1.0F / (this.owner.getRNG().nextFloat() * 0.4F + 0.8F));
        this.owner.world.addEntity(abstractarrowentity);
    }

    public void resetTask() {
        this.owner.setAggroed(false);
        this.seeTime = 0;
        this.attackTime = -1;
        this.owner.resetActiveHand();
        this.archer.setAimingBow(false);
    }

    @Override
    public void endModeTask() {

    }

    @Override
    public String getName() {
        return "archer";
    }

    static {
        ModeManager.ModeItems items = new ModeManager.ModeItems();
        items.add(BowItem.class);
        ModeManager.INSTANCE.register(ArcherMode.class, items);
    }
}
