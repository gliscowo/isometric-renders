package com.glisco.isometricrenders.widget;

import com.glisco.isometricrenders.util.Translate;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.function.Supplier;

import static com.glisco.isometricrenders.util.AreaSelectionHelper.pos1;
import static com.glisco.isometricrenders.util.AreaSelectionHelper.pos2;

public class AreaSelectionComponent extends FlowLayout {

    public AreaSelectionComponent() {
        super(Sizing.content(), Sizing.content(), Algorithm.VERTICAL);

        this.surface(Surface.flat(0x77000000).and(Surface.outline(0x77000000)));
        this.padding(Insets.of(5));

        this.child(Components.label(Translate.PREFIX).shadow(true).margins(Insets.bottom(10)));

        this.child(Components.label(Translate.gui("hud.area_selection")));
        this.child(new DynamicLabelComponent(
                positionText(() -> pos1, "from"))
                .shadow(false).horizontalSizing(Sizing.fixed(MinecraftClient.getInstance().textRenderer.getWidth(positionText(() -> pos1, "from").get())))
        );
        this.child(new DynamicLabelComponent(
                positionText(() -> pos2, "to"))
                .shadow(false).margins(Insets.bottom(10))
        );

        this.child(Components.label(Translate.gui("hud.area_selection.clear_hint")));
    }

    private static Supplier<Text> positionText(Supplier<BlockPos> pos, String name) {
        return () -> Translate.gui("hud.area_selection." + name, pos.get() == null ? "---" : pos.get().getX() + " " + pos.get().getY() + " " + pos.get().getZ());
    }

}
