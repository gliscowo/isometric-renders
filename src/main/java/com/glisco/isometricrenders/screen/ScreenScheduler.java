package com.glisco.isometricrenders.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

public class ScreenScheduler {

    private static Screen SCHEDULED_SCREEN = null;

    public static void schedule(Screen screen) {
        if (MinecraftClient.getInstance().currentScreen == null) {
            MinecraftClient.getInstance().setScreen(screen);
        } else {
            SCHEDULED_SCREEN = screen;
        }
    }

    public static boolean hasScheduled() {
        return SCHEDULED_SCREEN != null;
    }

    public static void open() {
        MinecraftClient.getInstance().setScreen(SCHEDULED_SCREEN);
        SCHEDULED_SCREEN = null;
    }

}
