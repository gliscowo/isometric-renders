package com.glisco.isometricrenders.util;

import com.glisco.isometricrenders.IsometricRendersClient;
import com.glisco.isometricrenders.mixin.BlockStateArgumentAccessor;
import com.glisco.isometricrenders.mixin.EntitySummonArgumentTypeAccessor;
import com.glisco.isometricrenders.render.DefaultLightingProfiles;
import com.glisco.isometricrenders.render.IsometricRenderHelper;
import com.glisco.isometricrenders.render.IsometricRenderPresets;
import com.glisco.isometricrenders.screen.AreaIsometricRenderScreen;
import com.glisco.isometricrenders.screen.IsometricRenderScreen;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
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

import static com.glisco.isometricrenders.util.Translator.msg;
import static com.glisco.isometricrenders.IsometricRendersClient.prefix;
import static com.glisco.isometricrenders.Translator.tr;
import static net.fabricmc.fabric.api.client.command.v1.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v1.ClientCommandManager.literal;

public class IsoRenderCommand {

    private static final SuggestionProvider<FabricClientCommandSource> CLIENT_SUMMONABLE_ENTITIES;
    private static final SuggestionProvider<FabricClientCommandSource> ITEM_GROUPS;
    private static final SuggestionProvider<FabricClientCommandSource> NAMESPACE_PROVIDER;

    private static final List<String> NAMESPACES = new ArrayList<>();

    static {
        CLIENT_SUMMONABLE_ENTITIES = (context, builder) -> {
            return CommandSource.suggestFromIdentifier(Registry.ENTITY_TYPE.stream().filter(EntityType::isSummonable), builder, EntityType::getId, entityType -> {
                return new TranslatableText(Util.createTranslationKey("entity", EntityType.getId(entityType)));
            });
        };

        ITEM_GROUPS = (context, builder) -> {
            return CommandSource.suggestMatching(Arrays.stream(ItemGroup.GROUPS).map(ItemGroup::getName), builder);
        };

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

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("isorender").then(literal("block").executes(context -> executeBlockTarget(context.getSource())).then(argument("block", BlockStateArgumentType.blockState()).executes(context -> {
            final BlockStateArgument blockStateArgument = context.getArgument("block", BlockStateArgument.class);
            final BlockState blockState = blockStateArgument.getBlockState();
            return executeBlockState(context.getSource(), blockState, ((BlockStateArgumentAccessor) blockStateArgument).getData());
        }))).then(literal("item").executes(context -> {
            return executeItem(context.getSource(), context.getSource().getPlayer().getMainHandStack().copy());
        }).then(argument("item", ItemStackArgumentType.itemStack()).executes(context -> {
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

            RuntimeConfig.allowInsaneResolutions = !RuntimeConfig.allowInsaneResolutions;

            if (RuntimeConfig.allowInsaneResolutions) {
                context.getSource().sendFeedback(msg("insane_resolution_unlocked"));
            } else {
                context.getSource().sendFeedback(msg("insane_resolution_locked"));
            }

            return 0;
        })).then(literal("area").executes(context -> {
            if (!AreaSelectionHelper.tryOpenScreen()) {
                context.getSource().sendError(msg("incomplete_selection"));
            }
            return 0;
        }).then(argument("start", BlockPosArgumentType.blockPos()).then(argument("end", BlockPosArgumentType.blockPos()).executes(context -> {
            return executeArea(context, false);
        }).then(literal("enable_translucency").executes(context -> {
            return executeArea(context, true);
        }))))).then(literal("creative_tab").then(argument("itemgroup", ItemGroupArgumentType.itemGroup()).suggests(ITEM_GROUPS).then(literal("batch").then(literal("blocks").executes(context -> {
            ItemGroup group = context.getArgument("itemgroup", ItemGroup.class);
            IsometricRenderHelper.batchRenderItemGroupBlocks(group);
            return 0;
        })).then(literal("items").executes(context -> {
            ItemGroup group = context.getArgument("itemgroup", ItemGroup.class);
            IsometricRenderHelper.batchRenderItemGroupItems(group);
            return 0;
        }))).then(literal("atlas").executes(context -> {
            ItemGroup group = context.getArgument("itemgroup", ItemGroup.class);
            IsometricRenderHelper.renderItemGroupAtlas(group);
            return 0;
        })))).then(literal("lighting").executes(context -> {
            if (RuntimeConfig.lightingProfile instanceof DefaultLightingProfiles.UserLightingProfile profile) {
                context.getSource().sendFeedback(msg("custom_lighting", profile.getVector().getX(), profile.getVector().getY(), profile.getVector().getZ()));
            } else {
                context.getSource().sendFeedback(msg("current_profile", RuntimeConfig.lightingProfile.getFriendlyName()));
            }
            return 0;
        }).then(argument("x", FloatArgumentType.floatArg()).then(argument("y", FloatArgumentType.floatArg()).then(argument("z", FloatArgumentType.floatArg()).executes(context -> {
            RuntimeConfig.lightingProfile = new DefaultLightingProfiles.UserLightingProfile(FloatArgumentType.getFloat(context, "x"), FloatArgumentType.getFloat(context, "y"), FloatArgumentType.getFloat(context, "z"));
            context.getSource().sendFeedback(msg("lighting_profile_updated"));
            return 0;
        }))))).then(literal("namespace").then(argument("namespace", StringArgumentType.string()).suggests(NAMESPACE_PROVIDER).then(literal("batch").then(literal("items").executes(context -> {
            String namespace = StringArgumentType.getString(context, "namespace");
            IsometricRenderHelper.batchRenderNamespaceItems(namespace);
            return 0;
        })).then(literal("blocks").executes(context -> {
            String namespace = StringArgumentType.getString(context, "namespace");
            IsometricRenderHelper.batchRenderNamespaceBlocks(namespace);
            return 0;
        }))).then(literal("atlas").executes(context -> {
            String namespace = StringArgumentType.getString(context, "namespace");
            IsometricRenderHelper.renderNamespaceAtlas(namespace);
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

        AreaIsometricRenderScreen screen = new AreaIsometricRenderScreen(enableTranslucency);
        IsometricRenderPresets.setupAreaRender(screen, start, end, enableTranslucency);
        IsometricRenderHelper.scheduleScreen(screen);

        return 0;
    }
}
