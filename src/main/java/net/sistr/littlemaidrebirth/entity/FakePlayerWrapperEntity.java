package net.sistr.littlemaidrebirth.entity;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.DataFixer;
import net.sistr.littlemaidrebirth.util.LivingAccessor;
import net.sistr.littlemaidrebirth.util.PlayerAccessor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.advancements.AdvancementManager;
import net.minecraft.advancements.PlayerAdvancements;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntitySize;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Pose;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketDirection;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.FakePlayer;

import javax.annotation.Nullable;
import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

//エンティティをプレイヤーにラップするクラス
//基本的にサーバーオンリー
//アイテムの使用/アイテム回収/その他
public abstract class FakePlayerWrapperEntity extends FakePlayer {
    private static final UUID FPWE_UUID = UUID.fromString("8eabd891-5b4a-44f5-8ea4-89b04100baf6");
    private static final GameProfile FPWE_PROFILE = new GameProfile(FPWE_UUID, "fake_player_name");
    @Nullable
    private static PlayerAdvancements advancementTracker;

    public FakePlayerWrapperEntity(LivingEntity origin) {
        super((ServerWorld) origin.world, FPWE_PROFILE);
        setEntityId(origin.getEntityId());
        connection = new FakePlayNetHandler(getServer(), this);
    }

    public static UUID getFPWEUuid() {
        return FPWE_UUID;
    }

    public static Optional<PlayerAdvancements> getFPWEAdvancementTracker() {
        return Optional.ofNullable(advancementTracker);
    }

    public static PlayerAdvancements initFPWEAdvancementTracker(DataFixer dataFixer, PlayerList playerManager,
                                                                AdvancementManager serverAdvancementLoader,
                                                                File file, ServerPlayerEntity serverPlayerEntity) {
        if (advancementTracker == null)
            advancementTracker = new PlayerAdvancements(dataFixer, playerManager,
                    serverAdvancementLoader, file, serverPlayerEntity);
        return advancementTracker;
    }

    public abstract LivingEntity getOrigin();

    public abstract Optional<PlayerAdvancements> getOriginAdvancementTracker();

    @Override
    public void tick() {
        //Fencer
        ++ticksSinceLastSwing;
        ((LivingAccessor) this).applyEquipmentAttributes_LM();
        //Archer
        ((LivingAccessor) this).tickActiveItemStack_LM();

        //アイテム回収
        pickupItems();

        //InventoryTick
        this.inventory.tick();

        setPositionAndRotation(getOrigin().getPosX(), getOrigin().getPosY(), getOrigin().getPosZ(),
                this.getOrigin().rotationYaw, this.getOrigin().rotationPitch);
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
                    ((PlayerAccessor) this).onCollideWithEntity_LM(entity);
                }
            }
        }
    }

    @Override
    public void onItemPickup(Entity entityIn, int quantity) {
        getOrigin().onItemPickup(entityIn, quantity);
    }

    @Override
    public PlayerAdvancements getAdvancements() {
        return getOriginAdvancementTracker().orElse(super.getAdvancements());
    }

    @Override
    public EntitySize getSize(Pose poseIn) {
        return getOrigin().getSize(poseIn);
    }

    //id系

    @Override
    public int getEntityId() {
        int id = getOrigin().getEntityId();
        if (super.getEntityId() != id) setEntityId(id);
        return id;
    }

    @Override
    public UUID getUniqueID() {
        UUID uuid = getOrigin().getUniqueID();
        if (super.getUniqueID() != uuid) setUniqueId(uuid);
        return uuid;
    }

    @Override
    public String getCachedUniqueIdString() {
        return getOrigin().getCachedUniqueIdString();
    }

    //座標系

    @Override
    public Vector3d getPositionVec() {
        Vector3d vec = getOrigin().getPositionVec();
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

    public static class FakePlayNetHandler extends ServerPlayNetHandler {

        public FakePlayNetHandler(MinecraftServer server, ServerPlayerEntity playerIn) {
            super(server, new FakeNetworkManager(), playerIn);
        }

        @Override
        public NetworkManager getNetworkManager() {
            return super.getNetworkManager();
        }

        @Override
        public void sendPacket(IPacket<?> packetIn) {

        }

        @Override
        public void sendPacket(IPacket<?> packetIn, @Nullable GenericFutureListener<? extends Future<? super Void>> futureListeners) {

        }
    }

    public static class FakeNetworkManager extends NetworkManager {

        public FakeNetworkManager() {
            super(PacketDirection.SERVERBOUND);
        }

        @Override
        public void sendPacket(IPacket<?> packetIn) {

        }

        @Override
        public void sendPacket(IPacket<?> packetIn, @Nullable GenericFutureListener<? extends Future<? super Void>> p_201058_2_) {

        }

    }

}
