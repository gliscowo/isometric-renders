package com.glisco.isometricrenders.mixin.access;

import net.minecraft.command.argument.BlockStateArgument;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockStateArgument.class)
public interface BlockStateArgumentAccessor {

    @Accessor
    NbtCompound getData();

}
