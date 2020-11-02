package com.sistr.littlemaidrebirth.mixin;


import com.sistr.littlemaidrebirth.LittleMaidReBirthMod;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.connect.IMixinConnector;

public class Connector implements IMixinConnector {

    @Override
    public void connect() {
        LittleMaidReBirthMod.LOGGER.info("Invoking Mixin Connector");
        Mixins.addConfiguration("assets/littlemaidrebirth/littlemaidrebirth.mixins.json");
    }

}
