package com.glisco.isometricrenders;

import com.glisco.isometricrenders.command.IsorenderCommand;
import com.glisco.isometricrenders.render.TooltipRenderable;
import com.glisco.isometricrenders.screen.RenderScreen;
import com.glisco.isometricrenders.util.AreaSelectionHelper;
import com.glisco.isometricrenders.util.ImageIO;
import com.glisco.isometricrenders.util.ParticleRestriction;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.option.KeyBinding;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class IsometricRenders implements ClientModInitializer {

    public static final Logger LOGGER = LogManager.getLogger();
    public static final String VERSION = FabricLoader.getInstance().getModContainer("isometric-renders").get().getMetadata().getVersion().getFriendlyString();

    public static ParticleRestriction<?> particleRestriction = ParticleRestriction.always();

    public static boolean inRenderableTick = false;
    public static boolean skipWorldRender = false;
    public static boolean centerNextTooltip = false;

    private static final KeyBinding SELECT = new KeyBinding("key.isometric-renders.area_select", GLFW.GLFW_KEY_C, KeyBinding.MISC_CATEGORY);

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register(IsorenderCommand::register);

        ClientLifecycleEvents.CLIENT_STARTED.register(newClient -> {
            TooltipRenderable.TooltipScreen.INSTANCE.init(newClient, 10000, 10000);
        });

        HudRenderCallback.EVENT.register((matrixStack, tickDelta) -> {
            final MinecraftClient client = MinecraftClient.getInstance();
            if (ImageIO.taskCount() < 1 || client.currentScreen != null) return;

            DrawableHelper.fill(matrixStack, 20, 20, 140, 60, 0x90000000);
            client.textRenderer.draw(matrixStack, ImageIO.progressText(), 30, 30, 0xFFFFFF);

            RenderScreen.drawExportProgressBar(matrixStack, 30, 45, 100, 50, 10);
        });

        KeyBindingHelper.registerKeyBinding(SELECT);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!SELECT.wasPressed()) return;

            if (client.player.isSneaking()) {
                AreaSelectionHelper.clear();
            } else {
                AreaSelectionHelper.select();
            }
        });
    }

    public static void skipNextWorldRender() {
        skipWorldRender = true;
    }

    public static void beginRenderableTick() {
        inRenderableTick = true;
    }

    public static void endRenderableTick() {
        inRenderableTick = false;
    }
}
