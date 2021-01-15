package com.sistr.littlemaidrebirth.tags;

import com.sistr.littlemaidrebirth.LittleMaidReBirthMod;
import net.minecraft.item.Item;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.Tags;

public class LMTags {

    public static class Items {
        public static final Tags.IOptionalNamedTag<Item> MAIDS_EMPLOYABLE = tag("maids_employable");
        public static final Tags.IOptionalNamedTag<Item> MAIDS_SALARY = tag("maids_salary");

        private static Tags.IOptionalNamedTag<Item> tag(String name) {
            return ItemTags.createOptional(new ResourceLocation(LittleMaidReBirthMod.MODID, name));
        }
    }
}
