package com.sistr.littlemaidrebirth.item;

import com.sistr.littlemaidrebirth.setup.ModSetup;
import com.sistr.littlemaidrebirth.setup.Registration;
import net.minecraft.item.Item;
import net.minecraft.item.SpawnEggItem;

public class LittleMaidSpawnEgg extends SpawnEggItem {

    public LittleMaidSpawnEgg() {
        super(Registration.LITTLE_MAID_MOB_BEFORE, 0xFFFFFF, 0x804000,
                new Item.Properties().group(ModSetup.ITEM_GROUP));
    }

}
