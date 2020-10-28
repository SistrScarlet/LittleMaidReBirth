package com.sistr.littlemaidrebirth.entity;

import com.mojang.blaze3d.systems.RenderSystem;
import com.sistr.littlemaidrebirth.LittleMaidReBirthMod;
import com.sistr.littlemaidrebirth.network.SyncMovingStatePacket;
import com.sistr.littlemaidrebirth.network.SyncSoundConfigPacket;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.screen.inventory.InventoryScreen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.util.InputMappings;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.sistr.lmml.client.ModelSelectScreen;
import net.sistr.lmml.entity.compound.IHasMultiModel;
import net.sistr.lmml.resource.manager.LMConfigManager;

//todo 体力/防御力表示、モード名表示/移動状態をアイコンで表記
@OnlyIn(Dist.CLIENT)
public class LittleMaidScreen extends ContainerScreen<LittleMaidContainer> {
    private static final ResourceLocation GUI =
            new ResourceLocation("lmreengaged", "textures/gui/container/littlemaidinventory2.png");
    private static final ResourceLocation ICONS = new ResourceLocation("textures/gui/icons.png");
    private static final ItemStack ARMOR = Items.DIAMOND_CHESTPLATE.getDefaultInstance();
    private final LittleMaidEntity openAt;
    private ITextComponent stateText;

    public LittleMaidScreen(LittleMaidContainer screenContainer, PlayerInventory inv, ITextComponent titleIn) {
        super(screenContainer, inv, titleIn);
        this.ySize = 208;
        this.openAt = screenContainer.getGuiEntity();
    }

    @Override
    protected void init() {
        super.init();
        if (openAt == null) {
            minecraft.displayGuiScreen(null);
            return;
        }
        int left = (int) ((this.width - xSize) / 2F);
        int top = (int) ((this.height - ySize) / 2F);
        this.addButton(new Button(left - 20, top + 20, 20, 20, "", button -> {
            openAt.setConfigHolder(LMConfigManager.INSTANCE.getAnyConfig());
            SyncSoundConfigPacket.sendC2SPacket(openAt, openAt.getConfigHolder().getName());
        }) {
            @Override
            public void renderButton(int mouseX, int mouseY, float partialTicks) {
                super.renderButton(mouseX, mouseY, partialTicks);
                String text = "S";
                font.drawStringWithShadow(text,
                        this.x + (this.width - font.getStringWidth(text)) / 2F,
                        this.y + (this.height - font.FONT_HEIGHT) / 2F, 0xFFFFFF);
                if (this.isHovered()) {
                    renderToolTip(mouseX, mouseY);
                }
            }

            @Override
            public void renderToolTip(int x, int y) {
                super.renderToolTip(x, y);
                String text = openAt.getConfigHolder().getName();
                float renderX = Math.max(0, x - font.getStringWidth(text) / 2F);
                font.drawStringWithShadow(text, renderX,
                        y - font.FONT_HEIGHT / 2F, 0xFFFFFF);
            }
        });
        this.addButton(new Button(left - 20, top + 40, 20, 20, "", button ->
                minecraft.displayGuiScreen(new ModelSelectScreen(title, openAt.world, (IHasMultiModel) openAt))) {
            @Override
            public void renderButton(int mouseX, int mouseY, float partialTicks) {
                super.renderButton(mouseX, mouseY, partialTicks);
                itemRenderer.renderItemIntoGUI(ARMOR, this.x - 8 + this.width / 2, this.y - 8 + this.height / 2);
            }
        });
        this.addButton(new Button(left - 20, top + 60, 20, 20, "",
                button -> {
                    openAt.changeMovingState();
                    stateText = getStateText();
                }) {
            @Override
            public void renderButton(int mouseX, int mouseY, float delta) {
                super.renderButton(mouseX, mouseY, delta);
                String state = openAt.getMovingState().substring(0, 2);
                font.drawStringWithShadow(state,
                        this.x + (this.width - font.getStringWidth(state)) / 2F,
                        this.y + (this.height - font.FONT_HEIGHT) / 2F, 0xFFFFFF);
            }
        });
        stateText = getStateText();
    }

    public ITextComponent getStateText() {
        TextComponent stateText = new TranslationTextComponent("state." + LittleMaidReBirthMod.MODID + "." + openAt.getMovingState());
        openAt.getModeName().ifPresent(
                modeName -> stateText.appendText(" : ")
                        .appendSibling(new TranslationTextComponent("mode." + LittleMaidReBirthMod.MODID + "." + modeName)));
        return stateText;
    }

    @Override
    public void tick() {
        super.tick();
        //少し重たいかもしれないが、screenを開く直前にsetModeNameした場合に取得がズレるので毎tickやる
        stateText = getStateText();
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        super.render(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
        InventoryScreen.drawEntityOnScreen(
                (this.width - this.xSize) / 2 + 52,
                (this.height - this.ySize) / 2 + 59,
                20,
                (this.width - this.xSize) / 2F + 52 - mouseX,
                (this.height - this.ySize) / 2F + 30 - mouseY, openAt);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        RenderSystem.disableBlend();
        this.font.drawString(this.stateText.getString(), 8F, 65F, 0x404040);
        String insideSkirt = new TranslationTextComponent("entity.littlemaidrebirth.little_maid_mob.InsideSkirt").getString();
        this.font.drawString(insideSkirt, 168F - font.getStringWidth(insideSkirt), 65F, 0x404040);
        float left = (width - xSize) / 2F;
        float top = (height - ySize) / 2F;
        if (left + 7 <= mouseX && mouseX < left + 96 && top + 7 <= mouseY && mouseY < top + 60) {
            drawArmor(mouseX, mouseY);
        } else {
            drawHealth(mouseX, mouseY);
        }
    }

    protected void drawHealth(int mouseX, int mouseY) {
        float left = (width - xSize) / 2F;
        float top = (height - ySize) / 2F;
        if (left + 98 <= mouseX && mouseX < left + 98 + 5 * 9 && top + 7 <= mouseY && mouseY < top + 7 + 2 * 9) {
            String healthStr = MathHelper.ceil(openAt.getHealth()) + " / " + MathHelper.ceil(openAt.getMaxHealth());
            this.font.drawString(healthStr,
                    98F + (5F * 9F - font.getStringWidth(healthStr)) / 2F,
                    16F - font.FONT_HEIGHT / 2F, 0x404040);
        } else {
            float health = (openAt.getHealth() / openAt.getMaxHealth()) * 20F;
            drawHealth(98, 7, MathHelper.clamp(health - 10, 0, 10), 5);
            drawHealth(98, 16, MathHelper.clamp(health, 0, 10), 5);
        }
        this.minecraft.getTextureManager().bindTexture(GUI);
    }

    protected void drawArmor(int mouseX, int mouseY) {
        float armor = openAt.getTotalArmorValue();
        drawArmor(98, 7, MathHelper.clamp(armor - 10, 0, 10), 5);
        drawArmor(98, 16, MathHelper.clamp(armor, 0, 10), 5);
    }

    protected void drawHealth(int x, int y, float health, int rowHeart) {
        drawIcon(x, y, health, rowHeart, 16, 0, 52, 0, 61, 0);
    }

    protected void drawArmor(int x, int y, float health, int rowHeart) {
        drawIcon(x, y, health, rowHeart, 16, 9, 34, 9, 25, 9);
    }

    protected void drawIcon(int x, int y, float num, int row,
                            int baseU, int baseV, int overU, int overV, int halfU, int halfV) {
        this.minecraft.getTextureManager().bindTexture(ICONS);
        for (int i = 0; i < row; i++) {
            this.blit(x + i * 9, y, baseU, baseV, 9, 9);
            if (1 < num) {
                this.blit(x + i * 9, y, overU, overV, 9, 9);
            } else if (0 < num) {
                this.blit(x + i * 9, y, halfU, halfV, 9, 9);
            }
            num -= 2;
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        assert this.minecraft != null;
        this.minecraft.getTextureManager().bindTexture(GUI);
        int relX = (this.width - this.xSize) / 2;
        int relY = (this.height - this.ySize) / 2;
        this.blit(relX, relY, 0, 0, this.xSize, this.ySize);
    }

    //Forgeが余計なことしてるっぽい？ので修正
    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    //なんでE押してインベントリ閉じたときにonClose動かないの！
    @Override
    public boolean keyPressed(int p_keyPressed_1_, int p_keyPressed_2_, int p_keyPressed_3_) {
        InputMappings.Input mouseKey = InputMappings.getInputByCode(p_keyPressed_1_, p_keyPressed_2_);
        if (this.minecraft.gameSettings.keyBindInventory.isActiveAndMatches(mouseKey)) {
            onClose();
            return true;
        }
        return super.keyPressed(p_keyPressed_1_, p_keyPressed_2_, p_keyPressed_3_);
    }

    @Override
    public void onClose() {
        super.onClose();
        SyncMovingStatePacket.sendC2SPacket(openAt, openAt.getMovingState());
    }
}
