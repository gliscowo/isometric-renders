package com.glisco.isometricrenders.mixin.access;

import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ButtonWidget.class)
public interface ButtonWidgetAccessor {

    @Mutable
    @Accessor("tooltipSupplier")
    void isometric$setTooltipSupplier(ButtonWidget.TooltipSupplier supplier);

}
