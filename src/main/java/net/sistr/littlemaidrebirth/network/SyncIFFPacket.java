package net.sistr.littlemaidrebirth.network;

import net.sistr.littlemaidrebirth.entity.iff.HasIFF;
import net.sistr.littlemaidrebirth.entity.iff.IFF;
import net.sistr.littlemaidrebirth.entity.iff.IFFTypeManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class SyncIFFPacket {
    private final int id;
    private final CompoundNBT tag;

    public SyncIFFPacket(PacketBuffer buf) {
        this.id = buf.readVarInt();
        this.tag = buf.readCompoundTag();
    }

    public SyncIFFPacket(int id, List<IFF> iffs) {
        this.id = id;
        CompoundNBT tag = new CompoundNBT();
        ListNBT list = new ListNBT();
        tag.put("IFFs", list);
        iffs.forEach(iff -> list.add(iff.writeTag()));
        this.tag = tag;
    }

    public void toBytes(PacketBuffer buf) {
        buf.writeVarInt(id);
        buf.writeCompoundTag(tag);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            PlayerEntity player = ctx.get().getSender();
            applyIFFServer(id, tag, player);
        });
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    public static void sendC2SPacket(Entity entity, List<IFF> iffs) {
        Networking.INSTANCE.sendToServer(new SyncIFFPacket(entity.getEntityId(), iffs));
    }

    private static void applyIFFServer(int id, CompoundNBT tag, PlayerEntity player) {
        Entity entity = player.world.getEntityByID(id);
        if (!(entity instanceof HasIFF)) {
            return;
        }
        if (entity instanceof TameableEntity && !player.getUniqueID().equals(((TameableEntity) entity).getOwnerId())) {
            return;
        }
        ListNBT list = tag.getList("IFFs", 10);
        List<IFF> iffs = list.stream()
                .map(t -> (CompoundNBT) t)
                .map(t -> IFFTypeManager.getINSTANCE().loadIFF(t))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
        ((HasIFF) entity).setIFFs(iffs);
    }
}
