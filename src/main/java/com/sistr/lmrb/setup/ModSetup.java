package com.sistr.lmrb.setup;

import com.sistr.lmrb.LittleMaidReBirthMod;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.sistr.lmml.ForgeEventHandlers;
import net.sistr.lmml.LittleMaidModelLoader;
import net.sistr.lmml.config.LMRConfig;
import net.sistr.lmml.network.Networking;
import net.sistr.lmml.util.loader.LMFileLoader;
import net.sistr.lmml.util.manager.ModelManager;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = LittleMaidReBirthMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModSetup {

    public static final ItemGroup ITEM_GROUP = new ItemGroup("littlemaidrebirth") {
        @Override
        public ItemStack createIcon() {
            return new ItemStack(Items.CAKE);
        }
    };

    public static void init(final FMLCommonSetupEvent event) {

    }

}
