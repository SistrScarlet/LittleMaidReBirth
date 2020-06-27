package com.sistr.littlemaidrebirth.entity;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.blacklab.lmr.entity.maidmodel.IHasMultiModel;
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

import javax.annotation.Nullable;

//todo 体力/防御力表示、モード名表示/移動状態をアイコンで表記
@OnlyIn(Dist.CLIENT)
public class LittleMaidScreen extends ContainerScreen<LittleMaidContainer> {
    private static final ResourceLocation GUI = new ResourceLocation("lmreengaged", "textures/gui/container/littlemaidinventory2.png");
    @Nullable
    private Entity openAt;
    private static final ItemStack armor = Items.DIAMOND_CHESTPLATE.getDefaultInstance();

    public LittleMaidScreen(LittleMaidContainer screenContainer, PlayerInventory inv, ITextComponent titleIn) {
        super(screenContainer, inv, titleIn);
        this.ySize = 208;
    }

    @Override
    protected void func_231160_c_() {
        super.func_231160_c_();
        if (this.field_230706_i_ != null && openAt == null) {
            openAt = this.field_230706_i_.pointedEntity;
        }
        //ひどい
        this.func_230480_a_(new Button((this.field_230708_k_ - xSize) / 2 - 20, (this.field_230709_l_ - ySize) / 2,
                20, 20, new StringTextComponent(""), button -> {
            //見ているエンティティが雇用中のメイドであるか
            if (openAt != null && openAt instanceof LivingEntity && openAt instanceof IHasMultiModel
                    && openAt instanceof ITameable && ((ITameable) openAt).getOwnerId().isPresent()
                    && ((ITameable) openAt).getOwnerId().get().equals(this.field_230706_i_.player.getUniqueID())) {
                this.field_230706_i_.displayGuiScreen(new ModelSelectScreen(new StringTextComponent(""),
                        (LivingEntity) openAt, (IHasMultiModel) openAt, ~0));
            } else {//違う場合は閉じる
                this.field_230706_i_.displayGuiScreen(null);
            }
        }
        ) {
            @Override
            public void func_230431_b_(MatrixStack matrixStack, int p_renderButton_1_, int p_renderButton_2_, float p_renderButton_3_) {
                super.func_230431_b_(matrixStack, p_renderButton_1_, p_renderButton_2_, p_renderButton_3_);
                field_230707_j_.renderItemIntoGUI(armor, this.field_230690_l_ - 8 + this.field_230688_j_ / 2, this.field_230691_m_ - 8 + this.field_230689_k_ / 2);
            }
        });
    }

    @Override
    public void func_230430_a_(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.func_230446_a_(matrixStack);
        super.func_230430_a_(matrixStack, mouseX, mouseY, partialTicks);
        this.func_230459_a_(matrixStack, mouseX, mouseY);
        if (openAt != null && openAt instanceof LivingEntity)
            InventoryScreen.drawEntityOnScreen((this.field_230708_k_ - this.xSize) / 2 + 52, (this.field_230709_l_ - this.ySize) / 2 + 59,
                    20, (this.field_230708_k_ - this.xSize) / 2F + 52 - mouseX, (this.field_230709_l_ - this.ySize) / 2F + 30 - mouseY, (LivingEntity) openAt);
    }

    @Override
    protected void func_230451_b_(MatrixStack matrixStack, int mouseX, int mouseY) {
        RenderSystem.disableBlend();
        this.field_230712_o_.func_238421_b_(matrixStack, this.field_230704_d_.getString(), 8F, 65F, 4210752);
        String insideSkirt = new TranslationTextComponent("entity.littlemaidrebirth.little_maid_mob.InsideSkirt").getString();
        this.field_230712_o_.func_238421_b_(matrixStack, insideSkirt, 168F - this.field_230712_o_.getStringWidth(insideSkirt), 65F, 4210752);
    }

    @Override
    protected void func_230450_a_(MatrixStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.field_230706_i_.getTextureManager().bindTexture(GUI);
        int relX = (this.field_230708_k_ - this.xSize) / 2;
        int relY = (this.field_230709_l_ - this.ySize) / 2;
        this.func_238474_b_(matrixStack, relX, relY, 0, 0, this.xSize, this.ySize);
    }
}
