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
    private final String state;

    public SyncMovingStatePacket(PacketBuffer buf) {
        this.entityId = buf.readVarInt();
        int num = buf.readByte();
        switch (num) {
            case 1:
                state = Tameable.ESCORT;
                break;
            case 2:
                state = Tameable.WAIT;
                break;
            default:
                state = Tameable.FREEDOM;
                break;
        }
    }

    public SyncMovingStatePacket(Entity entity, String state) {
        this.entityId = entity.getEntityId();
        this.state = state;
    }

    public void toBytes(PacketBuffer buf) {
        buf.writeVarInt(entityId);
        switch (state) {
            case Tameable.ESCORT:
                buf.writeByte(1);
                break;
            case Tameable.WAIT:
                buf.writeByte(2);
                break;
            default:
                buf.writeByte(0);
                break;
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            PlayerEntity player = ctx.get().getSender();
            if (player == null) return;
            receiveC2SPacket(player, entityId, state);
        });
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    public static void sendC2SPacket(Entity entity, String state) {
        Networking.INSTANCE.sendToServer(new SyncMovingStatePacket(entity, state));
    }

    public static void receiveC2SPacket(PlayerEntity player, int entityId, String state) {
        Entity entity = player.world.getEntityByID(entityId);
        if (entity instanceof Tameable) {
            if (!((Tameable) entity).getTameOwnerUuid()
                    .filter(ownerId -> ownerId.equals(player.getUniqueID()))
                    .isPresent()) {
                return;
            }
            ((Tameable) entity).setMovingState(state);
        }
    }

}
