package com.glisco.isometricrenders.command;

import com.glisco.isometricrenders.util.Translate;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class NamespaceArgumentType implements ArgumentType<NamespaceArgumentType.Namespace> {

    private static final SimpleCommandExceptionType NO_SUCH_NAMESPACE = new SimpleCommandExceptionType(Translate.msg("no_such_namespace"));

    public static NamespaceArgumentType namespace() {
        return new NamespaceArgumentType();
    }

    public static <S> Namespace getNamespace(String name, CommandContext<S> context) {
        return context.getArgument(name, Namespace.class);
    }

    @Override
    public Namespace parse(StringReader reader) throws CommandSyntaxException {
        final var input = reader.readString();
        final var namespaces = getNamespaces();

        if (!namespaces.contains(input)) {
            throw NO_SUCH_NAMESPACE.create();
        }

        return new Namespace(input);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return CommandSource.suggestMatching(getNamespaces(), builder);
    }

    private Set<String> getNamespaces() {
        final var set = new HashSet<String>();
        for (var id : Registries.ITEM.getIds()) {
            set.add(id.getNamespace());
        }
        return set;
    }

    public record Namespace(String name) {
        public List<ItemStack> getContent() {
            return Registries.ITEM.streamEntries()
                    .filter(entry -> Objects.equals(entry.registryKey().getValue().getNamespace(), this.name))
                    .map(RegistryEntry.Reference::value)
                    .map(Item::getDefaultStack)
                    .toList();
        }
    }
}
