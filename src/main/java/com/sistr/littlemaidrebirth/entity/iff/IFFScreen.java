package com.sistr.littlemaidrebirth.entity.iff;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.inventory.InventoryScreen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import net.sistr.lmml.LittleMaidModelLoader;
import net.sistr.lmml.setup.Registration;

@OnlyIn(Dist.CLIENT)
public class IFFScreen extends Screen {
    protected static final ResourceLocation MODEL_SELECT_GUI_TEXTURE = new ResourceLocation(LittleMaidModelLoader.MODID, "textures/gui/model_select.png");
    private final ImmutableList<LivingEntity> entities;
    private final LivingEntity owner;
    protected final int sizeWidth = 256;
    protected final int sizeHeight = 151;
    protected final int scale = 15;
    protected final int offsetX = 8;
    protected final int offsetY = 8;
    protected final int layerSize = scale * 3;
    protected final int layerPile = 3;
    private int scroll;
    private int select;

    public IFFScreen(ITextComponent titleIn, LivingEntity owner, HasIFF iff) {
        super(titleIn);
        this.owner = owner;
        ImmutableList.Builder<LivingEntity> builder = ImmutableList.builder();
        ForgeRegistries.ENTITIES.getValues().forEach(type -> {
            if (type != Registration.DUMMY_MODEL_ENTITY.get() && type != Registration.MULTI_MODEL_ENTITY.get()) {
                Entity entity = type.create(owner.world);
                if (entity instanceof LivingEntity) {
                    builder.add((LivingEntity) entity);
                }
            }
        });
        entities = builder.build();
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        super.render(mouseX, mouseY, partialTicks);

        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.minecraft.getTextureManager().bindTexture(MODEL_SELECT_GUI_TEXTURE);
        int relX = (this.width - sizeWidth) / 2;
        int relY = (this.height - sizeHeight) / 2;
        this.blit(relX, relY, 0, 0, sizeWidth, sizeHeight);

        int posY = layerSize * scroll + offsetY;
        int number = 0;
        for (LivingEntity entity : entities) {
            if (offsetY <= posY && posY < layerSize * layerPile + offsetY) {
                float side = width / 6F;
                String name = new TranslationTextComponent(entity.getType().getTranslationKey()).getString();
                this.font.drawString(name,
                        width / 2F + (int) (number % 2 == 0 ? side : -side) - (font.getStringWidth(name)) / 2F,
                        relY + posY + layerSize - font.FONT_HEIGHT,
                        0xFFFFFF);
                InventoryScreen.drawEntityOnScreen(
                        width / 2 + (int) (number % 2 == 0 ? side : -side),
                        relY + posY + layerSize - font.FONT_HEIGHT,
                        scale, this.width / 2F - mouseX, this.height / 2F - mouseY, entity);
            }
            posY += layerSize;
            number++;
        }

        RenderSystem.disableTexture();
        RenderSystem.enableBlend();

        if (0 <= select && -scroll <= select && select < -scroll + layerPile) {
            posY = (select + scroll) * layerSize + offsetY;
            drawColor(relX + offsetX, relY + posY,
                    relX + offsetX + scale * 16, relY + posY + layerSize,
                    0, 0xFFFFFF40);
        }

        RenderSystem.enableTexture();
        RenderSystem.disableBlend();

    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        int relX = (this.width - this.sizeWidth) / 2;
        int relY = (this.height - this.sizeHeight) / 2;
        //モデル選択
        if (relX + offsetX < x && x < relX + offsetX + scale * 16
                && relY + offsetY < y && y < relY + offsetY + layerSize * layerPile) {
            int select = ((int) y - (relY + offsetY)) / layerSize - scroll;
            if (0 <= select && select < entities.size()) {
                this.select = select;
            }
        }
        return true;
    }

    protected static void drawColor(int x1, int y1, int x2, int y2, int z, int RGBA) {
        BufferBuilder bufferbuilder = Tessellator.getInstance().getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
        bufferbuilder.pos(x1, y1, z).color(RGBA >> 24, (RGBA >> 16) & 0xFF, (RGBA >> 8) & 0xFF, RGBA & 0xFF).endVertex();
        bufferbuilder.pos(x2, y1, z).color(RGBA >> 24, (RGBA >> 16) & 0xFF, (RGBA >> 8) & 0xFF, RGBA & 0xFF).endVertex();
        bufferbuilder.pos(x2, y2, z).color(RGBA >> 24, (RGBA >> 16) & 0xFF, (RGBA >> 8) & 0xFF, RGBA & 0xFF).endVertex();
        bufferbuilder.pos(x1, y2, z).color(RGBA >> 24, (RGBA >> 16) & 0xFF, (RGBA >> 8) & 0xFF, RGBA & 0xFF).endVertex();
        bufferbuilder.finishDrawing();
        RenderSystem.enableAlphaTest();
        WorldVertexBufferUploader.draw(bufferbuilder);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollAmount) {
        boolean scrollUp = 0 < scrollAmount;
        scroll = MathHelper.clamp(scroll + (scrollUp ? 1 : -1), 2 - entities.size(), 0);
        return true;
    }

}
