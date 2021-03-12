package net.sistr.littlemaidrebirth.datagen;

import net.sistr.littlemaidrebirth.setup.Registration;
import net.minecraft.data.DataGenerator;
import net.minecraft.entity.EntityType;
import net.minecraft.loot.LootParameterSets;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.LootTableManager;

public class LMEntityLootTableProvider extends BaseTableProvider {

    public LMEntityLootTableProvider(DataGenerator dataGeneratorIn) {
        super(dataGeneratorIn);
    }

    @Override
    protected void addTables() {
        add(Registration.LITTLE_MAID_MOB.get(), LootTable.builder());
    }

    private void add(EntityType<?> type, LootTable.Builder loot) {
        addEntryData(type.getLootTable(),
                () -> LootTableManager.toJson(loot.setParameterSet(LootParameterSets.ENTITY).build()));
    }

    @Override
    public String getName() {
        return "LittleMaidReBirth Entity LootTables";
    }
}
