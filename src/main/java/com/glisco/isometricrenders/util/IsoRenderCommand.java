package com.glisco.isometricrenders.util;

import com.glisco.isometricrenders.mixin.access.BlockStateArgumentAccessor;
import com.glisco.isometricrenders.mixin.access.EntitySummonArgumentTypeAccessor;
import com.glisco.isometricrenders.render.DefaultLightingProfiles;
import com.glisco.isometricrenders.render.IsometricRenderHelper;
import com.glisco.isometricrenders.render.IsometricRenderPresets;
import com.glisco.isometricrenders.screen.AreaIsometricRenderScreen;
import com.glisco.isometricrenders.screen.IsometricRenderScreen;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static com.glisco.isometricrenders.util.Translate.msg;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class IsoRenderCommand {

    private static final SuggestionProvider<FabricClientCommandSource> CLIENT_SUMMONABLE_ENTITIES;
    private static final SuggestionProvider<FabricClientCommandSource> ITEM_GROUPS;
    private static final SuggestionProvider<FabricClientCommandSource> NAMESPACE_PROVIDER;

    private static final List<String> NAMESPACES = new ArrayList<>();

    private static final ArgKey<ItemGroup> ITEMGROUP = new ArgKey<>(ItemGroup.class, "itemgroup");
    private static final ArgKey<String> NAMESPACE = new ArgKey<>(String.class, "namespace");

    static {
        CLIENT_SUMMONABLE_ENTITIES = (context, builder) -> CommandSource.suggestFromIdentifier(Registry.ENTITY_TYPE.stream().filter(EntityType::isSummonable),
                builder, EntityType::getId, entityType -> Text.translatable(Util.createTranslationKey("entity", EntityType.getId(entityType)))
        );

        ITEM_GROUPS = (context, builder) ->
                CommandSource.suggestMatching(Arrays.stream(ItemGroup.GROUPS).map(ItemGroup::getName), builder);

        NAMESPACE_PROVIDER = (context, builder) -> {
            cacheNamespaces();
            return CommandSource.suggestMatching(NAMESPACES, builder);
        };
    }

    private static void cacheNamespaces() {
        if (!NAMESPACES.isEmpty()) return;

        Registry.ITEM.forEach(item -> {
            final String namespace = Registry.ITEM.getId(item).getNamespace();
            if (NAMESPACES.contains(namespace)) return;
            NAMESPACES.add(namespace);
        });
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess access) {
        dispatcher.register(literal("isorender").then(literal("block").executes(context -> executeBlockTarget(context.getSource())).then(argument("block", BlockStateArgumentType.blockState(access)).executes(context -> {
            final BlockStateArgument blockStateArgument = context.getArgument("block", BlockStateArgument.class);
            final BlockState blockState = blockStateArgument.getBlockState();
            return executeBlockState(context.getSource(), blockState, ((BlockStateArgumentAccessor) blockStateArgument).getData());
        }))).then(literal("item").executes(context -> {
            return executeItem(context.getSource(), context.getSource().getPlayer().getMainHandStack().copy());
        }).then(argument("item", ItemStackArgumentType.itemStack(access)).executes(context -> {
            final ItemStack stack = ItemStackArgumentType.getItemStackArgument(context, "item").createStack(1, false);
            return executeItem(context.getSource(), stack);
        }))).then(literal("entity").executes(context -> {
            return executeEntityTarget(context.getSource());
        }).then(argument("entity", EntitySummonArgumentType.entitySummon()).suggests(CLIENT_SUMMONABLE_ENTITIES).executes(context -> {
            Identifier id = context.getArgument("entity", Identifier.class);
            return executeEntity(context.getSource(), EntitySummonArgumentTypeAccessor.invokeValidate(id), new NbtCompound());
        }).then(argument("nbt", NbtCompoundArgumentType.nbtCompound()).executes(context -> {
            Identifier id = context.getArgument("entity", Identifier.class);
            return executeEntity(context.getSource(), EntitySummonArgumentTypeAccessor.invokeValidate(id), context.getArgument("nbt", NbtCompound.class));
        })))).then(literal("insanity").executes(context -> {
            Translate.commandFeedback(context, RuntimeConfig.toggleInsaneResolutions() ? "insane_resolution_unlocked" : "insane_resolution_locked");
            return 0;
        })).then(literal("area").executes(context -> {
            if (AreaSelectionHelper.tryOpenScreen()) return 0;
            Translate.commandError(context, "incomplete_selection");
            return 0;
        }).then(argument("start", BlockPosArgumentType.blockPos()).then(argument("end", BlockPosArgumentType.blockPos()).executes(context -> {
            return executeArea(context, false);
        }).then(literal("enable_translucency").executes(context -> {
            return executeArea(context, true);
        }))))).then(literal("creative_tab").then(ITEMGROUP.toNode(ItemGroupArgumentType.itemGroup()).suggests(ITEM_GROUPS).then(literal("batch").then(literal("blocks").executes(context -> {
            IsometricRenderHelper.batchRenderItemGroupBlocks(ITEMGROUP.get(context));
            return 0;
        })).then(literal("items").executes(context -> {
            IsometricRenderHelper.batchRenderItemGroupItems(ITEMGROUP.get(context));
            return 0;
        }))).then(literal("atlas").executes(context -> {
            IsometricRenderHelper.renderItemGroupAtlas(ITEMGROUP.get(context));
            return 0;
        })))).then(literal("lighting").executes(context -> {
            if (RuntimeConfig.lightingProfile instanceof DefaultLightingProfiles.UserLightingProfile profile) {
                final var vector = profile.getVector();
                Translate.commandFeedback(context, "custom_lighting", vector.getX(), vector.getY(), vector.getZ());
            } else {
                Translate.commandFeedback(context, "current_profile");
            }
            return 0;
        }).then(argument("x", FloatArgumentType.floatArg()).then(argument("y", FloatArgumentType.floatArg()).then(argument("z", FloatArgumentType.floatArg()).executes(context -> {

            RuntimeConfig.lightingProfile = new DefaultLightingProfiles.UserLightingProfile(
                    FloatArgumentType.getFloat(context, "x"),
                    FloatArgumentType.getFloat(context, "y"),
                    FloatArgumentType.getFloat(context, "z")
            );

            Translate.commandFeedback(context, "lighting_profile_updated");
            return 0;
        }))))).then(literal("namespace").then(NAMESPACE.toNode(StringArgumentType.string()).suggests(NAMESPACE_PROVIDER).then(literal("batch").then(literal("items").executes(context -> {
            IsometricRenderHelper.batchRenderNamespaceItems(NAMESPACE.get(context));
            return 0;
        })).then(literal("blocks").executes(context -> {
            IsometricRenderHelper.batchRenderNamespaceBlocks(NAMESPACE.get(context));
            return 0;
        }))).then(literal("atlas").executes(context -> {
            IsometricRenderHelper.renderNamespaceAtlas(NAMESPACE.get(context));
            return 0;
        })))));
    }

    private static int executeBlockState(FabricClientCommandSource source, BlockState state, NbtCompound tag) {

        IsometricRenderScreen screen = new IsometricRenderScreen();

        BlockEntity be = null;

        if (state.getBlock() instanceof BlockWithEntity) {
            be = ((BlockWithEntity) state.getBlock()).createBlockEntity(MinecraftClient.getInstance().player.getBlockPos(), state);
        }

        if (be != null) {
            IsometricRenderHelper.initBlockEntity(state, be, tag);
            IsometricRenderPresets.setupBlockEntityRender(screen, be);
        } else {
            IsometricRenderPresets.setupBlockStateRender(screen, state);
        }

        IsometricRenderHelper.scheduleScreen(screen);

        return 0;
    }

    private static int executeBlockTarget(FabricClientCommandSource source) {

        final MinecraftClient client = MinecraftClient.getInstance();
        IsometricRenderScreen screen = new IsometricRenderScreen();

        if (client.crosshairTarget.getType() != HitResult.Type.BLOCK) {
            source.sendError(msg("no_block"));
            return 0;
        }

        final BlockPos hitPos = ((BlockHitResult) client.crosshairTarget).getBlockPos();
        BlockState state = client.world.getBlockState(hitPos);

        BlockEntity be = null;

        if (state.getBlock() instanceof BlockWithEntity && client.world.getBlockEntity(hitPos) != null) {
            NbtCompound tag = client.world.getBlockEntity(hitPos).createNbt();
            be = ((BlockWithEntity) state.getBlock()).createBlockEntity(hitPos, state);
            IsometricRenderHelper.initBlockEntity(state, be, tag);
        }

        if (be != null) {
            IsometricRenderPresets.setupBlockEntityRender(screen, be);
        } else {
            IsometricRenderPresets.setupBlockStateRender(screen, state);
        }

        IsometricRenderHelper.scheduleScreen(screen);
        return 0;
    }

    private static int executeEntity(FabricClientCommandSource source, Identifier entityType, NbtCompound entityTag) {

        final MinecraftClient client = MinecraftClient.getInstance();
        IsometricRenderScreen screen = new IsometricRenderScreen();

        entityTag.putString("id", entityType.toString());

        Entity entity = EntityType.loadEntityWithPassengers(entityTag, client.world, Function.identity());
        entity.updatePosition(client.player.getX(), client.player.getY(), client.player.getZ());

        if (entity instanceof MobEntity) {
            ((MobEntity) entity).setPersistent();
        }

        IsometricRenderPresets.setupEntityRender(screen, entity);

        IsometricRenderHelper.scheduleScreen(screen);

        return 0;
    }

    private static int executeEntityTarget(FabricClientCommandSource source) {
        final MinecraftClient client = MinecraftClient.getInstance();
        IsometricRenderScreen screen = new IsometricRenderScreen();

        if (client.crosshairTarget.getType() != HitResult.Type.ENTITY) {
            source.sendError(msg("no_entity"));
            return 0;
        }

        final Entity targetEntity = ((EntityHitResult) client.crosshairTarget).getEntity();
        final NbtCompound entityTag = targetEntity.writeNbt(new NbtCompound());

        entityTag.remove("UUID");
        entityTag.putString("id", Registry.ENTITY_TYPE.getId(targetEntity.getType()).toString());

        final Entity renderEntity = EntityType.loadEntityWithPassengers(entityTag, client.world, Function.identity());

        IsometricRenderPresets.setupEntityRender(screen, renderEntity);
        IsometricRenderHelper.scheduleScreen(screen);

        return 0;
    }

    private static int executeItem(FabricClientCommandSource source, ItemStack stack) {

        IsometricRenderScreen screen = new IsometricRenderScreen();
        IsometricRenderPresets.setupItemStackRender(screen, stack);
        IsometricRenderHelper.scheduleScreen(screen);

        return 0;
    }

    private static int executeArea(CommandContext<FabricClientCommandSource> context, boolean enableTranslucency) {
        DefaultPosArgument startArg = context.getArgument("start", DefaultPosArgument.class);
        DefaultPosArgument endArg = context.getArgument("end", DefaultPosArgument.class);

        BlockPos pos1 = IsometricRenderHelper.getPosFromArgument(startArg, context.getSource());
        BlockPos pos2 = IsometricRenderHelper.getPosFromArgument(endArg, context.getSource());

        BlockPos start = new BlockPos(Math.min(pos1.getX(), pos2.getX()), Math.min(pos1.getY(), pos2.getY()), Math.min(pos1.getZ(), pos2.getZ()));
        BlockPos end = new BlockPos(Math.max(pos1.getX(), pos2.getX()), Math.max(pos1.getY(), pos2.getY()), Math.max(pos1.getZ(), pos2.getZ()));

        AreaIsometricRenderScreen screen = new AreaIsometricRenderScreen();
        IsometricRenderPresets.setupAreaRender(screen, start, end);
        IsometricRenderHelper.scheduleScreen(screen);

        return 0;
    }

    private static ItemGroup itemgroup(CommandContext<?> context) {
        return context.getArgument("itemgroup", ItemGroup.class);
    }

    private static final class ArgKey<V> {

        public final Class<V> clazz;
        public final String name;

        public ArgKey(Class<V> clazz, String name) {
            this.clazz = clazz;
            this.name = name;
        }

        public V get(CommandContext<?> context) {
            return context.getArgument(this.name, this.clazz);
        }

        public RequiredArgumentBuilder<FabricClientCommandSource, V> toNode(ArgumentType<V> type) {
            return argument(this.name, type);
        }
    }

}
