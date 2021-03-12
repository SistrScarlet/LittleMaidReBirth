package net.sistr.littlemaidrebirth.datagen;

import net.sistr.littlemaidrebirth.LittleMaidReBirthMod;
import net.minecraft.data.BlockTagsProvider;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.ExistingFileHelper;

import javax.annotation.Nullable;

public class LMBlockTagsProvider extends BlockTagsProvider {

    public LMBlockTagsProvider(DataGenerator generatorIn, @Nullable ExistingFileHelper existingFileHelper) {
        super(generatorIn, LittleMaidReBirthMod.MODID, existingFileHelper);
    }

    @Override
    public void registerTags() {

    }
}
