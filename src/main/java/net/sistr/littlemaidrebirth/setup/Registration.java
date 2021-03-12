package net.sistr.littlemaidrebirth.setup;

import net.sistr.littlemaidrebirth.entity.LittleMaidContainer;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.item.IFFCopyBookItem;
import net.sistr.littlemaidrebirth.item.LittleMaidSpawnEggItem;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.Item;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import static net.sistr.littlemaidrebirth.LittleMaidReBirthMod.MODID;

public class Registration {

    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITIES, MODID);
    private static final DeferredRegister<ContainerType<?>> CONTAINERS = DeferredRegister.create(ForgeRegistries.CONTAINERS, MODID);

    public static void init() {
        ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        ENTITIES.register(FMLJavaModLoadingContext.get().getModEventBus());
        CONTAINERS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }

    //アイテムレジストリ
    //public static final RegistryObject<Item> BATTLE_MANUAL = ITEMS.register("battle_manual", BattleManualItem::new);
    public static final RegistryObject<Item> LITTLE_MAID_SPAWN_EGG_ITEM = ITEMS.register("little_maid_spawn_egg", LittleMaidSpawnEggItem::new);
    public static final RegistryObject<Item> IFF_COPY_BOOK_ITEM = ITEMS.register("iff_copy_book", IFFCopyBookItem::new);

    public static final EntityType<LittleMaidEntity> LITTLE_MAID_MOB_BEFORE = EntityType.Builder.create((EntityType.IFactory<LittleMaidEntity>) LittleMaidEntity::new, EntityClassification.CREATURE)
            .size(0.5F, 1.35F)
            .setShouldReceiveVelocityUpdates(false)
            .build("little_maid_mob");
    public static final RegistryObject<EntityType<LittleMaidEntity>> LITTLE_MAID_MOB =
            ENTITIES.register("little_maid_mob", () -> LITTLE_MAID_MOB_BEFORE);

    public static final RegistryObject<ContainerType<LittleMaidContainer>> LITTLE_MAID_CONTAINER =
            CONTAINERS.register("little_maid_container", () -> IForgeContainerType.create(LittleMaidContainer::new));

}
