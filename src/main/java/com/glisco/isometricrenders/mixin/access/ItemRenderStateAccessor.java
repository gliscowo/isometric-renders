package com.glisco.isometricrenders.mixin.access;

import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemRenderState.class)
public interface ItemRenderStateAccessor {

    @Accessor("displayContext")
    void isometric$setDisplayContext(ItemDisplayContext ctx);
}
