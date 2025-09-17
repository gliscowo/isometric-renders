package com.glisco.isometricrenders;

import com.glisco.isometricrenders.command.IsorenderCommand;
import com.glisco.isometricrenders.util.AreaSelectionHelper;
import com.glisco.isometricrenders.util.ImageIO;
import com.glisco.isometricrenders.util.ParticleRestriction;
import com.glisco.isometricrenders.widget.AreaSelectionComponent;
import com.glisco.isometricrenders.widget.IOStateComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Positioning;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.hud.Hud;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public class IsometricRenders implements ClientModInitializer {

	public static final String MOD_ID = "isometric-renders";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final String VERSION = FabricLoader.getInstance().getModContainer(MOD_ID).get().getMetadata().getVersion().getFriendlyString();

    public static ParticleRestriction<?> particleRestriction = ParticleRestriction.always();

    public static boolean inRenderableDraw = false;
    public static boolean inRenderableTick = false;
    public static boolean skipWorldRender = false;

    public static Framebuffer mainTargetOverride = null;

    public static final KeyBinding SELECT = new KeyBinding("key.isometric-renders.area_select", GLFW.GLFW_KEY_C, KeyBinding.MISC_CATEGORY);

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register(IsorenderCommand::register);

        KeyBindingHelper.registerKeyBinding(SELECT);

        final var ioStateId = "io-state";
        final var areaSelectionHintId = "area-selection-hint";

        var hudId = Identifier.of(MOD_ID, "hud");
        Hud.add(hudId, () -> Containers.verticalFlow(Sizing.content(), Sizing.content()).positioning(Positioning.absolute(20, 20)));

        HudElementRegistry.addLast(hudId, (matrixStack, tickDelta) -> {
            var client = MinecraftClient.getInstance();
            var isometricHud = (FlowLayout) Hud.getComponent(hudId);

            final var ioState = isometricHud.childById(IOStateComponent.class, ioStateId);
            if ((ioState == null) == (ImageIO.taskCount() > 0 && client.currentScreen == null)) {
                if (ImageIO.taskCount() > 0 && client.currentScreen == null) {
                    isometricHud.child(new IOStateComponent().positioning(Positioning.absolute(20, 20)).id(ioStateId));
                } else {
                    isometricHud.removeChild(ioState);
                }
            }

            final var selectionHint = isometricHud.childById(AreaSelectionComponent.class, areaSelectionHintId);
            if ((selectionHint == null) == AreaSelectionHelper.shouldDraw()) {
                if (AreaSelectionHelper.shouldDraw()) {
                    isometricHud.child(new AreaSelectionComponent().id(areaSelectionHintId));
                } else {
                    isometricHud.removeChild(selectionHint);
                }
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (SELECT.wasPressed()) {
                if (client.player.isSneaking()) {
                    AreaSelectionHelper.clear();
                } else {
                    AreaSelectionHelper.select();
                }
            }
        });
    }

    public static void skipNextWorldRender() {
        skipWorldRender = true;
    }

    public static void beginRenderableDraw(){
        inRenderableDraw = true;
    }

    public static void endRenderableDraw(){
        inRenderableDraw = false;
    }

    public static void beginRenderableTick() {
        inRenderableTick = true;
    }

    public static void endRenderableTick() {
        inRenderableTick = false;
    }
}
