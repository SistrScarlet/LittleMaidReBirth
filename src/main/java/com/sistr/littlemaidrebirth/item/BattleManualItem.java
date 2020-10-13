package com.sistr.littlemaidrebirth.item;

import com.sistr.littlemaidrebirth.setup.ModSetup;
import net.minecraft.item.Item;

public class BattleManualItem extends Item {

    public BattleManualItem() {
        super(new Item.Properties().group(ModSetup.ITEM_GROUP).maxStackSize(0));
    }

}
