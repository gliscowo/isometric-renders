package com.glisco.isometricrenders.command;

import com.glisco.isometricrenders.IsometricRenders;
import com.glisco.isometricrenders.mixin.access.BlockStateArgumentAccessor;
import com.glisco.isometricrenders.mixin.access.DefaultPosArgumentAccessor;
import com.glisco.isometricrenders.mixin.access.EntitySummonArgumentTypeInvoker;
import com.glisco.isometricrenders.property.GlobalProperties;
import com.glisco.isometricrenders.render.*;
import com.glisco.isometricrenders.screen.RenderScreen;
import com.glisco.isometricrenders.screen.ScreenScheduler;
import com.glisco.isometricrenders.util.AreaSelectionHelper;
import com.glisco.isometricrenders.util.Translate;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.*;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntry;

import java.util.List;
import java.util.function.BiConsumer;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class IsorenderCommand {

    private static final SuggestionProvider<FabricClientCommandSource> CLIENT_SUMMONABLE_ENTITIES = (context, builder) -> CommandSource.suggestFromIdentifier(Registry.ENTITY_TYPE.stream().filter(EntityType::isSummonable),
            builder, EntityType::getId, entityType -> Text.translatable(Util.createTranslationKey("entity", EntityType.getId(entityType)))
    );

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess access) {
        dispatcher.register(literal("isorender")
                .executes(IsorenderCommand::showRootNodeHelp)
                .then(literal("area")
                        .executes(IsorenderCommand::renderAreaSelection)
                        .then(argument("start", BlockPosArgumentType.blockPos())
                                .then(argument("end", BlockPosArgumentType.blockPos())
                                        .executes(IsorenderCommand::renderAreaWithArguments))))
                .then(literal("block")
                        .executes(IsorenderCommand::renderTargetedBlock)
                        .then(argument("block", BlockStateArgumentType.blockState(access))
                                .executes(IsorenderCommand::renderBlockWithArgument)))
                .then(literal("entity")
                        .executes(IsorenderCommand::renderTargetedEntity)
                        .then(argument("entity", EntitySummonArgumentType.entitySummon())
                                .suggests(CLIENT_SUMMONABLE_ENTITIES)
                                .executes(IsorenderCommand::renderEntityWithoutNbt)
                                .then(argument("nbt", NbtCompoundArgumentType.nbtCompound())
                                        .executes(IsorenderCommand::renderEntityWithNbt))))
                .then(literal("item")
                        .executes(IsorenderCommand::renderHeldItem)
                        .then(argument("item", ItemStackArgumentType.itemStack(access))
                                .executes(IsorenderCommand::renderItemWithArgument)))
                .then(literal("tooltip")
                        .executes(IsorenderCommand::renderHeldItemTooltip)
                        .then(argument("item", ItemStackArgumentType.itemStack(access))
                                .executes(IsorenderCommand::renderItemTooltipWithArgument)))
                .then(literal("namespace")
                        .then(argument("namespace", NamespaceArgumentType.namespace())
                                .then(argument("task", new RenderTaskArgumentType())
                                        .executes(IsorenderCommand::renderNamespace))))
                .then(literal("creative_tab")
                        .then(argument("itemgroup", ItemGroupArgumentType.itemGroup())
                                .then(argument("task", new RenderTaskArgumentType())
                                        .executes(IsorenderCommand::renderCreativeTab))))
                .then(literal("tag")
                        .then(argument("tag", new TagArgumentType(access))
                                .then(argument("task", new RenderTaskArgumentType())
                                        .executes(IsorenderCommand::renderTagContents))))
                .then(literal("unsafe")
                        .then(literal("enable")
                                .executes(IsorenderCommand::enableUnsafe))
                        .then(literal("disable")
                                .executes(IsorenderCommand::disableUnsafe))));
    }

    private static int showRootNodeHelp(CommandContext<FabricClientCommandSource> context) {
        final var source = context.getSource();

        source.sendFeedback(Translate.prefixed(Translate.make("version", Text.literal(IsometricRenders.VERSION).formatted(Formatting.DARK_GRAY)).formatted(Formatting.GRAY)));
        source.sendFeedback(Translate.prefixed(Translate.make("command_hint").styled(
                style -> style
                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://docs.wispforest.io/isometric-renders/slash_isorender/"))
                        .withFormatting(Formatting.UNDERLINE)
                        .withFormatting(Formatting.GRAY)
        )));
        return 0;
    }

    private static int disableUnsafe(CommandContext<FabricClientCommandSource> context) {
        GlobalProperties.unsafe.set(false);
        Translate.commandFeedback(context, "unsafe_disabled");
        return 0;
    }

    private static int enableUnsafe(CommandContext<FabricClientCommandSource> context) {
        GlobalProperties.unsafe.set(true);
        Translate.commandFeedback(context, "unsafe_enabled");
        return 0;
    }

    private static int renderTagContents(CommandContext<FabricClientCommandSource> context) {
        final var tag = TagArgumentType.getTag("tag", context);
        RenderTaskArgumentType.getTask("task", context).action.accept(
                "tag_" + tag.id().getNamespace() + "/" + tag.id().getPath(),
                tag.entries().stream()
                        .map(RegistryEntry::value)
                        .map(Item::getDefaultStack)
                        .toList()
        );
        return 0;
    }

    private static int renderCreativeTab(CommandContext<FabricClientCommandSource> context) {
        final var task = RenderTaskArgumentType.getTask("task", context);
        withItemGroupFromContext(context, (itemStacks, name) -> {
            task.action.accept(name, itemStacks);
        });
        return 0;
    }

    private static int renderNamespace(CommandContext<FabricClientCommandSource> context) {
        final var namespace = NamespaceArgumentType.getNamespace("namespace", context);
        RenderTaskArgumentType.getTask("task", context).action.accept("namespace_" + namespace.name(), namespace.getContent());
        return 0;
    }

    private static int renderItemWithArgument(CommandContext<FabricClientCommandSource> context) throws CommandSyntaxException {
        ScreenScheduler.schedule(new RenderScreen(
                new ItemRenderable(ItemStackArgumentType.getItemStackArgument(context, "item").createStack(1, false))
        ));
        return 0;
    }

    private static int renderHeldItem(CommandContext<FabricClientCommandSource> context) {
        ScreenScheduler.schedule(new RenderScreen(
                new ItemRenderable(MinecraftClient.getInstance().player.getMainHandStack())
        ));
        return 0;
    }

    private static int renderItemTooltipWithArgument(CommandContext<FabricClientCommandSource> context) throws CommandSyntaxException {
        ScreenScheduler.schedule(new RenderScreen(
                new TooltipRenderable(ItemStackArgumentType.getItemStackArgument(context, "item").createStack(1, false))
        ));
        return 0;
    }

    private static int renderHeldItemTooltip(CommandContext<FabricClientCommandSource> context) {
        ScreenScheduler.schedule(new RenderScreen(
                new TooltipRenderable(MinecraftClient.getInstance().player.getMainHandStack())
        ));
        return 0;
    }

    private static int renderEntityWithNbt(CommandContext<FabricClientCommandSource> context) {
        final var entityNbt = NbtCompoundArgumentType.getNbtCompound(context, "nbt");
        final var entityId = EntitySummonArgumentTypeInvoker.isometric$validate(
                context.getArgument("entity", Identifier.class)
        );

        ScreenScheduler.schedule(new RenderScreen(
                EntityRenderable.of(Registry.ENTITY_TYPE.get(entityId), entityNbt)
        ));

        return 0;
    }

    private static int renderEntityWithoutNbt(CommandContext<FabricClientCommandSource> context) {
        final var entityId = EntitySummonArgumentTypeInvoker.isometric$validate(
                context.getArgument("entity", Identifier.class)
        );

        ScreenScheduler.schedule(new RenderScreen(
                EntityRenderable.of(Registry.ENTITY_TYPE.get(entityId), null)
        ));

        return 0;
    }

    private static int renderTargetedEntity(CommandContext<FabricClientCommandSource> context) {
        final var client = MinecraftClient.getInstance();

        if (client.crosshairTarget.getType() != HitResult.Type.ENTITY) {
            Translate.commandError(context, "no_entity");
            return 0;
        }

        final var targetEntity = ((EntityHitResult) client.crosshairTarget).getEntity();
        ScreenScheduler.schedule(new RenderScreen(
                EntityRenderable.copyOf(targetEntity)
        ));

        return 0;
    }

    private static int renderBlockWithArgument(CommandContext<FabricClientCommandSource> context) {
        final var stateArg = context.getArgument("block", BlockStateArgument.class);
        final var state = stateArg.getBlockState();
        final var data = ((BlockStateArgumentAccessor) stateArg).isometric$getData();

        ScreenScheduler.schedule(new RenderScreen(
                BlockStateRenderable.of(state, data)
        ));
        return 0;
    }

    private static int renderTargetedBlock(CommandContext<FabricClientCommandSource> context) {
        final var client = MinecraftClient.getInstance();

        if (client.crosshairTarget.getType() != HitResult.Type.BLOCK) {
            Translate.commandError(context, "no_block");
            return 0;
        }

        final var hitPos = ((BlockHitResult) client.crosshairTarget).getBlockPos();
        ScreenScheduler.schedule(new RenderScreen(
                BlockStateRenderable.copyOf(client.world, hitPos)
        ));

        return 0;
    }

    private static int renderAreaWithArguments(CommandContext<FabricClientCommandSource> context) {
        final var startArg = context.getArgument("start", DefaultPosArgument.class);
        final var endArg = context.getArgument("end", DefaultPosArgument.class);

        final var pos1 = getPosFromArgument(startArg, context.getSource());
        final var pos2 = getPosFromArgument(endArg, context.getSource());

        ScreenScheduler.schedule(new RenderScreen(
                AreaRenderable.of(pos1, pos2)
        ));

        return 0;
    }

    private static int renderAreaSelection(CommandContext<FabricClientCommandSource> context) {
        if (!AreaSelectionHelper.tryOpenScreen()) {
            Translate.commandError(context, "incomplete_selection");
        }

        return 0;
    }

    private static <S> void withItemGroupFromContext(CommandContext<S> context, BiConsumer<List<ItemStack>, String> action) {
        final var itemGroup = ItemGroupArgumentType.getItemGroup("itemgroup", context);
        final var stacks = DefaultedList.<ItemStack>of();
        itemGroup.appendStacks(stacks);
        action.accept(stacks, "creative-tab_" + itemGroup.getName());
    }

    public static BlockPos getPosFromArgument(DefaultPosArgument argument, FabricClientCommandSource source) {

        DefaultPosArgumentAccessor accessor = (DefaultPosArgumentAccessor) argument;
        Vec3d pos = source.getPlayer().getPos();

        return new BlockPos(accessor.isometric$getX().toAbsoluteCoordinate(pos.x), accessor.isometric$getY().toAbsoluteCoordinate(pos.y), accessor.isometric$getZ().toAbsoluteCoordinate(pos.z));
    }
}
