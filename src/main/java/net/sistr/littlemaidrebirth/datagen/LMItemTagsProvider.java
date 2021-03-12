package net.sistr.littlemaidrebirth.datagen;

import net.sistr.littlemaidrebirth.LittleMaidReBirthMod;
import net.sistr.littlemaidrebirth.tags.LMTags;
import net.minecraft.data.BlockTagsProvider;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.ItemTagsProvider;
import net.minecraft.item.Items;
import net.minecraftforge.common.data.ExistingFileHelper;

import javax.annotation.Nullable;

public class LMItemTagsProvider extends ItemTagsProvider {

    public LMItemTagsProvider(DataGenerator dataGenerator, BlockTagsProvider blockTagProvider,
                              @Nullable ExistingFileHelper existingFileHelper) {
        super(dataGenerator, blockTagProvider, LittleMaidReBirthMod.MODID, existingFileHelper);
    }

    @Override
    public void registerTags() {
        getOrCreateBuilder(LMTags.Items.MAIDS_EMPLOYABLE).add(Items.CAKE);
        getOrCreateBuilder(LMTags.Items.MAIDS_SALARY).add(Items.SUGAR);
    }
}
