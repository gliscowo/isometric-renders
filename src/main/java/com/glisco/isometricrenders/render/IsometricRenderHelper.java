package com.glisco.isometricrenders.render;

import com.glisco.isometricrenders.mixin.access.BlockEntityAccessor;
import com.glisco.isometricrenders.mixin.access.CameraInvoker;
import com.glisco.isometricrenders.mixin.access.DefaultPosArgumentAccessor;
import com.glisco.isometricrenders.mixin.access.NativeImageAccessor;
import com.glisco.isometricrenders.screen.BatchIsometricBlockRenderScreen;
import com.glisco.isometricrenders.screen.BatchIsometricItemRenderScreen;
import com.glisco.isometricrenders.screen.ItemAtlasRenderScreen;
import com.glisco.isometricrenders.util.RuntimeConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.command.argument.DefaultPosArgument;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.registry.Registry;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;

public class IsometricRenderHelper {

    public static boolean allowParticles = true;
    private static Screen SCHEDULED_SCREEN = null;

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");

    public static void batchRenderItemGroupBlocks(ItemGroup group) {
        DefaultedList<ItemStack> stacks = DefaultedList.of();
        group.appendStacks(stacks);

        BatchIsometricBlockRenderScreen screen = new BatchIsometricBlockRenderScreen(extractBlocks(stacks), "creative-tab_" + group.getName());
        scheduleScreen(screen);
    }

    public static void batchRenderItemGroupItems(ItemGroup group) {
        DefaultedList<ItemStack> stacks = DefaultedList.of();
        group.appendStacks(stacks);

        BatchIsometricItemRenderScreen screen = new BatchIsometricItemRenderScreen(stacks.iterator(), "creative-tab_" + group.getName());
        scheduleScreen(screen);
    }

    public static void batchRenderNamespaceItems(String namespace) {
        var stacks = Registry.ITEM.stream().filter(item -> Registry.ITEM.getId(item).getNamespace().equals(namespace)).map(ItemStack::new);

        BatchIsometricItemRenderScreen screen = new BatchIsometricItemRenderScreen(stacks.iterator(), "namespace_" + namespace);
        scheduleScreen(screen);
    }

    public static void batchRenderNamespaceBlocks(String namespace) {
        var stacks = Registry.ITEM.stream().filter(item -> Registry.ITEM.getId(item).getNamespace().equals(namespace)).map(ItemStack::new);

        BatchIsometricBlockRenderScreen screen = new BatchIsometricBlockRenderScreen(extractBlocks(stacks.toList()), "namespace_" + namespace);
        scheduleScreen(screen);
    }

    public static void renderNamespaceAtlas(String namespace) {
        var stacks = Registry.ITEM.stream().filter(item -> Registry.ITEM.getId(item).getNamespace().equals(namespace)).map(ItemStack::new);

        renderItemAtlas("namespace_" + namespace, stacks.toList(), false);
    }

    public static void renderItemGroupAtlas(ItemGroup group) {
        DefaultedList<ItemStack> stacks = DefaultedList.of();
        group.appendStacks(stacks);
        renderItemAtlas("creative-tab_" + group.getName(), stacks, false);
    }

    public static void renderItemAtlas(String name, List<ItemStack> stacks, boolean forceOpen) {
        ItemAtlasRenderScreen screen = new ItemAtlasRenderScreen();

        screen.setup((matrices, vertexConsumerProvider, tickDelta) -> {

            matrices.translate(-0.88 + 0.05, 0.925 - 0.05, 0);
            matrices.scale(0.125f, 0.125f, 1);

            int rows = (int) Math.ceil(stacks.size() / (double) RuntimeConfig.atlasColumns);
            for (int i = 0; i < rows; i++) {
                matrices.push();
                matrices.translate(0, -1.2 * i, 0);

                for (int j = 0; j < RuntimeConfig.atlasColumns; j++) {
                    int index = i * RuntimeConfig.atlasColumns + j;
                    if (index > stacks.size() - 1) continue;

                    ItemStack stack = stacks.get(index);
                    MinecraftClient.getInstance().getItemRenderer()
                            .renderItem(stack, ModelTransformation.Mode.GUI, 15728880, OverlayTexture.DEFAULT_UV, matrices, vertexConsumerProvider, 0);

                    matrices.translate(1.2, 0, 0);
                }
                matrices.pop();
            }
        }, "atlases/" + name);

        if (forceOpen) {
            MinecraftClient.getInstance().setScreen(screen);
        } else {
            IsometricRenderHelper.scheduleScreen(screen);
        }
    }

    public static NativeImage renderIntoImage(int size, RenderCallback renderCallback, LightingProfile lightingProfile) {

        Framebuffer framebuffer = new SimpleFramebuffer(size, size, true, MinecraftClient.IS_SYSTEM_MAC);

        Matrix4f modelMatrix = RenderSystem.getModelViewMatrix().copy();
        MatrixStack modelStack = RenderSystem.getModelViewStack();
        modelStack.push();

        RenderSystem.enableBlend();
        RenderSystem.clear(16640, MinecraftClient.IS_SYSTEM_MAC);

        framebuffer.setClearColor(0, 0, 0, 0);
        framebuffer.clear(MinecraftClient.IS_SYSTEM_MAC);

        framebuffer.beginWrite(true);

        RenderSystem.backupProjectionMatrix();
        Matrix4f projectionMatrix = Matrix4f.projectionMatrix(-1, 1, 1, -1, -100, 100);
        RenderSystem.setProjectionMatrix(projectionMatrix);

        modelStack.push();
        modelStack.loadIdentity();
        modelStack.scale(1, -1, -1);

        RenderSystem.applyModelViewMatrix();

        lightingProfile.setupForExternal();

        VertexConsumerProvider.Immediate vertexConsumers = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
        MatrixStack matrixStack = new MatrixStack();
        matrixStack.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(-180));

        renderCallback.render(matrixStack, vertexConsumers, MinecraftClient.getInstance().getTickDelta());

        vertexConsumers.draw();

        modelStack.pop();

        modelStack.loadIdentity();
        modelStack.multiplyPositionMatrix(modelMatrix);
        RenderSystem.applyModelViewMatrix();
        modelStack.pop();

        RenderSystem.restoreProjectionMatrix();

        framebuffer.endWrite();

        return takeSnapshot(framebuffer, RuntimeConfig.backgroundColor, false, false);
    }

    public static NativeImage takeSnapshot(Framebuffer framebuffer, int backgroundColor, boolean crop, boolean key) {

        final NativeImage img = new NativeImage(framebuffer.textureWidth, framebuffer.textureHeight, false);
        RenderSystem.bindTexture(framebuffer.getColorAttachment());
        img.loadFromTextureImage(0, false);
        img.mirrorVertically();

        if (framebuffer != MinecraftClient.getInstance().getFramebuffer()) framebuffer.delete();

        if (key) {
            int argbColor = backgroundColor | 255 << 24;
            int r = (argbColor >> 16) & 0xFF;
            int b = argbColor & 0xFF;
            int abgrColor = (argbColor & 0xFF00FF00) | (b << 16) | r;

            long pointer = ((NativeImageAccessor) (Object) img).getPointer();

            final IntBuffer buffer = MemoryUtil.memIntBuffer(pointer, (img.getWidth() * img.getHeight()));
            int[] pixelColors = new int[buffer.remaining()];
            buffer.get(pixelColors);
            buffer.clear();

            for (int i = 0; i < pixelColors.length; i++) {
                if (pixelColors[i] == abgrColor) {
                    pixelColors[i] = 0;
                }
            }

            buffer.put(pixelColors);
            buffer.clear();
        }

        int imgWidth = img.getWidth();
        int imgHeight = img.getHeight();

        NativeImage rect = new NativeImage(imgWidth, imgWidth, false);
        if (crop) {
            int k = 0;
            int l = 0;
            if (imgWidth > imgHeight) {
                k = (imgWidth - imgHeight) / 2;
                imgWidth = imgHeight;
            } else {
                l = (imgHeight - imgWidth) / 2;
                imgHeight = imgWidth;
            }
            img.resizeSubRectTo(k, l, imgWidth, imgHeight, rect);
        } else {
            rect = img;
        }

        return rect;
    }

    /**
     * Tries to prepare a {@link BlockEntity} for rendering inside a screen
     *
     * @param state The {@link BlockState} this {@code BlockEntity} should use
     * @param be    The {@code BlockEntity} to prepare, this can be null if the block does not have one for some reason
     * @param nbt   An optional {@link NbtCompound} tag to write to the entity
     */
    public static void initBlockEntity(BlockState state, @Nullable BlockEntity be, @Nullable NbtCompound nbt) {

        if (be == null) return;

        ((BlockEntityAccessor) be).setCachedState(state);
        be.setWorld(MinecraftClient.getInstance().world);

        if (nbt == null) return;

        NbtCompound copyTag = nbt.copy();

        copyTag.putInt("x", 0);
        copyTag.putInt("y", 0);
        copyTag.putInt("z", 0);

        be.readNbt(copyTag);
    }

    public static void setupLighting() {
        Matrix4f matrix4f = new Matrix4f();
        matrix4f.loadIdentity();
        matrix4f.addToLastColumn(new Vec3f(-0.15f, -0.5f, -0.15f));
        DiffuseLighting.enableForLevel(matrix4f);
    }

    public static Camera getParticleCamera() {
        Camera camera = MinecraftClient.getInstance().getEntityRenderDispatcher().camera;
        ((CameraInvoker) camera).invokeSetRotation(RuntimeConfig.rotation + 180, RuntimeConfig.angle);
        return camera;
    }

    public static File getNextFile(File baseDir, String filename) {
        int i = 0;
        while (true) {
            File file = new File(baseDir, filename + (i == 0 ? "" : "_" + i) + ".png");
            if (!file.exists()) {
                return file;
            }
            ++i;
        }
    }

    public static Path getNextFolder(File baseDir, String name) {
        int i = 0;
        while (true) {
            File file = new File(baseDir, name + (i == 0 ? "" : "_" + i) + "/");
            if (!file.exists() || (file.isDirectory() && isDirectoryEmpty(file.toPath()))) {
                return file.toPath();
            }
            ++i;
        }
    }

    public static boolean isDirectoryEmpty(Path directory) {
        try {
            return !Files.list(directory).findAny().isPresent();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String getLastFile(String file) {
        return new File(file).getName();
    }

    public static BlockPos getPosFromArgument(DefaultPosArgument argument, FabricClientCommandSource source) {

        DefaultPosArgumentAccessor accessor = (DefaultPosArgumentAccessor) argument;
        Vec3d pos = source.getPlayer().getPos();

        return new BlockPos(accessor.getX().toAbsoluteCoordinate(pos.x), accessor.getY().toAbsoluteCoordinate(pos.y), accessor.getZ().toAbsoluteCoordinate(pos.z));
    }

    public static void scheduleScreen(Screen screen) {
        if (MinecraftClient.getInstance().currentScreen == null) {
            MinecraftClient.getInstance().setScreen(screen);
        } else {
            SCHEDULED_SCREEN = screen;
        }
    }

    public static boolean isScreenScheduled() {
        return SCHEDULED_SCREEN != null;
    }

    public static void openScheduledScreen() {
        MinecraftClient.getInstance().setScreen(SCHEDULED_SCREEN);
        SCHEDULED_SCREEN = null;
    }

    public static Iterator<BlockState> extractBlocks(List<ItemStack> stacks) {
        return stacks.stream().filter(itemStack -> !itemStack.isEmpty() && itemStack.getItem() instanceof BlockItem).map(itemStack -> ((BlockItem) itemStack.getItem()).getBlock().getDefaultState()).iterator();
    }

    @FunctionalInterface
    public interface RenderCallback {
        void render(MatrixStack matrices, VertexConsumerProvider vertexConsumerProvider, float tickDelta);
    }

}
