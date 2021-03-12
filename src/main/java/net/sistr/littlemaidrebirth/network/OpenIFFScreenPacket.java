package net.sistr.littlemaidrebirth.network;

import net.sistr.littlemaidrebirth.client.IFFScreen;
import net.sistr.littlemaidrebirth.entity.iff.HasIFF;
import net.sistr.littlemaidrebirth.entity.iff.IFF;
import net.sistr.littlemaidrebirth.entity.iff.IFFTypeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class OpenIFFScreenPacket {
    private final int id;
    private final CompoundNBT tag;

    public OpenIFFScreenPacket(PacketBuffer buf) {
        this.id = buf.readVarInt();
        this.tag = buf.readCompoundTag();
    }

    public OpenIFFScreenPacket(int id, List<IFF> iffs) {
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
            if (player != null)
                openIFFScreen(id, player);
            else
                openIFFScreen(id, tag, getPlayer());
        });
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    public PlayerEntity getPlayer() {
        return Minecraft.getInstance().player;
    }

    public static void sendS2CPacket(Entity entity, List<IFF> iffs, PlayerEntity player) {
        Networking.INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) player),
                new OpenIFFScreenPacket(entity.getEntityId(), iffs));
    }

    public static void sendC2SPacket(Entity entity) {
        if (!(entity instanceof HasIFF)) {
            return;
        }
        Networking.INSTANCE.sendToServer(new OpenIFFScreenPacket(entity.getEntityId(), ((HasIFF) entity).getIFFs()));
    }

    @OnlyIn(Dist.CLIENT)
    private static void openIFFScreen(int id, CompoundNBT tag, PlayerEntity player) {
        Entity entity = player.world.getEntityByID(id);
        if (!(entity instanceof HasIFF)) {
            return;
        }
        ListNBT list = tag.getList("IFFs", 10);
        List<IFF> iffs = list.stream()
                .map(t -> (CompoundNBT) t)
                .map(t -> IFFTypeManager.getINSTANCE().loadIFF(t))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        Minecraft.getInstance().displayGuiScreen(new IFFScreen(entity, iffs));
    }

    private static void openIFFScreen(int id, PlayerEntity player) {
        Entity entity = player.world.getEntityByID(id);
        if (!(entity instanceof HasIFF)
                || (entity instanceof TameableEntity
                && !player.getUniqueID().equals(((TameableEntity) entity).getOwnerId()))) {
            return;
        }
        sendS2CPacket(entity, ((HasIFF) entity).getIFFs(), player);
    }

}
