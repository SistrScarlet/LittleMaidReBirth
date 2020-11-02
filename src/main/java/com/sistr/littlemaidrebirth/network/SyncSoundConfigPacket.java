package com.sistr.littlemaidrebirth.network;

import com.sistr.littlemaidrebirth.entity.Tameable;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;
import net.sistr.lmml.entity.compound.SoundPlayable;
import net.sistr.lmml.resource.manager.LMConfigManager;

import java.util.function.Supplier;

public class SyncSoundConfigPacket {
    private final int entityId;
    private final String configName;

    public SyncSoundConfigPacket(PacketBuffer buf) {
        this.entityId = buf.readVarInt();
        this.configName = buf.readString(32767);
    }

    public SyncSoundConfigPacket(Entity entity, String configName) {
        this.entityId = entity.getEntityId();
        this.configName = configName;
    }

    public void toBytes(PacketBuffer buf) {
        buf.writeVarInt(entityId);
        buf.writeString(configName);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
                PlayerEntity player = Minecraft.getInstance().player;
                if (player == null) return;
                receiveS2CPacket(player, entityId, configName);
            } else {
                PlayerEntity player = ctx.get().getSender();
                if (player == null) return;
                receiveC2SPacket(player, entityId, configName);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    public static void sendC2SPacket(Entity entity, String configName) {
        Networking.INSTANCE.sendToServer(new SyncSoundConfigPacket(entity, configName));
    }

    public static void sendS2CPacket(Entity entity, String configName) {
        Networking.INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity),
                new SyncSoundConfigPacket(entity, configName));
    }

    @OnlyIn(Dist.CLIENT)
    public static void receiveS2CPacket(PlayerEntity player, int entityId, String configName) {
        World world = player.world;
        Entity entity = world.getEntityByID(entityId);
        if (entity instanceof SoundPlayable) {
            LMConfigManager.INSTANCE.getConfig(configName)
                    .ifPresent(((SoundPlayable) entity)::setConfigHolder);
        }
    }

    public static void receiveC2SPacket(PlayerEntity player, int entityId, String configName) {
        World world = player.world;
        Entity entity = world.getEntityByID(entityId);
        if (!(entity instanceof SoundPlayable)) {
            return;
        }
        if (entity instanceof Tameable
                && !((Tameable) entity).getTameOwnerUuid()
                .filter(ownerId -> ownerId.equals(player.getUniqueID()))
                .isPresent()) {
            return;
        }
        LMConfigManager.INSTANCE.getConfig(configName)
                .ifPresent(((SoundPlayable) entity)::setConfigHolder);
        sendS2CPacket(entity, configName);
    }

}
