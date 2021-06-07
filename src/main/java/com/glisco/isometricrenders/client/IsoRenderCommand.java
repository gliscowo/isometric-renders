package com.glisco.isometricrenders.client;

import com.glisco.isometricrenders.client.gui.AreaIsometricRenderScreen;
import com.glisco.isometricrenders.client.gui.IsometricRenderHelper;
import com.glisco.isometricrenders.client.gui.IsometricRenderPresets;
import com.glisco.isometricrenders.client.gui.IsometricRenderScreen;
import com.glisco.isometricrenders.mixin.BlockEntityAccessor;
import com.glisco.isometricrenders.mixin.BlockStateArgumentAccessor;
import com.glisco.isometricrenders.mixin.EntitySummonArgumentTypeAccessor;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.minecraft.block.Block;
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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

import java.util.Arrays;
import java.util.function.Function;

import static com.glisco.isometricrenders.client.IsometricRendersClient.prefix;
import static net.fabricmc.fabric.api.client.command.v1.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v1.ClientCommandManager.literal;

public class IsoRenderCommand {

    private static final SuggestionProvider<FabricClientCommandSource> CLIENT_SUMMONABLE_ENTITIES;
    private static final SuggestionProvider<FabricClientCommandSource> ITEM_GROUPS;

    static {
        CLIENT_SUMMONABLE_ENTITIES = (context, builder) -> {
            return CommandSource.suggestFromIdentifier(Registry.ENTITY_TYPE.stream().filter(EntityType::isSummonable), builder, EntityType::getId, entityType -> {
                return new TranslatableText(Util.createTranslationKey("entity", EntityType.getId(entityType)));
            });
        };

        ITEM_GROUPS = (context, builder) -> {
            return CommandSource.suggestMatching(Arrays.stream(ItemGroup.GROUPS).map(ItemGroup::getName), builder);
        };
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
        }))).then(literal("entity").then(argument("entity", EntitySummonArgumentType.entitySummon()).suggests(CLIENT_SUMMONABLE_ENTITIES).executes(context -> {
            Identifier id = context.getArgument("entity", Identifier.class);
            return executeEntity(context.getSource(), EntitySummonArgumentTypeAccessor.invokeValidate(id), new CompoundTag());
        }).then(argument("nbt", NbtCompoundTagArgumentType.nbtCompound()).executes(context -> {
            Identifier id = context.getArgument("entity", Identifier.class);
            return executeEntity(context.getSource(), EntitySummonArgumentTypeAccessor.invokeValidate(id), context.getArgument("nbt", CompoundTag.class));
        })))).then(literal("insanity").executes(context -> {

            RuntimeConfig.allowInsaneResolutions = !RuntimeConfig.allowInsaneResolutions;

            if (RuntimeConfig.allowInsaneResolutions) {
                context.getSource().sendFeedback(prefix("Insane resolutions §cunlocked§7. I will not be your place to cry if this blows up your computer."));
            } else {
                context.getSource().sendFeedback(prefix("Insane resolutions §alocked§7. You're safe again"));
            }

            return 0;
        })).then(literal("area").then(argument("start", BlockPosArgumentType.blockPos()).then(argument("end", BlockPosArgumentType.blockPos()).executes(context -> {

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
        })))).then(literal("creative_tab").then(argument("itemgroup", ItemGroupArgumentType.itemGroup()).suggests(ITEM_GROUPS).then(literal("batch").then(literal("blocks").executes(context -> {
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
        })))));
    }

    private static int executeBlockState(FabricClientCommandSource source, BlockState state, CompoundTag tag) {

        IsometricRenderScreen screen = new IsometricRenderScreen();

        BlockEntity be = null;

        if (state.getBlock() instanceof BlockWithEntity) {
            be = ((BlockWithEntity) state.getBlock()).createBlockEntity(MinecraftClient.getInstance().world);
            ((BlockEntityAccessor) be).setCachedState(state);
            be.setLocation(MinecraftClient.getInstance().world, MinecraftClient.getInstance().player.getBlockPos());

            if (tag != null) {

                CompoundTag copyTag = tag.copy();

                copyTag.putInt("x", 0);
                copyTag.putInt("y", 0);
                copyTag.putInt("z", 0);

                be.fromTag(be.getCachedState(), copyTag);
            }
        }

        if (be != null) {
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
            source.sendError(Text.of("You're not looking at a block"));
            return 0;
        }

        final BlockPos hitPos = ((BlockHitResult) client.crosshairTarget).getBlockPos();
        BlockState state = client.world.getBlockState(hitPos);

        BlockEntity be = null;

        if (state.getBlock() instanceof BlockWithEntity) {

            CompoundTag tag = client.world.getBlockEntity(hitPos).toTag(new CompoundTag());

            be = ((BlockWithEntity) state.getBlock()).createBlockEntity(client.world);
            ((BlockEntityAccessor) be).setCachedState(state);
            be.setLocation(client.world, MinecraftClient.getInstance().player.getBlockPos());

            CompoundTag copyTag = tag.copy();

            copyTag.putInt("x", 0);
            copyTag.putInt("y", 0);
            copyTag.putInt("z", 0);

            be.fromTag(be.getCachedState(), copyTag);
            be.setLocation(client.world, MinecraftClient.getInstance().player.getBlockPos());
        }

        if (be != null) {
            IsometricRenderPresets.setupBlockEntityRender(screen, be);
        } else {
            IsometricRenderPresets.setupBlockStateRender(screen, state);
        }

        IsometricRenderHelper.scheduleScreen(screen);
        return 0;
    }

    private static int executeEntity(FabricClientCommandSource source, Identifier entityType, CompoundTag entityTag) {

        final MinecraftClient client = MinecraftClient.getInstance();
        IsometricRenderScreen screen = new IsometricRenderScreen();

        entityTag.putString("id", entityType.toString());

        Entity entity = EntityType.loadEntityWithPassengers(entityTag, client.world, Function.identity());
        entity.updatePosition(client.player.getX(), client.player.getY(), client.player.getZ());
        entity.setWorld(client.world);
        if (entity instanceof MobEntity) {
            ((MobEntity) entity).setPersistent();
        }

        IsometricRenderPresets.setupEntityRender(screen, entity);

        IsometricRenderHelper.scheduleScreen(screen);

        return 0;
    }

    private static int executeItem(FabricClientCommandSource source, ItemStack stack) {

        MinecraftClient client = MinecraftClient.getInstance();
        IsometricRenderScreen screen = new IsometricRenderScreen();

        IsometricRenderPresets.setupItemStackRender(screen, stack);

        IsometricRenderHelper.scheduleScreen(screen);

        return 0;
    }

    private static BlockState nullifyIfInvisible(BlockState state, BlockPos pos, World world) {
        if (state.isAir()) return null;
        for (Direction direction : Direction.values()) {
            if (Block.shouldDrawSide(state, world, pos, direction))
                return state;
        }
        return null;
    }
}
