package com.glisco.isometricrenders.mixin.access;

import net.minecraft.command.argument.EntitySummonArgumentType;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(EntitySummonArgumentType.class)
public interface EntitySummonArgumentTypeAccessor {

    @Invoker
    static Identifier invokeValidate(Identifier identifier) {
        throw new AssertionError();
    }

}
