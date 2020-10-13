package com.sistr.littlemaidrebirth.entity;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.screen.inventory.InventoryScreen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.sistr.lmml.client.ModelSelectScreen;
import net.sistr.lmml.entity.IHasMultiModel;

import javax.annotation.Nullable;

//todo 体力/防御力表示、モード名表示/移動状態をアイコンで表記
@OnlyIn(Dist.CLIENT)
public class LittleMaidScreen extends ContainerScreen<LittleMaidContainer> {
    private static final ResourceLocation GUI =
            new ResourceLocation("lmreengaged", "textures/gui/container/littlemaidinventory2.png");
    @Nullable
    private Entity openAt;
    private static final ItemStack armor = Items.DIAMOND_CHESTPLATE.getDefaultInstance();

    public LittleMaidScreen(LittleMaidContainer screenContainer, PlayerInventory inv, ITextComponent titleIn) {
        super(screenContainer, inv, titleIn);
        this.ySize = 208;
    }

    @Override
    protected void init() {
        super.init();
        assert minecraft != null;
        openAt = minecraft.pointedEntity;
        if (openAt == null || !(openAt instanceof IHasMultiModel)) {
            minecraft.displayGuiScreen(null);
            return;
        }
        this.addButton(new Button((this.width - xSize) / 2 - 20, (this.height - ySize) / 2,
                20, 20, new StringTextComponent(""), button ->
                minecraft.displayGuiScreen(new ModelSelectScreen(new StringTextComponent(""),
                        openAt.world, (IHasMultiModel) openAt))
        ) {
            @Override
            public void renderButton(MatrixStack matrixStack, int p_renderButton_1_, int p_renderButton_2_, float p_renderButton_3_) {
                super.renderButton(matrixStack, p_renderButton_1_, p_renderButton_2_, p_renderButton_3_);
                itemRenderer.renderItemIntoGUI(armor, this.x - 8 + this.width / 2, this.y - 8 + this.height / 2);
            }
        });
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        this.renderHoveredTooltip(matrixStack, mouseX, mouseY);
        if (openAt != null && openAt instanceof LivingEntity)
            InventoryScreen.drawEntityOnScreen((this.width - this.xSize) / 2 + 52, (this.height - this.ySize) / 2 + 59,
                    20, (this.width - this.xSize) / 2F + 52 - mouseX, (this.height - this.ySize) / 2F + 30 - mouseY, (LivingEntity) openAt);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(MatrixStack matrixStack, int mouseX, int mouseY) {
        RenderSystem.disableBlend();
        this.font.drawString(matrixStack, this.title.getString(), 8F, 65F, 4210752);
        String insideSkirt = new TranslationTextComponent("entity.littlemaidrebirth.little_maid_mob.InsideSkirt").getString();
        this.font.drawString(matrixStack, insideSkirt, 168F - font.getStringWidth(insideSkirt), 65F, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(MatrixStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        assert this.minecraft != null;
        this.minecraft.getTextureManager().bindTexture(GUI);
        int relX = (this.width - this.xSize) / 2;
        int relY = (this.height - this.ySize) / 2;
        this.blit(matrixStack, relX, relY, 0, 0, this.xSize, this.ySize);
    }
}
