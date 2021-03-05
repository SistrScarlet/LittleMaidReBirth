package com.sistr.littlemaidrebirth.entity.mode;

import com.sistr.littlemaidrebirth.api.item.IRangedWeapon;
import com.sistr.littlemaidrebirth.entity.AimingPoseable;
import com.sistr.littlemaidrebirth.entity.FakePlayerSupplier;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileHelper;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.common.util.FakePlayer;
import net.sistr.lmml.entity.compound.SoundPlayable;
import net.sistr.lmml.resource.util.LMSounds;

public class ArcherMode<T extends CreatureEntity & AimingPoseable & FakePlayerSupplier & SoundPlayable> implements Mode {
    private final T mob;
    private final float inaccuracy;
    private final java.util.function.Predicate<Entity> friend;
    private int seeTime;
    private boolean strafingClockwise;
    private boolean strafingBackwards;
    private int strafingTime = -1;
    private int reUseCool;

    public ArcherMode(T mob, float inaccuracy, java.util.function.Predicate<Entity> friend) {
        this.mob = mob;
        this.inaccuracy = inaccuracy;
        this.friend = friend;
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
        this.mob.setAimingBow(true);
    }

    public void tick() {
        LivingEntity target = this.mob.getAttackTarget();
        if (target == null) {
            return;
        }
        double distanceSq = this.mob.getDistanceSq(target.getPosX(), target.getPosY(), target.getPosZ());
        boolean canSee = this.mob.getEntitySenses().canSee(target);
        net.minecraft.item.Item item = this.mob.getHeldItemMainhand().getItem();
        float maxRange = ((IRangedWeapon) item).getMaxRange_LMRB();
        Vector3d start = this.mob.getEyePosition(1F);
        Vector3d end = start.add(this.mob.getLook(1F)
                .scale(maxRange));
        AxisAlignedBB box = new AxisAlignedBB(start, end).grow(1D);
        EntityRayTraceResult clear = ProjectileHelper.rayTraceEntities(mob.world, this.mob, start, end, box, friend);
        canSee = canSee && clear == null;
        boolean prevCanSee = 0 < this.seeTime;
        //見えなくなるか、見えるようになったら
        if (canSee != prevCanSee) {
            this.seeTime = 0;
        }
        //見えなくなったら
        if (prevCanSee && !canSee) {
            this.strafingClockwise = !this.strafingClockwise;
        }

        if (canSee) {
            ++this.seeTime;
        } else {
            --this.seeTime;
        }

        //レンジ内
        if (distanceSq < maxRange * maxRange) {
            this.mob.getNavigator().clearPath();
            ++this.strafingTime;
        } else {
            this.strafingTime = -1;
        }

        //レンジ内かつ、視認が20tick以上
        if (20 <= this.strafingTime) {

            if ((double) this.mob.getRNG().nextFloat() < 0.1D) {
                this.strafingClockwise = !this.strafingClockwise;
            }

            this.strafingTime = 0;
        }

        if (distanceSq > (double) (maxRange * 0.75F)) {
            this.strafingBackwards = false;
        } else if (distanceSq < (double) (maxRange * 0.25F)) {
            this.strafingBackwards = true;
        }

        this.mob.getMoveHelper().strafe(this.strafingBackwards ? -0.5F : 0.5F, this.strafingClockwise ? 0.5F : -0.5F);
        this.mob.faceEntity(target, 30.0F, 30.0F);


        FakePlayer fakePlayer = this.mob.getFakePlayer();
        Vector3d vec3d = fakePlayer.getEyePosition(1F);
        Vector3d targetPos = target.getEyePosition(1F);
        double xD = targetPos.x - vec3d.x;
        double yD = targetPos.y - vec3d.y;
        double zD = targetPos.z - vec3d.z;
        double hDist = MathHelper.sqrt(xD * xD + zD * zD);
        float pitch = (float) (-(MathHelper.atan2(yD, hDist) * (180D / Math.PI)));
        pitch += ((this.mob.getRNG().nextFloat() * 2 - 1) * (this.mob.getRNG().nextFloat() * 2 - 1)) * inaccuracy;
        pitch = MathHelper.wrapDegrees(pitch);
        float yaw = MathHelper.wrapDegrees((float) (MathHelper.atan2(zD, xD) * (180D / Math.PI)) - 90.0F);
        fakePlayer.rotationYawHead = yaw;
        fakePlayer.rotationYaw = yaw;
        fakePlayer.rotationPitch = pitch;

        //FPがアイテムを構えていないとき
        if (--reUseCool < 0 && !fakePlayer.isHandActive()) {
            reUseCool = 4;
            //見えているか、見えてない時間が60tick以内
            if (-60 <= this.seeTime) {
                this.mob.setAimingBow(true);

                mob.play(LMSounds.SIGHTING);

                ItemStack stack = fakePlayer.getHeldItemMainhand();
                stack.useItemRightClick(mob.world, fakePlayer, net.minecraft.util.Hand.MAIN_HAND);
            }
            return;
        }

        //見えないなら
        if (!canSee) {
            if (this.seeTime < -60) {
                this.mob.setAimingBow(false);

                fakePlayer.resetActiveHand();
            }
            return;
        }

        //十分に引き絞ったか
        int useCount = fakePlayer.getItemInUseMaxCount();
        int interval = ((IRangedWeapon) item).getInterval_LMRB();
        if (interval <= useCount) {
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

            fakePlayer.stopActiveHand();

            mob.play(LMSounds.SHOOT);
        }

    }

    public void resetTask() {
        this.mob.setAggroed(false);
        this.seeTime = 0;
        this.mob.setAimingBow(false);

        this.mob.getNavigator().clearPath();

        FakePlayer fakePlayer = this.mob.getFakePlayer();
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
        items.add(IRangedWeapon.class);
        ModeManager.INSTANCE.register(ArcherMode.class, items);
    }
}
