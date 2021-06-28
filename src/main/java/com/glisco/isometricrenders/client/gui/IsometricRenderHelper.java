package com.glisco.isometricrenders.client.gui;

import com.glisco.isometricrenders.client.RuntimeConfig;
import com.glisco.isometricrenders.mixin.CameraInvoker;
import com.glisco.isometricrenders.mixin.DefaultPosArgumentAccessor;
import com.glisco.isometricrenders.mixin.MinecraftClientAccessor;
import com.glisco.isometricrenders.mixin.NativeImageAccessor;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.command.argument.DefaultPosArgument;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;
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
    public static final Matrix4f LIGHT_MATRIX;

    private static Screen SCHEDULED_SCREEN = null;

    static {
        LIGHT_MATRIX = new Matrix4f();
        LIGHT_MATRIX.loadIdentity();
        LIGHT_MATRIX.addToLastColumn(new Vector3f(0.15f, 0.5f, 0.15f));
    }

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");

    public static void batchRenderItemGroupBlocks(ItemGroup group) {
        DefaultedList<ItemStack> stacks = DefaultedList.of();
        group.appendStacks(stacks);

        BatchIsometricBlockRenderScreen screen = new BatchIsometricBlockRenderScreen(extractBlocks(stacks), group.getName());
        scheduleScreen(screen);
    }

    public static void batchRenderItemGroupItems(ItemGroup group) {
        DefaultedList<ItemStack> stacks = DefaultedList.of();
        group.appendStacks(stacks);

        BatchIsometricItemRenderScreen screen = new BatchIsometricItemRenderScreen(stacks.iterator(), group.getName());
        scheduleScreen(screen);
    }

    public static void renderItemGroupAtlas(ItemGroup group) {
        DefaultedList<ItemStack> stacks = DefaultedList.of();
        group.appendStacks(stacks);
        renderItemAtlas(group.getName(), stacks, false);
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

                    MinecraftClient.getInstance().getItemRenderer().renderItem(stack, ModelTransformation.Mode.GUI, 15728880, OverlayTexture.DEFAULT_UV, matrices, vertexConsumerProvider);

                    matrices.translate(1.2, 0, 0);

                }

                matrices.pop();

            }
        }, "atlases/" + name);

        if (forceOpen) {
            MinecraftClient.getInstance().openScreen(screen);
        } else {
            IsometricRenderHelper.scheduleScreen(screen);
        }
    }

    public static NativeImage renderIntoImage(int size, RenderCallback renderCallback) {

        Framebuffer framebuffer = new Framebuffer(size, size, true, MinecraftClient.IS_SYSTEM_MAC);

        RenderSystem.pushMatrix();
        RenderSystem.enableBlend();
        RenderSystem.clear(16640, MinecraftClient.IS_SYSTEM_MAC);

        framebuffer.setClearColor(0, 0, 0, 0);
        framebuffer.clear(MinecraftClient.IS_SYSTEM_MAC);

        framebuffer.beginWrite(true);

        RenderSystem.matrixMode(GL11.GL_PROJECTION);
        RenderSystem.pushMatrix();
        RenderSystem.loadIdentity();
        RenderSystem.ortho(-1, 1, 1, -1, -100.0, 100.0);
        RenderSystem.matrixMode(GL11.GL_MODELVIEW);
        RenderSystem.pushMatrix();
        RenderSystem.loadIdentity();

        setupLighting();

        VertexConsumerProvider.Immediate vertexConsumers = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
        MatrixStack matrixStack = new MatrixStack();

        renderCallback.render(matrixStack, vertexConsumers, ((MinecraftClientAccessor) MinecraftClient.getInstance()).getRenderTickCounter().tickDelta);

        vertexConsumers.draw();

        RenderSystem.popMatrix();
        RenderSystem.matrixMode(GL11.GL_PROJECTION);
        RenderSystem.popMatrix();
        RenderSystem.matrixMode(GL11.GL_MODELVIEW);
        RenderSystem.popMatrix();

        framebuffer.endWrite();

        return takeKeyedSnapshot(framebuffer, RuntimeConfig.backgroundColor, false, false);
    }

    public static NativeImage takeKeyedSnapshot(Framebuffer framebuffer, int backgroundColor, boolean crop, boolean key) {
        NativeImage img = new NativeImage(framebuffer.textureWidth, framebuffer.textureHeight, false);
        RenderSystem.bindTexture(framebuffer.getColorAttachment());
        img.loadFromTextureImage(0, false);
        img.mirrorVertically();

        if (framebuffer != MinecraftClient.getInstance().getFramebuffer()) framebuffer.delete();

        if(key){
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

            if (RuntimeConfig.exportOpacity != 100) {

                int opacityMask = 0xFFFFFF | (Math.round(RuntimeConfig.exportOpacity * 2.55f) << 24);

                for (int i = 0; i < pixelColors.length; i++) {
                    pixelColors[i] = pixelColors[i] & opacityMask;
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

    public static void setupLighting() {
        DiffuseLighting.enableForLevel(LIGHT_MATRIX);
    }

    public static Camera getParticleCamera() {
        Camera camera = MinecraftClient.getInstance().getEntityRenderDispatcher().camera;
        ((CameraInvoker) camera).invokeSetRotation(RuntimeConfig.rotation, RuntimeConfig.angle);
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
            MinecraftClient.getInstance().openScreen(screen);
        } else {
            SCHEDULED_SCREEN = screen;
        }
    }

    public static boolean isScreenScheduled() {
        return SCHEDULED_SCREEN != null;
    }

    public static void openScheduledScreen() {
        MinecraftClient.getInstance().openScreen(SCHEDULED_SCREEN);
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
