package com.sistr.littlemaidrebirth.mixin;

import com.sistr.littlemaidrebirth.util.BiomeAccessor;
import net.minecraft.entity.EntityClassification;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Biome.class)
public abstract class MixinBiome implements BiomeAccessor {

    @Shadow protected abstract void addSpawn(EntityClassification type, Biome.SpawnListEntry spawnListEntry);

    @Override
    public void addSpawn_LM(EntityClassification type, Biome.SpawnListEntry spawnListEntry) {
        addSpawn(type, spawnListEntry);
    }
}
