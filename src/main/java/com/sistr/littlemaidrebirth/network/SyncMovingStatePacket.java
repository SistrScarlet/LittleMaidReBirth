package com.sistr.littlemaidrebirth.network;

import com.sistr.littlemaidrebirth.entity.Tameable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncMovingStatePacket {
    private final int entityId;
    private final Tameable.MovingState state;

    public SyncMovingStatePacket(PacketBuffer buf) {
        this.entityId = buf.readVarInt();
        this.state = buf.readEnumValue(Tameable.MovingState.class);
    }

    public SyncMovingStatePacket(Entity entity, Tameable.MovingState state) {
        this.entityId = entity.getEntityId();
        this.state = state;
    }

    public void toBytes(PacketBuffer buf) {
        buf.writeVarInt(entityId);
        buf.writeEnumValue(state);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            PlayerEntity player = ctx.get().getSender();
            if (player == null) return;
            applyMovingStateServer(player, entityId, state);
        });
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    public static void sendC2SPacket(Entity entity, Tameable.MovingState state) {
        Networking.INSTANCE.sendToServer(new SyncMovingStatePacket(entity, state));
    }

    public static void applyMovingStateServer(PlayerEntity player, int entityId, Tameable.MovingState state) {
        Entity entity = player.world.getEntityByID(entityId);
        if (!(entity instanceof Tameable)
                || !((Tameable) entity).getTameOwnerUuid()
                .filter(ownerId -> ownerId.equals(player.getUniqueID()))
                .isPresent()) {
            return;
        }
        ((Tameable) entity).setMovingState(state);
        if (state == Tameable.MovingState.FREEDOM) {
            ((Tameable) entity).setFreedomPos(entity.getPosition());
        }
    }

}
