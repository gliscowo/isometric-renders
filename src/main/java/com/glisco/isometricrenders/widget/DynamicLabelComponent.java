package com.glisco.isometricrenders.widget;

import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.util.function.Supplier;

public class DynamicLabelComponent extends BaseComponent {

    private final TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

    private final Supplier<Text> content;
    private int color = 0xFFFFFF;
    private boolean shadow = true;

    public DynamicLabelComponent(Supplier<Text> content) {
        this.content = content;
    }

    public DynamicLabelComponent color(int color) {
        this.color = color;
        return this;
    }

    public DynamicLabelComponent shadow(boolean shadow) {
        this.shadow = shadow;
        return this;
    }

    @Override
    protected int determineHorizontalContentSize(Sizing sizing) {
        this.width = 100;
        return 100;
    }

    @Override
    protected int determineVerticalContentSize(Sizing sizing) {
        this.height = this.textRenderer.fontHeight;
        return this.height;
    }

    @Override
    public void draw(MatrixStack matrices, int mouseX, int mouseY, float partialTicks, float delta) {
        if (this.shadow) {
            this.textRenderer.drawWithShadow(matrices, this.content.get(), this.x, this.y, this.color);
        } else {
            this.textRenderer.draw(matrices, this.content.get(), this.x, this.y, this.color);
        }
    }


}
