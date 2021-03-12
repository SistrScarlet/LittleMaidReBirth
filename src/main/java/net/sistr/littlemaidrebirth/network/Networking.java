package net.sistr.littlemaidrebirth.network;

import net.sistr.littlemaidrebirth.LittleMaidReBirthMod;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

public class Networking {
    public static SimpleChannel INSTANCE;
    private static int ID = 0;

    public static int nextID() {
        return ID++;
    }

    public static void registerMessages() {
        INSTANCE = NetworkRegistry.newSimpleChannel(new ResourceLocation(LittleMaidReBirthMod.MODID,
                "littlemaidrebirth"), () -> "1.0", s -> true, s -> true);

        INSTANCE.registerMessage(nextID(),
                SyncMovingStatePacket.class,
                SyncMovingStatePacket::toBytes,
                SyncMovingStatePacket::new,
                SyncMovingStatePacket::handle);
        INSTANCE.registerMessage(nextID(),
                SyncSoundConfigPacket.class,
                SyncSoundConfigPacket::toBytes,
                SyncSoundConfigPacket::new,
                SyncSoundConfigPacket::handle);
        INSTANCE.registerMessage(nextID(),
                OpenIFFScreenPacket.class,
                OpenIFFScreenPacket::toBytes,
                OpenIFFScreenPacket::new,
                OpenIFFScreenPacket::handle);
        INSTANCE.registerMessage(nextID(),
                SyncIFFPacket.class,
                SyncIFFPacket::toBytes,
                SyncIFFPacket::new,
                SyncIFFPacket::handle);
    }

}
