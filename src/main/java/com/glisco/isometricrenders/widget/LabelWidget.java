package com.glisco.isometricrenders.widget;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.util.function.Function;
import java.util.function.Supplier;

public class LabelWidget implements Drawable, Element, Selectable {

    private final TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

    private Function<LabelWidget, Boolean> clickAction = labelWidget -> false;
    private final Supplier<Text> content;
    private int color = 0xFFFFFF;
    private boolean shadow = true;

    public int x, y;
    public final int width, height;

    public LabelWidget(int x, int y, Text content) {
        this.content = () -> content;
        this.x = x;
        this.y = y;

        this.width = textRenderer.getWidth(content);
        this.height = textRenderer.fontHeight;
    }

    public LabelWidget(int x, int y, Supplier<Text> content) {
        this.content = content;
        this.x = x;
        this.y = y;

        this.width = textRenderer.getWidth(content.get());
        this.height = textRenderer.fontHeight;
    }

    public LabelWidget color(int color) {
        this.color = color;
        return this;
    }

    public LabelWidget shadow(boolean shadow) {
        this.shadow = shadow;
        return this;
    }

    public LabelWidget clickAction(Function<LabelWidget, Boolean> clickAction) {
        this.clickAction = clickAction;
        return this;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (this.shadow) {
            this.textRenderer.drawWithShadow(matrices, this.content.get(), this.x, this.y, this.color);
        } else {
            this.textRenderer.draw(matrices, this.content.get(), this.x, this.y, this.color);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return this.isMouseOver(mouseX, mouseY) && this.clickAction.apply(this);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + this.height;
    }

    @Override
    public SelectionType getType() {
        return SelectionType.NONE;
    }

    @Override
    public void appendNarrations(NarrationMessageBuilder builder) {
        builder.put(NarrationPart.TITLE, this.content.get());
    }

}
