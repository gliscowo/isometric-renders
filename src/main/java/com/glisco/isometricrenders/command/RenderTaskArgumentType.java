package com.glisco.isometricrenders.command;

import com.glisco.isometricrenders.util.RenderTask;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

import java.util.concurrent.CompletableFuture;

public class RenderTaskArgumentType implements ArgumentType<RenderTask> {

    private final SimpleCommandExceptionType EXCEPTION = new SimpleCommandExceptionType(Text.of("mald about it, see if anybody notices"));

    public static <S> RenderTask getTask(String name, CommandContext<S> context) {
        return context.getArgument(name, RenderTask.class);
    }

    @Override
    public RenderTask parse(StringReader reader) throws CommandSyntaxException {
        final var first = reader.readString();
        if (first.equals("atlas")) return RenderTask.ATLAS;

        if (first.equals("batch")) {
            reader.expect(' ');
            final var second = reader.readString();

            if (second.equals("items")) return RenderTask.BATCH_ITEM;
            if (second.equals("blocks")) return RenderTask.BATCH_BLOCK;
        }

        throw EXCEPTION.create();
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        final var input = builder.getRemaining();

        if (input.codePoints().filter(value -> value == ' ').count() > 0) {
            return CommandSource.suggestMatching(new String[]{"items", "blocks"}, builder.createOffset(builder.getStart() + input.length()));
        } else {
            return CommandSource.suggestMatching(new String[]{"atlas", "batch"}, builder);
        }
    }

}
