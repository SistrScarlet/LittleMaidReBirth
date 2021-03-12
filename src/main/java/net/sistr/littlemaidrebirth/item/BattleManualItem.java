package net.sistr.littlemaidrebirth.item;

import net.sistr.littlemaidrebirth.setup.ModSetup;
import net.minecraft.item.Item;

public class BattleManualItem extends Item {

    public BattleManualItem() {
        super(new Properties().group(ModSetup.ITEM_GROUP).maxStackSize(0));
    }

}
