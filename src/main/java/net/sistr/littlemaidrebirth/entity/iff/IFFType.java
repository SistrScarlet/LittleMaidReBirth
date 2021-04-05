package net.sistr.littlemaidrebirth.entity.iff;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.inventory.InventoryScreen;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.INPC;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.merchant.IMerchant;
import net.minecraft.entity.monster.CreeperEntity;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.WaterMobEntity;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

public class IFFType {
    public static final Logger LOGGER = LogManager.getLogger();
    protected IFFTag iffTag;
    protected final EntityType<?> entityType;
    protected Entity entity;
    protected boolean renderClashed;

    public IFFType(IFFTag iffTag, EntityType<?> entityType) {
        this.iffTag = iffTag;
        this.entityType = entityType;
    }

    @OnlyIn(Dist.CLIENT)
    public void render(MatrixStack matrices, int x, int y, int mouseX, int mouseY) {
        FontRenderer textRenderer = Minecraft.getInstance().fontRenderer;
        textRenderer.func_243246_a(matrices, new TranslationTextComponent(entityType.getTranslationKey()),
                (float) x + 60, (float) y - textRenderer.FONT_HEIGHT, 0xFFFFFFFF);
        if (renderClashed || !(entity instanceof LivingEntity)) return;
        try {
            InventoryScreen.drawEntityOnScreen(x, y, 15, mouseX, mouseY, (LivingEntity) entity);
        } catch (Exception e) {
            LOGGER.warn("描画処理がクラッシュしました。" + entityType + ":" + entity);
            e.printStackTrace();
            renderClashed = true;
            //行われない終了処理を行う
            //ちょっと強引
            IRenderTypeBuffer.Impl buffer = Minecraft.getInstance().getRenderTypeBuffers().getBufferSource();
            buffer.finish();
            EntityRendererManager entityRendererManager = Minecraft.getInstance().getRenderManager();
            entityRendererManager.setRenderShadow(true);
            RenderSystem.popMatrix();
        }
    }

    public IFF createIFF() {
        return new IFF(iffTag, this, entityType);
    }

    public boolean checkEntity(World world) {
        entity = entityType.create(world);
        if (entity instanceof IMob && !(entity instanceof CreeperEntity)) {
            iffTag = IFFTag.ENEMY;
            return true;
        }
        if (entity instanceof AnimalEntity || entity instanceof WaterMobEntity
                || entity instanceof INPC || entity instanceof IMerchant) {
            iffTag = IFFTag.FRIEND;
            return true;
        }
        return entity instanceof LivingEntity;
    }

    public Optional<LivingEntity> getEntity() {
        return Optional.ofNullable((LivingEntity) entity);
    }

}
