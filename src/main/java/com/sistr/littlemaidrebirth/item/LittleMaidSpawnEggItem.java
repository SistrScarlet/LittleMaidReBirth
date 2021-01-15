package com.sistr.littlemaidrebirth.item;

import com.sistr.littlemaidrebirth.setup.ModSetup;
import com.sistr.littlemaidrebirth.setup.Registration;
import net.minecraft.item.SpawnEggItem;

public class LittleMaidSpawnEggItem extends SpawnEggItem {

    public LittleMaidSpawnEggItem() {
        super(Registration.LITTLE_MAID_MOB_BEFORE, 0xFFFFFF, 0x804000,
                new Properties().group(ModSetup.ITEM_GROUP));
    }

}
