package com.sistr.littlemaidrebirth.entity;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public class LittleMaidScreen extends ContainerScreen<LittleMaidContainer> {

    private final ResourceLocation GUI = new ResourceLocation("lmreengaged", "textures/gui/container/littlemaidinventory2.png");

    public LittleMaidScreen(LittleMaidContainer screenContainer, PlayerInventory inv, ITextComponent titleIn) {
        super(screenContainer, inv, titleIn);
        this.ySize = 208;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        this.renderBackground();
        super.render(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        RenderSystem.disableBlend();
        this.font.drawString(this.title.getFormattedText(), 8F, 65F, 4210752);
        String insideSkirt = new TranslationTextComponent("entity.littlemaidrebirth.little_maid_mob.InsideSkirt").getFormattedText();
        this.font.drawString(insideSkirt, 168F - font.getStringWidth(insideSkirt), 65F, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.minecraft.getTextureManager().bindTexture(GUI);
        int relX = (this.width - this.xSize) / 2;
        int relY = (this.height - this.ySize) / 2;
        this.blit(relX, relY, 0, 0, this.xSize, this.ySize);
    }
}
