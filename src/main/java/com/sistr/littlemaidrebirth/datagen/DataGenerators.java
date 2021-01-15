package com.sistr.littlemaidrebirth.datagen;

import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class DataGenerators {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        ExistingFileHelper fileHelper = event.getExistingFileHelper();
        if (event.includeServer()) {
            generator.addProvider(new LMRecipes(generator));
            LMBlockTagsProvider blockTagsProvider = new LMBlockTagsProvider(generator, fileHelper);
            generator.addProvider(blockTagsProvider);
            generator.addProvider(new LMItemTagsProvider(generator, blockTagsProvider, fileHelper));
            generator.addProvider(new LMEntityLootTableProvider(generator));
            generator.addProvider(new LMAdvancements(generator));
        }
    }
}
