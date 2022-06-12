package com.glisco.isometricrenders.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandRegistryWrapper;
import net.minecraft.command.CommandSource;
import net.minecraft.item.Item;
import net.minecraft.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntryList;

import java.util.concurrent.CompletableFuture;

public class TagArgumentType implements ArgumentType<TagArgumentType.TagArgument> {

    private static final DynamicCommandExceptionType UNKNOWN_TAG_EXCEPTION = new DynamicCommandExceptionType(
            tag -> Text.translatable("arguments.item.tag.unknown", tag)
    );

    private final CommandRegistryWrapper<Item> registryWrapper;

    public TagArgumentType(CommandRegistryAccess registryAccess) {
        this.registryWrapper = registryAccess.createWrapper(Registry.ITEM_KEY);
    }

    public static <S> TagArgument getTag(String name, CommandContext<S> context) {
        return context.getArgument(name, TagArgument.class);
    }

    @Override
    public TagArgument parse(StringReader reader) throws CommandSyntaxException {
        reader.expect('#');
        final var tagId = Identifier.fromCommandInput(reader);
        return registryWrapper.getEntryList(TagKey.of(Registry.ITEM_KEY, tagId))
                .map(entryList -> new TagArgument(tagId, entryList))
                .orElseThrow(() -> UNKNOWN_TAG_EXCEPTION.createWithContext(reader, tagId));
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return CommandSource.suggestIdentifiers(this.registryWrapper.streamTags().map(TagKey::id), builder, String.valueOf('#'));
    }

    public record TagArgument(Identifier id, RegistryEntryList<Item> entries) {}
}
