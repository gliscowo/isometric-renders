package com.glisco.isometricrenders.client.gui;

import com.glisco.isometricrenders.client.ImageExportThread;
import com.glisco.isometricrenders.mixin.CameraInvoker;
import com.glisco.isometricrenders.mixin.MinecraftClientAccessor;
import com.glisco.isometricrenders.mixin.NativeImageAccessor;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotUtils;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.entity.Entity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Tickable;
import net.minecraft.util.Util;
import net.minecraft.util.collection.DefaultedList;
import org.jetbrains.annotations.NotNull;
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

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");

    public static void setupBlockStateRender(IsometricRenderScreen screen, @NotNull BlockState state) {
        final MinecraftClient client = MinecraftClient.getInstance();
        screen.setRenderCallback((matrices, vertexConsumerProvider, tickDelta) -> {
            matrices.push();
            matrices.translate(-0.5, 0, -0.5);

            client.getBlockRenderManager().renderBlockAsEntity(state, matrices, vertexConsumerProvider, 15728880, OverlayTexture.DEFAULT_UV);

            double xOffset = client.player.getX() % 1d;
            double zOffset = client.player.getZ() % 1d;

            if (xOffset < 0) xOffset += 1;
            if (zOffset < 0) zOffset += 1;

            matrices.translate(xOffset, 1.65 + client.player.getY() % 1d, zOffset);

            client.particleManager.renderParticles(matrices, (VertexConsumerProvider.Immediate) vertexConsumerProvider, client.gameRenderer.getLightmapTextureManager(), getParticleCamera(), tickDelta);

            matrices.pop();
        });

        screen.setTickCallback(() -> {
            if (client.world.random.nextDouble() < 0.150) {
                state.getBlock().randomDisplayTick(state, client.world, client.player.getBlockPos(), client.world.random);
            }
        });
    }

    public static void setupBlockEntityRender(IsometricRenderScreen screen, @NotNull BlockEntity entity) {

        final MinecraftClient client = MinecraftClient.getInstance();
        screen.setRenderCallback((matrices, vertexConsumerProvider, tickDelta) -> {

            matrices.push();
            matrices.translate(-0.5, 0, -0.5);

            client.getBlockRenderManager().renderBlockAsEntity(entity.getCachedState(), matrices, vertexConsumerProvider, 15728880, OverlayTexture.DEFAULT_UV);

            if (BlockEntityRenderDispatcher.INSTANCE.get(entity) != null) {
                BlockEntityRenderDispatcher.INSTANCE.get(entity).render(entity, tickDelta, matrices, vertexConsumerProvider, 15728880, OverlayTexture.DEFAULT_UV);
            }

            double xOffset = client.player.getX() % 1d;
            double zOffset = client.player.getZ() % 1d;

            if (xOffset < 0) xOffset += 1;
            if (zOffset < 0) zOffset += 1;

            matrices.translate(xOffset, 1.65 + client.player.getY() % 1d, zOffset);

            client.particleManager.renderParticles(matrices, (VertexConsumerProvider.Immediate) vertexConsumerProvider, client.gameRenderer.getLightmapTextureManager(), getParticleCamera(), tickDelta);

            matrices.pop();
        });

        screen.setTickCallback(() -> {

            if (entity instanceof Tickable) {
                ((Tickable) entity).tick();
            }
            if (client.world.random.nextDouble() < 0.150) {
                entity.getCachedState().getBlock().randomDisplayTick(entity.getCachedState(), client.world, client.player.getBlockPos(), client.world.random);
            }
        });

    }

    public static void setupItemStackRender(IsometricRenderScreen screen, @NotNull ItemStack stack) {
        screen.setRenderCallback((matrices, vertexConsumerProvider, tickDelta) -> {
            matrices.push();
            matrices.scale(4, 4, 4);
            MinecraftClient.getInstance().getItemRenderer().renderItem(stack, ModelTransformation.Mode.GROUND, 15728880, OverlayTexture.DEFAULT_UV, matrices, vertexConsumerProvider);
            matrices.pop();
        });
    }

    public static void setupEntityRender(IsometricRenderScreen screen, @NotNull Entity entity) {

        final MinecraftClient client = MinecraftClient.getInstance();
        screen.setRenderCallback((matrixStack, vertexConsumerProvider, delta) -> {
            matrixStack.push();
            matrixStack.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(180));
            entity.setPos(client.player.getX(), client.player.getY(), client.player.getZ());
            client.getEntityRenderDispatcher().render(entity, 0, 0, 0, 0, delta, matrixStack, vertexConsumerProvider, 15728880);

            matrixStack.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(-180));
            matrixStack.translate(0, 2, 0);
            client.particleManager.renderParticles(matrixStack, (VertexConsumerProvider.Immediate) vertexConsumerProvider, client.gameRenderer.getLightmapTextureManager(), getParticleCamera(), delta);
            matrixStack.pop();
        });
        screen.setTickCallback(() -> {
            client.world.tickEntity(entity);
        });
        screen.setClosedCallback(() -> {
            entity.updatePosition(0, 0, 0);
        });
    }

    public static void batchRenderItemGroupBlocks(ItemGroup group) {
        DefaultedList<ItemStack> stacks = DefaultedList.of();
        group.appendStacks(stacks);

        MinecraftClient.getInstance().openScreen(new BatchIsometricBlockRenderScreen(extractBlocks(stacks)));
    }

    public static void batchRenderItemGroupItems(ItemGroup group) {
        DefaultedList<ItemStack> stacks = DefaultedList.of();
        group.appendStacks(stacks);

        MinecraftClient.getInstance().openScreen(new BatchIsometricItemRenderScreen(stacks.iterator()));
    }

    public static void renderItemGroupAtlas(ItemGroup group, int size, int columns, float scale) {
        DefaultedList<ItemStack> stacks = DefaultedList.of();
        group.appendStacks(stacks);
        renderItemAtlas(stacks, size, columns, scale);
    }

    public static void renderItemAtlas(List<ItemStack> stacks, int size, int columns, float scale) {

        renderAndSave(size, (matrices, vertexConsumerProvider, tickDelta) -> {

            matrices.scale(1, -1, 1);
            matrices.translate(-0.925 + scale * 0.05, 0.925 - scale * 0.05, 0);
            matrices.scale(0.125f * scale, 0.125f * scale, 1);

            int rows = (int) Math.ceil(stacks.size() / (double) columns);

            for (int i = 0; i < rows; i++) {

                matrices.push();
                matrices.translate(0, -1.2 * i, 0);

                for (int j = 0; j < columns; j++) {

                    int index = i * columns + j;
                    if (index > stacks.size() - 1) continue;

                    ItemStack stack = stacks.get(index);

                    MinecraftClient.getInstance().getItemRenderer().renderItem(stack, ModelTransformation.Mode.GUI, 15728880, OverlayTexture.DEFAULT_UV, matrices, vertexConsumerProvider);

                    matrices.translate(1.2, 0, 0);

                }

                matrices.pop();

            }
        });
    }

    public static void renderAndSave(int size, RenderCallback renderCallback) {

        Framebuffer framebuffer = new Framebuffer(size, size, true, MinecraftClient.IS_SYSTEM_MAC);

        RenderSystem.pushMatrix();
        RenderSystem.enableBlend();
        RenderSystem.clear(16640, MinecraftClient.IS_SYSTEM_MAC);

        float r = (IsometricRenderScreen.backgroundColor >> 16) / 255f;
        float g = (IsometricRenderScreen.backgroundColor >> 8 & 0xFF) / 255f;
        float b = (IsometricRenderScreen.backgroundColor & 0xFF) / 255f;

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
        RenderSystem.setupGuiFlatDiffuseLighting(Util.make(new Vector3f(0.2F, 1.0F, -0.7F), Vector3f::normalize), Util.make(new Vector3f(-0.2F, 1.0F, 0.7F), Vector3f::normalize));

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

        takeKeyedScreenshot(framebuffer, IsometricRenderScreen.backgroundColor, false);
    }

    public static Camera getParticleCamera() {
        Camera camera = MinecraftClient.getInstance().getEntityRenderDispatcher().camera;
        ((CameraInvoker) camera).invokeSetRotation(IsometricRenderScreen.rotation, IsometricRenderScreen.angle);
        return camera;
    }

    public static void takeKeyedScreenshot(Framebuffer framebuffer, int backgroundColor, boolean crop) {
        NativeImage img = ScreenshotUtils.takeScreenshot(0, 0, framebuffer);
        if (framebuffer != MinecraftClient.getInstance().getFramebuffer()) framebuffer.delete();

        int argbColor = backgroundColor | 255 << 24;
        int r = (argbColor >> 16) & 0xFF;
        int b = argbColor & 0xFF;
        int abgrColor = (argbColor & 0xFF00FF00) | (b << 16) | r;

        long pointer = ((NativeImageAccessor) (Object) img).getPointer();

        final IntBuffer buffer = MemoryUtil.memIntBuffer(pointer, (img.getWidth() * img.getHeight()));
        int[] ints = new int[buffer.remaining()];
        buffer.get(ints);
        buffer.clear();

        for (int i = 0; i < ints.length; i++) {
            if (ints[i] == abgrColor) {
                ints[i] = 0;
            }
        }

        buffer.put(ints);

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

        ImageExportThread.addJob(rect);

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

    public static Iterator<BlockState> extractBlocks(List<ItemStack> stacks) {
        return stacks.stream().filter(itemStack -> !itemStack.isEmpty() && itemStack.getItem() instanceof BlockItem).map(itemStack -> ((BlockItem) itemStack.getItem()).getBlock().getDefaultState()).iterator();
    }

    @FunctionalInterface
    public interface RenderCallback {
        void render(MatrixStack matrices, VertexConsumerProvider vertexConsumerProvider, float tickDelta);
    }

}
