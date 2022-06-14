package com.glisco.isometricrenders.widget;

import com.glisco.isometricrenders.property.Property;
import com.glisco.isometricrenders.util.Translate;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.function.Supplier;

public class WidgetColumnBuilder {

    private final WidgetConsumer consumer;
    private final int width;
    private final int baseX;

    private int currentY;
    private int currentRowHeight = 0;

    public WidgetColumnBuilder(WidgetConsumer consumer, int baseY, int baseX, int width) {
        this.consumer = consumer;
        this.currentY = baseY;
        this.baseX = baseX + 10;
        this.width = width;
    }

    public <T extends Element & Drawable & Selectable> T add(T widget, int widgetHeight) {
        this.consumer.accept(widget);
        if (widgetHeight > this.currentRowHeight) this.currentRowHeight = widgetHeight;
        return widget;
    }

    public LabelWidget label(String key) {
        final var label = this.add(new LabelWidget(baseX + 2, this.currentY(), Translate.gui(key)), MinecraftClient.getInstance().textRenderer.fontHeight);
        this.nextRow();
        return label;
    }

    public LabelWidget dynamicLabel(Supplier<Text> content) {
        final var label = this.add(new LabelWidget(baseX + 2, this.currentY(), content), MinecraftClient.getInstance().textRenderer.fontHeight);
        this.nextRow();
        return label;
    }

    public IsometricButtonWidget button(String messageKey, int xOffset, int width, ButtonWidget.PressAction onPress) {
        return this.add(new IsometricButtonWidget(baseX + xOffset, this.currentY(), width, 20, Translate.gui(messageKey), onPress), 20);
    }

    public TextFieldWidget labeledTextField(String content, int width, String labelKey) {
        final var textField = this.add(
                new TextFieldWidget(
                        MinecraftClient.getInstance().textRenderer,
                        this.baseX,
                        this.currentY(),
                        width,
                        20,
                        Text.empty()
                ),
                20
        );
        textField.setText(content);

        this.add(new LabelWidget(baseX + width + 5, this.currentY + 6, Translate.gui(labelKey)), MinecraftClient.getInstance().textRenderer.fontHeight)
                .color(0xAAAAAA).shadow(false);

        this.nextRow();
        return textField;
    }

    public PropertyCheckbox propertyCheckbox(Property<Boolean> property, String key) {
        final var checkbox = this.add(new PropertyCheckbox(this.baseX, this.currentY, Translate.gui(key), property), 20);
        this.nextRow();
        return checkbox;
    }

    public void nextRow() {
        this.move(this.currentRowHeight + 10);
        this.currentRowHeight = 0;
    }

    public void move(int pixels) {
        this.currentY += pixels;
    }

    public int currentY() {
        return this.currentY;
    }

    public int width() {
        return this.width;
    }

    public interface WidgetConsumer {
        <T extends Element & Drawable & Selectable> void accept(T widget);
    }
}
