package com.glisco.isometricrenders.util;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.item.ItemGroup;

import java.util.Arrays;

public class ItemGroupArgumentType implements ArgumentType<ItemGroup> {

    DynamicCommandExceptionType NO_ITEMGROUP = new DynamicCommandExceptionType(o -> () -> "No such item group: " + o);

    private ItemGroupArgumentType() {}

    public static ItemGroupArgumentType itemGroup() {
        return new ItemGroupArgumentType();
    }

    @Override
    public ItemGroup parse(StringReader reader) throws CommandSyntaxException {
        String name = reader.readString();
        return Arrays.stream(ItemGroup.GROUPS).filter(itemGroup -> itemGroup.getName().equals(name)).findAny().orElseThrow(() -> NO_ITEMGROUP.create(name));
    }
}
