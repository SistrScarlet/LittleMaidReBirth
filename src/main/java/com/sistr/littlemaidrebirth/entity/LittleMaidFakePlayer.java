package com.sistr.littlemaidrebirth.entity;

import com.google.common.collect.Lists;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.common.util.FakePlayer;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

public class LittleMaidFakePlayer implements IHasFakePlayer {
    private final LivingEntity origin;
    private final IHasInventory hasInventory;
    @Nullable
    private FakePlayer fakePlayer;

    public LittleMaidFakePlayer(LivingEntity origin, IHasInventory hasInventory) {
        this.origin = origin;
        this.hasInventory = hasInventory;
    }

    @Override
    public FakePlayer getFakePlayer() {
        if (this.fakePlayer == null) {
            this.fakePlayer = new PlayerWrapperEntity(this.origin);
            syncToFakePlayer();
        }
        return this.fakePlayer;
    }

    @Override
    public void syncToFakePlayer() {
        FakePlayer fakePlayer = getFakePlayer();
        syncEntityData(origin, fakePlayer);
        syncLivingEntityData(origin, fakePlayer);

        PlayerInventory fakeInventory = fakePlayer.inventory;
        fakeInventory.clear();
        IInventory originInventory = hasInventory.getInventory();

        //メインハンド
        fakeInventory.setInventorySlotContents(0, origin.getHeldItemMainhand());

        for (int index = 0; index < originInventory.getSizeInventory(); ++index) {
            if (!originInventory.getStackInSlot(index).isEmpty()) {
                ItemStack stack = originInventory.getStackInSlot(index);
                fakeInventory.setInventorySlotContents(index + 1, stack);//プレイヤーインベントリの0はメインハンドなのでズラす
            }
        }

        List<ItemStack> armorInventory = Lists.newArrayList(origin.getArmorInventoryList());
        for (int index = 0; index < armorInventory.size(); ++index) {
            if (!armorInventory.get(index).isEmpty()) {
                ItemStack stack = armorInventory.get(index);
                fakeInventory.setInventorySlotContents(index + 100, stack);
            }
        }

        List<ItemStack> offHandInventory = Lists.newArrayList(origin.getHeldItemOffhand());
        for (int index = 0; index < offHandInventory.size(); ++index) {
            if (!offHandInventory.get(index).isEmpty()) {
                ItemStack stack = offHandInventory.get(index);
                fakeInventory.setInventorySlotContents(index + 150, stack);
            }
        }

        if (fakePlayer.getEyeHeight() < origin.getEyeHeight() - 0.01F || origin.getEyeHeight() + 0.01F < fakePlayer.getEyeHeight()) {
            fakePlayer.recalculateSize();
        }

    }

    @Override
    public void syncToOrigin() {
        FakePlayer fakePlayer = getFakePlayer();
        syncEntityData(fakePlayer, origin);
        syncLivingEntityData(fakePlayer, origin);

        PlayerInventory fakeInventory = fakePlayer.inventory;
        IInventory originInventory = hasInventory.getInventory();
        originInventory.clear();
        for (int index = 0; index < fakeInventory.getSizeInventory(); index++) {
            ItemStack stack = fakeInventory.getStackInSlot(index);
            if (index == 0) {//メインハンド
                origin.setHeldItem(Hand.MAIN_HAND, stack);
            } else if (index <= originInventory.getSizeInventory()) {//通常インベントリ
                originInventory.setInventorySlotContents(index - 1, stack);//プレイヤーインベントリ0はメインハンドなのでズラす
            } else if (index < 100) {//インベントリサイズ差異による溢れ分はドロップさせる
                origin.entityDropItem(stack);
            } else if (index < 4 + 100) {//防具
                origin.setItemStackToSlot(EquipmentSlotType
                        .fromSlotTypeAndIndex(EquipmentSlotType.Group.ARMOR, index - 100), stack);
            } else if (index < 1 + 150) {//オフハンド
                origin.setItemStackToSlot(EquipmentSlotType.OFFHAND, stack);
            }
        }

    }

    public static void syncEntityData(Entity src, Entity dst) {
        dst.setPosition(src.getPosX(), src.getPosY(), src.getPosZ());
        dst.prevPosX = src.prevPosX;
        dst.prevPosY = src.prevPosY;
        dst.prevPosZ = src.prevPosZ;
        dst.lastTickPosX = src.lastTickPosX;
        dst.lastTickPosY = src.lastTickPosY;
        dst.lastTickPosZ = src.lastTickPosZ;
        dst.setMotion(src.getMotion());
        dst.rotationYaw = src.rotationYaw % 360.0F;
        dst.rotationPitch = src.rotationPitch % 360.0F;
        dst.prevRotationYaw = src.prevRotationYaw;
        dst.prevRotationPitch = src.prevRotationPitch;
        dst.setRotationYawHead(src.getRotationYawHead());
        dst.fallDistance = src.fallDistance;
        dst.setFire(src.getFireTimer());
        dst.setAir(src.getAir());
        dst.setOnGround(src.isOnGround());
        dst.setInvulnerable(src.isInvulnerable());
        //dst.timeUntilPortal = src.timeUntilPortal;
        dst.setUniqueId(src.getUniqueID());
        if (src.getCustomName() != null) {
            dst.setCustomName(src.getCustomName());
        }
        dst.setCustomNameVisible(src.isCustomNameVisible());
        dst.setSilent(src.isSilent());
        dst.setNoGravity(src.hasNoGravity());
        dst.setGlowing(src.isGlowing());
        if (!src.getTags().isEmpty()) {
            dst.getTags().clear();
            src.getTags().forEach(tag -> dst.getTags().add(tag));
        }
    }

    //Health HurtTime HurtByTimestamp DeathTime AbsorptionAmount Attributes ActiveEffects FallFlying SleepingX SleepingY SleepingZ Brain
    public static void syncLivingEntityData(LivingEntity src, LivingEntity dst) {
        dst.setHealth(src.getHealth());
        dst.hurtTime = src.hurtTime;
        dst.hurtResistantTime = src.hurtResistantTime;
        dst.maxHurtTime = src.maxHurtTime;
        dst.deathTime = src.deathTime;
        dst.setAbsorptionAmount(src.getAbsorptionAmount());
        src.getAttributeManager().instanceMap.values().forEach(srcInstance -> {
            ModifiableAttributeInstance dstInstance = dst.getAttribute(srcInstance.getAttribute());
            if (dstInstance != null) {
                dstInstance.setBaseValue(srcInstance.getBaseValue());
                Collection<AttributeModifier> modifiers = srcInstance.getModifierListCopy();
                for (AttributeModifier srcModifier : modifiers) {
                    if (srcModifier == null) {
                        continue;
                    }
                    AttributeModifier dstModifier = dstInstance.getModifier(srcModifier.getID());
                    if (dstModifier != null) {
                        dstInstance.removeModifier(dstModifier);
                    }
                    dstInstance.applyPersistentModifier(srcModifier);
                }
            }
        });
        src.getActivePotionMap().forEach(((effect, instance) -> dst.getActivePotionMap().put(effect, instance)));
        //FallFlying
        //Sleeping-
        //Brain
        dst.ticksSinceLastSwing = src.ticksSinceLastSwing;
        dst.activeItemStack = src.activeItemStack;
        dst.activeItemStackUseCount = src.activeItemStackUseCount;
        if (src.isHandActive()) {
            if (!dst.isHandActive()) {
                dst.setActiveHand(src.getActiveHand());
            }
        } else {
            if (dst.isHandActive()) {
                dst.resetActiveHand();
            }
        }
    }

    public void livingTick() {
        if (this.origin.world.isRemote) {
            return;
        }
        if (this.origin.getHealth() > 0.0F && !this.origin.isSpectator()) {
            AxisAlignedBB axisalignedbb;
            if (this.origin.isPassenger() && this.origin.getRidingEntity().isAlive()) {
                axisalignedbb = this.origin.getBoundingBox().union(this.origin.getRidingEntity().getBoundingBox()).grow(1.0D, 0.0D, 1.0D);
            } else {
                axisalignedbb = this.origin.getBoundingBox().grow(1.0D, 0.5D, 1.0D);
            }

            List<Entity> list = this.origin.world.getEntitiesWithinAABBExcludingEntity(this.origin, axisalignedbb);

            boolean alreadySync = false;
            for (Entity entity : list) {
                if (entity.isAlive()) {
                    if (!alreadySync) {
                        alreadySync = true;
                        syncToFakePlayer();
                    }
                    FakePlayer fakePlayer = getFakePlayer();
                    entity.onCollideWithPlayer(fakePlayer);
                }
            }
            if (alreadySync) {
                syncToOrigin();
            }
        }
    }

    public void onDeath(DamageSource cause) {
        if (this.origin.world.isRemote) {
            return;
        }
        syncToFakePlayer();
        getFakePlayer().onDeath(cause);
    }
}
