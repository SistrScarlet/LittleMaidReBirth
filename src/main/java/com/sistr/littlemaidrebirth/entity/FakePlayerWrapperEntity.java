package com.sistr.littlemaidrebirth.entity;

import com.mojang.authlib.GameProfile;
import net.minecraft.advancements.PlayerAdvancements;
import net.minecraft.entity.*;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.play.server.SEntityEquipmentPacket;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.FakePlayer;

import java.util.List;
import java.util.Optional;

//エンティティをプレイヤーにラップするクラス
//基本的にサーバーオンリー
//アイテムの使用/アイテム回収/その他
//注意！ワールド起動時に読み込まれた場合、ワールド読み込みが停止する可能性がある
//必ずワールド読み込み後にインスタンスを生成するようにすること
public abstract class FakePlayerWrapperEntity extends FakePlayer {

    public FakePlayerWrapperEntity(LivingEntity origin) {
        super((ServerWorld) origin.world, new GameProfile(origin.getUniqueID(),
                origin.getType().getName().getString() + "_player_wrapper"));
        setEntityId(origin.getEntityId());
    }

    public abstract LivingEntity getOrigin();

    public abstract Optional<PlayerAdvancements> getOriginAdvancementTracker();

    @Override
    public void tick() {
        //Fencer
        ++ticksSinceLastSwing;
        this.applyEquipmentAttributes();
        //Archer
        this.updateActiveHand();

        //アイテム回収
        pickupItems();
    }

    private void applyEquipmentAttributes() {
        for(EquipmentSlotType equipmentslottype : EquipmentSlotType.values()) {
            ItemStack itemstack;
            switch(equipmentslottype.getSlotType()) {
                case HAND:
                    itemstack = this.handInventory.get(equipmentslottype.getIndex());
                    break;
                case ARMOR:
                    itemstack = this.armorArray.get(equipmentslottype.getIndex());
                    break;
                default:
                    continue;
            }

            ItemStack itemstack1 = this.getItemStackFromSlot(equipmentslottype);
            if (!ItemStack.areItemStacksEqual(itemstack1, itemstack)) {
                if (!itemstack1.equals(itemstack, true))
                    ((ServerWorld)this.world).getChunkProvider().sendToAllTracking(this, new SEntityEquipmentPacket(this.getEntityId(), equipmentslottype, itemstack1));
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent(this, equipmentslottype, itemstack, itemstack1));
                if (!itemstack.isEmpty()) {
                    this.getAttributes().removeAttributeModifiers(itemstack.getAttributeModifiers(equipmentslottype));
                }

                if (!itemstack1.isEmpty()) {
                    this.getAttributes().applyAttributeModifiers(itemstack1.getAttributeModifiers(equipmentslottype));
                }

                switch(equipmentslottype.getSlotType()) {
                    case HAND:
                        this.handInventory.set(equipmentslottype.getIndex(), itemstack1.copy());
                        break;
                    case ARMOR:
                        this.armorArray.set(equipmentslottype.getIndex(), itemstack1.copy());
                }
            }
        }
    }

    private void pickupItems() {
        if (this.getHealth() > 0.0F && !this.isSpectator()) {
            AxisAlignedBB box2;
            if (this.isPassenger() && !this.getRidingEntity().removed) {
                box2 = this.getBoundingBox().union(this.getRidingEntity().getBoundingBox()).expand(1.0D, 0.0D, 1.0D);
            } else {
                box2 = this.getBoundingBox().expand(1.0D, 0.5D, 1.0D);
            }

            List<Entity> list = this.world.getEntitiesWithinAABBExcludingEntity(this, box2);

            for (Entity entity : list) {
                if (!entity.removed && entity != getOrigin()) {
                    this.collideWithPlayer(entity);
                }
            }
        }
    }

    @Override
    public PlayerAdvancements getAdvancements() {
        return getOriginAdvancementTracker().orElse(super.getAdvancements());
    }

    @Override
    public EntitySize getSize(Pose poseIn) {
        return getOrigin().getSize(poseIn);
    }

    //座標系

    @Override
    public Vec3d getPositionVec() {
        Vec3d vec = getOrigin().getPositionVec();
        setPosition(vec.x, vec.y, vec.z);
        return vec;
    }

    @Override
    public double getPosYEye() {
        return getOrigin().getPosYEye();
    }

    @Override
    public BlockPos getPosition() {
        return getOrigin().getPosition();
    }

    @Override
    public AxisAlignedBB getBoundingBox() {
        return getOrigin().getBoundingBox();
    }

    /*@Override
    public AxisAlignedBB getBoundingBox(Pose pose) {
        return getOrigin().getBoundingBox(pose);
    }*/

    //体力

    @Override
    public void heal(float amount) {
        getOrigin().heal(amount);
    }

    @Override
    public float getHealth() {
        return getOrigin().getHealth();
    }

    @Override
    public void setHealth(float health) {
        getOrigin().setHealth(health);
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        return getOrigin().attackEntityFrom(source, amount);
    }

}
