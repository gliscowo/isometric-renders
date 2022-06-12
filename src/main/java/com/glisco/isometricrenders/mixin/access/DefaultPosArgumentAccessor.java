package com.glisco.isometricrenders.mixin.access;

import net.minecraft.command.argument.CoordinateArgument;
import net.minecraft.command.argument.DefaultPosArgument;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DefaultPosArgument.class)
public interface DefaultPosArgumentAccessor {

    @Accessor("x")
    CoordinateArgument isometric$getX();

    @Accessor("y")
    CoordinateArgument isometric$getY();

    @Accessor("z")
    CoordinateArgument isometric$getZ();

}
