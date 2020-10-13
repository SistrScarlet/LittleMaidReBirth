package com.sistr.littlemaidrebirth.setup;

import com.sistr.littlemaidrebirth.entity.LittleMaidContainer;
import com.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import com.sistr.littlemaidrebirth.item.BattleManualItem;
import com.sistr.littlemaidrebirth.item.LittleMaidSpawnEgg;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.Item;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import static com.sistr.littlemaidrebirth.LittleMaidReBirthMod.MODID;

public class Registration {

    private static final DeferredRegister<Item> ITEMS = new DeferredRegister<>(ForgeRegistries.ITEMS, MODID);
    private static final DeferredRegister<EntityType<?>> ENTITIES = new DeferredRegister<>(ForgeRegistries.ENTITIES, MODID);
    private static final DeferredRegister<ContainerType<?>> CONTAINERS = new DeferredRegister<>(ForgeRegistries.CONTAINERS, MODID);

    public static void init() {
        ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        ENTITIES.register(FMLJavaModLoadingContext.get().getModEventBus());
        CONTAINERS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }

    //アイテムレジストリ
    //public static final RegistryObject<Item> BATTLE_MANUAL = ITEMS.register("battle_manual", BattleManualItem::new);
    public static final RegistryObject<Item> LITTLE_MAID_SPAWN_EGG = ITEMS.register("little_maid_spawn_egg", LittleMaidSpawnEgg::new);

    //エンティティレジストリ
    public static final EntityType<LittleMaidEntity> LITTLE_MAID_MOB_BEFORE =
            EntityType.Builder.create((EntityType.IFactory<LittleMaidEntity>) LittleMaidEntity::new, EntityClassification.CREATURE)
            .size(0.7F, 1.5F)
            .setShouldReceiveVelocityUpdates(false)
            .build("little_maid_mob");
    public static final RegistryObject<EntityType<LittleMaidEntity>> LITTLE_MAID_MOB =
            ENTITIES.register("little_maid_mob", () -> LITTLE_MAID_MOB_BEFORE);

    //コンテナレジストリ
    public static final RegistryObject<ContainerType<LittleMaidContainer>> LITTLE_MAID_CONTAINER =
            CONTAINERS.register("little_maid_container", () -> IForgeContainerType.create((windowId, inv, data) ->
                    new LittleMaidContainer(windowId, inv)));

}
