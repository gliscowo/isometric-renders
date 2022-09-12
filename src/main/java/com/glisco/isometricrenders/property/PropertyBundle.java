package com.glisco.isometricrenders.property;

import com.glisco.isometricrenders.render.Renderable;
import io.wispforest.owo.ui.container.FlowLayout;
import net.minecraft.client.util.math.MatrixStack;

public interface PropertyBundle {

    void buildGuiControls(Renderable<?> renderable, FlowLayout container);

    void applyToViewMatrix(MatrixStack modelViewStack);
}
