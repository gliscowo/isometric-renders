package com.glisco.isometricrenders.widget;

import com.glisco.isometricrenders.screen.IsometricUI;
import com.glisco.isometricrenders.util.ImageIO;
import io.wispforest.owo.ui.container.VerticalFlowLayout;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import net.minecraft.client.util.math.MatrixStack;

public class IOStateComponent extends VerticalFlowLayout {

    public IOStateComponent() {
        super(Sizing.content(), Sizing.content());

        this.padding(Insets.of(10));
        this.surface(Surface.flat(0x77000000).and(Surface.outline(0x77000000)));

        this.child(new DynamicLabelComponent(ImageIO::progressText).margins(Insets.bottom(10)));
    }

    @Override
    public void draw(MatrixStack matrices, int mouseX, int mouseY, float partialTicks, float delta) {
        super.draw(matrices, mouseX, mouseY, partialTicks, delta);
        IsometricUI.drawExportProgressBar(matrices, this.x + 10, this.y + 25, 100, 50, 10);
    }
}
