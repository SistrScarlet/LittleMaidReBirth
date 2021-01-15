package com.sistr.littlemaidrebirth.datagen;

import com.sistr.littlemaidrebirth.setup.Registration;
import com.sistr.littlemaidrebirth.tags.LMTags;
import net.minecraft.advancements.criterion.InventoryChangeTrigger;
import net.minecraft.data.*;
import net.minecraft.item.Items;

import java.util.function.Consumer;

public class LMRecipes extends RecipeProvider {

    public LMRecipes(DataGenerator generatorIn) {
        super(generatorIn);
    }

    @Override
    protected void registerRecipes(Consumer<IFinishedRecipe> consumer) {
        ShapedRecipeBuilder.shapedRecipe(Registration.LITTLE_MAID_SPAWN_EGG_ITEM.get())
                .patternLine("SGS")
                .patternLine("CEC")
                .patternLine("SGS")
                .key('S', LMTags.Items.MAIDS_SALARY)
                .key('C', LMTags.Items.MAIDS_EMPLOYABLE)
                .key('G', Items.GOLD_INGOT)
                .key('E', Items.EGG)
                .addCriterion("sugar", InventoryChangeTrigger.Instance.forItems(Items.SUGAR))
                .addCriterion("cake", InventoryChangeTrigger.Instance.forItems(Items.CAKE))
                .addCriterion("gold_ingot", InventoryChangeTrigger.Instance.forItems(Items.GOLD_INGOT))
                .addCriterion("egg", InventoryChangeTrigger.Instance.forItems(Items.EGG))
                .build(consumer);
        ShapelessRecipeBuilder.shapelessRecipe(Registration.IFF_COPY_BOOK_ITEM.get())
                .addIngredient(LMTags.Items.MAIDS_SALARY)
                .addIngredient(Items.BOOK)
                .addCriterion("sugar", InventoryChangeTrigger.Instance.forItems(Items.SUGAR))
                .addCriterion("book", InventoryChangeTrigger.Instance.forItems(Items.BOOK))
                .build(consumer);
    }
}
