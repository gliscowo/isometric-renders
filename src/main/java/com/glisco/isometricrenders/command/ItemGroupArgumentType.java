package com.glisco.isometricrenders.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.concurrent.CompletableFuture;

public class ItemGroupArgumentType implements ArgumentType<ItemGroup> {

    private static final DynamicCommandExceptionType NO_ITEMGROUP = new DynamicCommandExceptionType(o -> () -> "No such item group: " + o);

    private ItemGroupArgumentType() {}

    public static ItemGroupArgumentType itemGroup() {
        return new ItemGroupArgumentType();
    }

    public static <S> ItemGroup getItemGroup(String name, CommandContext<S> context) {
        return context.getArgument(name, ItemGroup.class);
    }

    @Override
    public ItemGroup parse(StringReader reader) throws CommandSyntaxException {
        var id = Identifier.fromCommandInput(reader);
        return ItemGroups.getGroups().stream().filter(itemGroup -> Registries.ITEM_GROUP.getId(itemGroup).equals(id)).findAny().orElseThrow(() -> NO_ITEMGROUP.create(id));
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return CommandSource.suggestIdentifiers(ItemGroups.getGroups().stream().map(Registries.ITEM_GROUP::getId), builder);
    }
}
