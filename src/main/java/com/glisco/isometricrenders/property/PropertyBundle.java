package com.glisco.isometricrenders.property;

import com.glisco.isometricrenders.render.Renderable;
import com.glisco.isometricrenders.util.Translate;
import com.glisco.isometricrenders.widget.PropertySliderWidget;
import com.glisco.isometricrenders.widget.PropertyTextField;
import com.glisco.isometricrenders.widget.WidgetColumnBuilder;
import net.minecraft.client.util.math.MatrixStack;

public interface PropertyBundle {

    void buildGuiControls(Renderable<?> renderable, WidgetColumnBuilder builder);

    void applyToViewMatrix(MatrixStack modelViewStack);

    default void appendIntControls(WidgetColumnBuilder builder, IntProperty property, String name, int step) {
        final var textField = new PropertyTextField(10, builder.currentY(), property);
        builder.add(textField, textField.getHeight());
        final var slider = new PropertySliderWidget(50, builder.currentY(), builder.width() - 55, Translate.gui(name), step, property);
        builder.add(slider, slider.getHeight());

        builder.nextRow();
    }

}
