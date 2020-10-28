package com.sistr.littlemaidrebirth.entity.mode;

import com.sistr.littlemaidrebirth.entity.AimingPoseable;
import com.sistr.littlemaidrebirth.entity.FakePlayerSupplier;
import com.sistr.littlemaidrebirth.util.ModeManager;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileHelper;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Hand;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.common.util.FakePlayer;
import net.sistr.lmml.entity.compound.SoundPlayable;
import net.sistr.lmml.resource.util.LMSounds;

//todo クロスボウとかも撃てるように調整
public class ArcherMode implements Mode {
    private final CreatureEntity mob;
    private final AimingPoseable archer;
    private final double moveSpeedAmp;
    private final FakePlayerSupplier hasFakePlayer;
    private int attackCooldown;
    private final float maxAttackDistance;
    private int seeTime;
    private boolean strafingClockwise;
    private boolean strafingBackwards;
    private int strafingTime = -1;

    public ArcherMode(CreatureEntity mob, AimingPoseable archer, FakePlayerSupplier hasFakePlayer, double moveSpeedAmpIn, int attackCooldownIn, float maxAttackDistanceIn) {
        this.mob = mob;
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
        return this.mob.getAttackTarget() != null && this.mob.getAttackTarget().isAlive();
    }

    public boolean shouldContinueExecuting() {
        return this.shouldExecute();
    }

    public void startExecuting() {
        this.mob.setAggroed(true);
        this.archer.setAimingBow(true);
    }

    public void tick() {
        LivingEntity target = this.mob.getAttackTarget();
        if (target == null) {
            return;
        }
        double distanceSq = this.mob.getDistanceSq(target.getPosX(), target.getPosY(), target.getPosZ());
        boolean canSee = this.mob.getEntitySenses().canSee(target);
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
            this.mob.getNavigator().clearPath();
            ++this.strafingTime;
        } else {
            this.mob.getNavigator().tryMoveToEntityLiving(target, this.moveSpeedAmp);
            this.strafingTime = -1;
        }

        if (20 <= this.strafingTime) {
            if ((double) this.mob.getRNG().nextFloat() < 0.3D) {
                this.strafingClockwise = !this.strafingClockwise;
            }

            if ((double) this.mob.getRNG().nextFloat() < 0.3D) {
                this.strafingBackwards = !this.strafingBackwards;
            }

            this.strafingTime = 0;
        }

        if (this.strafingTime < 0) {
            this.mob.getLookController().setLookPositionWithEntity(target, 30.0F, 30.0F);
        } else {
            if (distanceSq > (double) (this.maxAttackDistance * 0.75F)) {
                this.strafingBackwards = false;
            } else if (distanceSq < (double) (this.maxAttackDistance * 0.25F)) {
                this.strafingBackwards = true;
            }

            this.mob.getMoveHelper().strafe(this.strafingBackwards ? -0.5F : 0.5F, this.strafingClockwise ? 0.5F : -0.5F);
            this.mob.faceEntity(target, 30.0F, 30.0F);
        }

        FakePlayer fakePlayer = this.hasFakePlayer.getFakePlayer();
        if (!fakePlayer.isHandActive()) {
            if (this.seeTime >= -60) {
                this.archer.setAimingBow(true);

                if (this.mob instanceof SoundPlayable) {
                    ((SoundPlayable)mob).play(LMSounds.SIGHTING);
                }

                ItemStack stack = fakePlayer.getHeldItemMainhand();
                stack.useItemRightClick(mob.world, fakePlayer, Hand.MAIN_HAND);
            }
            return;
        }

        //見えないなら
        if (!canSee) {
            if (this.seeTime < -60) {
                this.archer.setAimingBow(false);

                fakePlayer.resetActiveHand();
            }
            return;
        }

        int useCount = fakePlayer.getItemInUseMaxCount();
        if (20 <= useCount) {
            //簡易誤射チェック、射線にターゲット以外が居る場合は撃たない
            float distance = MathHelper.sqrt(distanceSq);
            EntityRayTraceResult result = ProjectileHelper.rayTraceEntities(mob.world, mob,
                    this.mob.getEyePosition(1F), target.getEyePosition(1F),
                    this.mob.getBoundingBox().grow(distance), entity ->
                            !entity.isSpectator() && entity.isAlive() && entity.canBeCollidedWith());
            if (result != null && result.getType() == RayTraceResult.Type.ENTITY) {
                Entity entity = result.getEntity();
                if (entity != target) {
                    return;
                }
            }

            fakePlayer.rotationYaw = this.mob.rotationYaw;
            fakePlayer.rotationPitch = this.mob.rotationPitch;
            fakePlayer.setPosition(mob.getPosX(), mob.getPosY(), mob.getPosZ());

            fakePlayer.stopActiveHand();

            if (this.mob instanceof SoundPlayable) {
                ((SoundPlayable)mob).play(LMSounds.SHOOT);
            }
        }

    }

    public void resetTask() {
        this.mob.setAggroed(false);
        this.seeTime = 0;
        this.archer.setAimingBow(false);

        this.mob.getNavigator().clearPath();

        FakePlayer fakePlayer = this.hasFakePlayer.getFakePlayer();
        fakePlayer.resetActiveHand();
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
        return "Archer";
    }

    static {
        ModeManager.ModeItems items = new ModeManager.ModeItems();
        items.add(BowItem.class);
        ModeManager.INSTANCE.register(ArcherMode.class, items);
    }
}
