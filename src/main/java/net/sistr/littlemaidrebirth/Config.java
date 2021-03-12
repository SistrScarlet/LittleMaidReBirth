package net.sistr.littlemaidrebirth;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

//todo 野良メイドさんがアイテム拾わないようにする
@Mod.EventBusSubscriber
public class Config {

    public static final String CATEGORY_COMMON = "common";
    public static final String CATEGORY_CLIENT = "client";
    public static final String SUBCATEGORY_SPAWN = "spawn";

    public static ForgeConfigSpec COMMON_CONFIG;
    public static ForgeConfigSpec CLIENT_CONFIG;

    public static ForgeConfigSpec.BooleanValue CAN_SPAWN_LM;
    public static ForgeConfigSpec.IntValue SPAWN_WEIGHT_LM;
    public static ForgeConfigSpec.IntValue SPAWN_LIMIT_LM;
    public static ForgeConfigSpec.IntValue SPAWN_MIN_GROUP_SIZE_LM;
    public static ForgeConfigSpec.IntValue SPAWN_MAX_GROUP_SIZE_LM;
    public static ForgeConfigSpec.BooleanValue CAN_DESPAWN_LM;

    static {
        ForgeConfigSpec.Builder COMMON_BUILDER = new ForgeConfigSpec.Builder();
        ForgeConfigSpec.Builder CLIENT_BUILDER = new ForgeConfigSpec.Builder();


        COMMON_BUILDER.comment("Common settings").push(CATEGORY_COMMON);

        setupSpawnConfig(COMMON_BUILDER);

        COMMON_BUILDER.pop();

        CLIENT_BUILDER.comment("Client settings").push(CATEGORY_CLIENT);
        CLIENT_BUILDER.pop();

        COMMON_CONFIG = COMMON_BUILDER.build();
        CLIENT_CONFIG = CLIENT_BUILDER.build();
    }

    private static void setupSpawnConfig(ForgeConfigSpec.Builder COMMON_BUILDER) {
        COMMON_BUILDER.comment("Spawn settings").push(SUBCATEGORY_SPAWN);
        CAN_SPAWN_LM = COMMON_BUILDER.comment("Whether LittleMaid can spawn or not")
                .define("canSpawnLM", true);
        SPAWN_WEIGHT_LM = COMMON_BUILDER.comment("LittleMaid spawn weight")
                .defineInRange("spawnWeightLM", 5, 1, 50);
        SPAWN_LIMIT_LM = COMMON_BUILDER.comment("LittleMaid spawn limit")
                .defineInRange("spawnLimitLM", 20, 1, 200);
        SPAWN_MIN_GROUP_SIZE_LM = COMMON_BUILDER.comment("LittleMaid min group size")
                .defineInRange("spawnMinGroupSizeLM", 1, 1, 30);
        SPAWN_MAX_GROUP_SIZE_LM = COMMON_BUILDER.comment("LittleMaid max group size")
                .defineInRange("spawnMaxGroupSizeLM", 3, 1, 30);
        CAN_DESPAWN_LM = COMMON_BUILDER.comment("Whether LittleMaid can despawn or not")
                .define("canDespawnLM", false);
        COMMON_BUILDER.pop();
    }

    @SubscribeEvent
    public static void onLoad(final ModConfig.Loading configEvent) {

    }

    @SubscribeEvent
    public static void onReload(final ModConfig.Reloading configEvent) {
    }

}
