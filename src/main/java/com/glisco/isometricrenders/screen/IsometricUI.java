package com.glisco.isometricrenders.screen;

import com.glisco.isometricrenders.property.IntProperty;
import com.glisco.isometricrenders.property.Property;
import com.glisco.isometricrenders.util.Translate;
import com.glisco.isometricrenders.widget.DynamicLabelComponent;
import com.glisco.isometricrenders.widget.PropertyCheckboxComponent;
import com.glisco.isometricrenders.widget.PropertySliderComponent;
import com.glisco.isometricrenders.widget.PropertyTextFieldComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.VerticalAlignment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.function.Supplier;

public class IsometricUI {

    public static TextFieldWidget labelledTextField(FlowLayout container, String content, String key, Sizing sizing) {
        try (var builder = row(container)) {
            final var textBox = Components.textBox(sizing, content);

            builder.row.child(textBox);
            builder.row.child(Components.label(Translate.gui(key)).margins(Insets.left(8)));

            return textBox;
        }
    }

    public static LabelComponent sectionHeader(FlowLayout container, String key, boolean separate) {
        final var label = Components.label(Translate.gui(key)).shadow(true);
        if (separate) label.margins(Insets.top(20));
        label.margins(label.margins().get().withBottom(5));

        container.child(label);
        return label;
    }

    public static DynamicLabelComponent dynamicLabel(FlowLayout container, Supplier<Text> content) {
        final var label = new DynamicLabelComponent(content).shadow(false);
        label.margins(Insets.bottom(5));

        container.child(label);
        return label;
    }

    public static void booleanControl(FlowLayout container, Property<Boolean> property, String key) {
        container.child(new PropertyCheckboxComponent(Translate.gui(key), property).margins(Insets.top(5)));
    }

    public static void intControl(FlowLayout container, IntProperty property, String name, int step) {
        try (var builder = row(container)) {
            builder.row.child(new PropertyTextFieldComponent(Sizing.fill(15), property));
            builder.row.child(new PropertySliderComponent(Sizing.fill(80), Translate.gui(name), step, property).margins(Insets.left(5)));
        }
    }

    public static void drawExportProgressBar(DrawContext context, int x, int y, int drawWidth, int barWidth, double speed) {
        int end = x + drawWidth + barWidth;

        int offset = (int) (System.currentTimeMillis() / speed % (drawWidth + barWidth));

        int endWithOffset = x + offset;
        if (endWithOffset > end) endWithOffset = end;

        context.fill(Math.max(x + offset - barWidth, x), y, Math.min(endWithOffset, x + drawWidth), y + 2, 0xFF00FF00);
    }

    public static RowBuilder row(FlowLayout container) {
        var component = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        component.margins(Insets.vertical(5)).verticalAlignment(VerticalAlignment.CENTER);
        return new RowBuilder(component, container);
    }

    public static class RowBuilder implements AutoCloseable {

        public final FlowLayout row;
        private final FlowLayout container;

        private RowBuilder(FlowLayout row, FlowLayout container) {
            this.row = row;
            this.container = container;
        }

        @Override
        public void close() {
            this.container.child(this.row);
        }
    }
}
