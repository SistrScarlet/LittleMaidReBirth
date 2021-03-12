package net.sistr.littlemaidrebirth.datagen;

import net.minecraft.data.DataGenerator;

public abstract class BaseTableProvider extends BaseDataProvider {

    public BaseTableProvider(DataGenerator dataGeneratorIn) {
        super(dataGeneratorIn);
    }

    @Override
    public String getPath() {
        return "loot_tables";
    }
}
