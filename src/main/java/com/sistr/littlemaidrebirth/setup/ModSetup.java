package com.sistr.littlemaidrebirth.setup;

import com.sistr.littlemaidrebirth.Config;
import com.sistr.littlemaidrebirth.LittleMaidReBirthMod;
import com.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import com.sistr.littlemaidrebirth.entity.iff.IFFTag;
import com.sistr.littlemaidrebirth.entity.iff.IFFType;
import com.sistr.littlemaidrebirth.entity.iff.IFFTypeManager;
import com.sistr.littlemaidrebirth.network.Networking;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.attributes.GlobalEntityTypeAttributes;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.MobSpawnInfo;
import net.minecraft.world.gen.Heightmap;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = LittleMaidReBirthMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModSetup {

    public static final ItemGroup ITEM_GROUP = new ItemGroup("littlemaidrebirth.common") {
        @Override
        public ItemStack createIcon() {
            return new ItemStack(Items.CAKE);
        }
    };

    public static void init(final FMLCommonSetupEvent event) {
        Networking.registerMessages();

        GlobalEntityTypeAttributes.put(Registration.LITTLE_MAID_MOB.get(),
                LittleMaidEntity.registerAttributes().create());
        EntitySpawnPlacementRegistry.register(
                Registration.LITTLE_MAID_MOB.get(), EntitySpawnPlacementRegistry.PlacementType.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, (littleMaid, worldIn, reason, pos, random) -> LittleMaidEntity.canLittleMaidSpawn(worldIn, pos));

        IFFTypeManager iffTypeManager = IFFTypeManager.getINSTANCE();
        ForgeRegistries.ENTITIES.getValues().stream().filter(EntityType::isSummonable).forEach(entityType ->
                iffTypeManager.register(EntityType.getKey(entityType),
                        new IFFType(IFFTag.UNKNOWN, entityType)));
    }

    @SubscribeEvent
    public static void onBiomeLoading(final BiomeLoadingEvent event) {
        if (!Config.CAN_SPAWN_LM.get()) {
            return;
        }
        Biome.Category category = event.getCategory();
        if (!event.getName().getNamespace().equals("minecraft")) {
            return;
        }
        if (category == Biome.Category.NONE || category == Biome.Category.THEEND || category == Biome.Category.NETHER) {
            return;
        }
        event.getSpawns().withSpawner(EntityClassification.CREATURE,
                new MobSpawnInfo.Spawners(Registration.LITTLE_MAID_MOB.get(),
                        Config.SPAWN_WEIGHT_LM.get(), Config.SPAWN_MIN_GROUP_SIZE_LM.get(), Config.SPAWN_MAX_GROUP_SIZE_LM.get()));
    }

}
