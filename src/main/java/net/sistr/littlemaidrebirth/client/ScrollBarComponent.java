package net.sistr.littlemaidrebirth.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.AbstractGui;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Optional;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public class ScrollBarComponent extends AbstractGui {
    private final Supplier<Float> left;
    private final Supplier<Float> top;
    private final Supplier<Float> width;
    private final Supplier<Float> height;

    public ScrollBarComponent(Supplier<Float> left, Supplier<Float> top, Supplier<Float> width, Supplier<Float> height) {
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
    }

    public void renderScrollBar(MatrixStack matrices, float percent) {
        fill(matrices, left.get(), top.get(), left.get() + width.get(), top.get() + height.get(), 0xFF000000);
        fill(matrices, left.get(), top.get(), left.get() + width.get(), top.get() + height.get() * percent, 0xFFFFFFFF);
    }

    public Optional<Float> click(double x, double y) {
        if (left.get() < x && x < left.get() + width.get()) {
            float percent = (float) ((y - top.get()) / height.get());
            if (0 <= percent && percent <= 1F) return Optional.of(percent);
        }
        return Optional.empty();
    }

    public static void fill(MatrixStack matrices, float x1, float y1, float x2, float y2, int color) {
        fill(matrices, (int) x1, (int) y1, (int) x2, (int) y2, color);
    }

}
