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
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotUtils;
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
import java.nio.IntBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class IsometricRenderHelper {

    public static boolean allowParticles = true;
    public static final Matrix4f LIGHT_MATRIX;

    static {
        LIGHT_MATRIX = new Matrix4f();
        LIGHT_MATRIX.loadIdentity();
        LIGHT_MATRIX.addToLastColumn(new Vector3f(0.15f, 0.5f, 0.15f));
    }

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");

    public static void batchRenderItemGroupBlocks(ItemGroup group) {
        DefaultedList<ItemStack> stacks = DefaultedList.of();
        group.appendStacks(stacks);

        BatchIsometricBlockRenderScreen screen = new BatchIsometricBlockRenderScreen(extractBlocks(stacks));
        MinecraftClient.getInstance().openScreen(screen);
    }

    public static void batchRenderItemGroupItems(ItemGroup group) {
        DefaultedList<ItemStack> stacks = DefaultedList.of();
        group.appendStacks(stacks);

        BatchIsometricItemRenderScreen screen = new BatchIsometricItemRenderScreen(stacks.iterator());
        MinecraftClient.getInstance().openScreen(screen);
    }

    public static void renderItemGroupAtlas(ItemGroup group) {
        DefaultedList<ItemStack> stacks = DefaultedList.of();
        group.appendStacks(stacks);
        renderItemAtlas(group.getName(), stacks);
    }

    public static void renderItemAtlas(String name, List<ItemStack> stacks) {
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

        MinecraftClient.getInstance().openScreen(screen);
    }

    public static NativeImage renderIntoImage(int size, RenderCallback renderCallback) {

        Framebuffer framebuffer = new Framebuffer(size, size, true, MinecraftClient.IS_SYSTEM_MAC);

        RenderSystem.pushMatrix();
        RenderSystem.enableBlend();
        RenderSystem.clear(16640, MinecraftClient.IS_SYSTEM_MAC);

        float r = (RuntimeConfig.backgroundColor >> 16) / 255f;
        float g = (RuntimeConfig.backgroundColor >> 8 & 0xFF) / 255f;
        float b = (RuntimeConfig.backgroundColor & 0xFF) / 255f;

        framebuffer.setClearColor(r, g, b, 0);
        framebuffer.clear(MinecraftClient.IS_SYSTEM_MAC);

        framebuffer.beginWrite(true);

        RenderSystem.matrixMode(GL11.GL_PROJECTION);
        RenderSystem.pushMatrix();
        RenderSystem.loadIdentity();
        RenderSystem.ortho(-1, 1, 1, -1, -100.0, 100.0);
        RenderSystem.matrixMode(GL11.GL_MODELVIEW);
        RenderSystem.pushMatrix();
        RenderSystem.loadIdentity();

        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        Matrix4f matrix4f = new Matrix4f();
        matrix4f.loadIdentity();
        matrix4f.addToLastColumn(new Vector3f(0.15f, 0.5f, 0.15f));
        DiffuseLighting.enableForLevel(matrix4f);

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

        return takeKeyedSnapshot(framebuffer, RuntimeConfig.backgroundColor, false);
    }

    public static NativeImage takeKeyedSnapshot(Framebuffer framebuffer, int backgroundColor, boolean crop) {
        NativeImage img = ScreenshotUtils.takeScreenshot(0, 0, framebuffer);
        if (framebuffer != MinecraftClient.getInstance().getFramebuffer()) framebuffer.delete();

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

        int i = img.getWidth();
        int j = img.getHeight();
        int k = 0;
        int l = 0;
        if (i > j) {
            k = (i - j) / 2;
            i = j;
        } else {
            l = (j - i) / 2;
            j = i;
        }

        NativeImage rect = new NativeImage(i, i, false);
        if (crop) {
            img.resizeSubRectTo(k, l, i, j, rect);
        } else {
            rect = img;
        }

        return rect;
    }

    public static File getScreenshotFilename(File directory) {
        String string = DATE_FORMAT.format(new Date());
        int i = 1;

        while (true) {
            File file = new File(directory, string + (i == 1 ? "" : "_" + i) + ".png");
            if (!file.exists()) {
                return file;
            }

            ++i;
        }
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

    public static BlockPos getPosFromArgument(DefaultPosArgument argument, FabricClientCommandSource source) {

        DefaultPosArgumentAccessor accessor = (DefaultPosArgumentAccessor) argument;
        Vec3d pos = source.getPlayer().getPos();

        return new BlockPos(accessor.getX().toAbsoluteCoordinate(pos.x), accessor.getY().toAbsoluteCoordinate(pos.y), accessor.getZ().toAbsoluteCoordinate(pos.z));

    }

    public static Iterator<BlockState> extractBlocks(List<ItemStack> stacks) {
        return stacks.stream().filter(itemStack -> !itemStack.isEmpty() && itemStack.getItem() instanceof BlockItem).map(itemStack -> ((BlockItem) itemStack.getItem()).getBlock().getDefaultState()).iterator();
    }

    @FunctionalInterface
    public interface RenderCallback {
        void render(MatrixStack matrices, VertexConsumerProvider vertexConsumerProvider, float tickDelta);
    }

}
