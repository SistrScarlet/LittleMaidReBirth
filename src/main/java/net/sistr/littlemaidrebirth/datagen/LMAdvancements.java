package net.sistr.littlemaidrebirth.datagen;

import net.sistr.littlemaidrebirth.LittleMaidReBirthMod;
import net.minecraft.advancements.Advancement;
import net.minecraft.data.DataGenerator;
import net.minecraft.util.ResourceLocation;

public class LMAdvancements extends BaseDataProvider {

    public LMAdvancements(DataGenerator dataGeneratorIn) {
        super(dataGeneratorIn);
    }

    @Override
    protected void addTables() {
        /*Advancement root = Advancement.Builder.builder().withDisplay(Items.CAKE,
                new TranslationTextComponent("advancements.scarlethill.root.title"),
                new TranslationTextComponent("advancements.scarlethill.root.description"),
                new ResourceLocation("scarlethill:textures/gui/advancements/backgrounds/scarlethill.png"),
                FrameType.TASK, false, false, false)
                .withCriterion("entered_scarlethill", ChangeDimensionTrigger.Instance.changedDimensionTo(ModDimensions.SCARLETHILL_TYPE))
                .build(location("root"));
        add(root);
        Advancement open_gate = Advancement.Builder.builder().withParent(root).withDisplay(Registration.SCARLET_PORTAL_BLOCK.get(),
                new TranslationTextComponent("advancements.scarlethill.open_gate.title"),
                new TranslationTextComponent("advancements.scarlethill.open_gate.description"), null,
                FrameType.TASK, true, true, false)
                .withCriterion("entered_scarlethill", ChangeDimensionTrigger.Instance.changedDimensionTo(ModDimensions.SCARLETHILL_TYPE))
                .build(location("open_gate"));
        add(open_gate);*/
    }

    private void add(Advancement advancement) {
        addEntryData(advancement.getId(), () -> advancement.copy().serialize());
    }

    private ResourceLocation location(String key) {
        return new ResourceLocation(LittleMaidReBirthMod.MODID, key);
    }

    @Override
    public String getPath() {
        return "advancement";
    }

    @Override
    public String getName() {
        return "LittleMaidReBirth Advancements";
    }
}
