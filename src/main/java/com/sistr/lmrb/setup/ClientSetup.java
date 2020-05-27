package com.sistr.lmrb.setup;

import com.sistr.lmrb.LittleMaidReBirthMod;
import com.sistr.lmrb.client.*;
import com.sistr.lmrb.entity.LittleMaidScreen;
import net.minecraft.client.gui.ScreenManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(modid = LittleMaidReBirthMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetup {

    public static void init(final FMLCommonSetupEvent event) {
        RenderingRegistry.registerEntityRenderingHandler(Registration.LITTLE_MAID_MOB.get(), MaidModelRenderer::new);
        ScreenManager.registerFactory(Registration.LITTLE_MAID_CONTAINER.get(), LittleMaidScreen::new);
    }

}
