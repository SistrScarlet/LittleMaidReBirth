package com.sistr.littlemaidrebirth.util;

import net.minecraft.entity.EntityClassification;
import net.minecraft.world.biome.Biome;

public interface BiomeAccessor {

    void addSpawn_LM(EntityClassification type, Biome.SpawnListEntry spawnListEntry);

}
