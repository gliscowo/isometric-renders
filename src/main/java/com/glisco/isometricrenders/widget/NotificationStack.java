package com.glisco.isometricrenders.widget;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NotificationStack implements Element, Selectable, Drawable {

    private final List<Notification> notifications = new ArrayList<>();

    private @Nullable Notification hoveredNotification = null;

    private int bottomRightX = 0;
    private int bottomRightY = 0;

    public void setPosition(int bottomRightX, int bottomRightY) {
        this.bottomRightX = bottomRightX;
        this.bottomRightY = bottomRightY;
    }

    public void add(Text... text) {
        this.notifications.add(new Notification(Arrays.asList(text), () -> {}));
    }

    public void add(Runnable clickListener, Text... text) {
        this.notifications.add(new Notification(Arrays.asList(text), clickListener));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.notifications.removeIf(Notification::isExpired);

        final var textRenderer = MinecraftClient.getInstance().textRenderer;
        int renderY = this.bottomRightY + 10;

        this.hoveredNotification = null;
        for (var notification : this.notifications) {
            final int height = notification.getHeight();
            final int renderX = this.bottomRightX - notification.width;

            renderY -= height + 10;

            boolean hovered = false;
            if (mouseX >= renderX && mouseX <= renderX + notification.width && mouseY >= renderY && mouseY <= renderY + height) {
                this.hoveredNotification = notification;
                hovered = true;
            }

            if (notification.getOpacity() * 0xFF > 5) {
                final var alpha = (int) (0x90 * notification.getOpacity()) << 24;
                DrawableHelper.fill(matrices, renderX, renderY, renderX + notification.width, renderY + height,
                        (hovered ? 0x242424 : 0) | alpha);

                int textAlpha = (int) ((0xFF) * notification.getOpacity()) << 24;
                for (int i = 0; i < notification.lines.size(); i++) {
                    textRenderer.draw(matrices, notification.lines.get(i), renderX + 10, renderY + 10 + i * 11, 0xFFFFFF | textAlpha);
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.hoveredNotification == null) return false;

        this.hoveredNotification.clickListener.run();
        return true;
    }

    @Override
    public SelectionType getType() {
        return SelectionType.HOVERED;
    }

    @Override
    public void appendNarrations(NarrationMessageBuilder builder) {}

    // TODO move render impl to here,
    // TODO have custom subclass for persistent, progress-based notifs
    private static class Notification {

        private final List<Text> lines;
        private final int width;
        private final int height;
        private final long startTime;
        private final Runnable clickListener;

        private Notification(List<Text> lines, Runnable clickListener) {
            this.lines = lines;
            this.clickListener = clickListener;

            int longestLine = 0;
            for (var line : lines) {
                longestLine = Math.max(longestLine, MinecraftClient.getInstance().textRenderer.getWidth(line));
            }
            this.width = longestLine + 20;
            this.height = 10 + lines.size() * 11 + 10 - 2;

            this.startTime = System.currentTimeMillis();
        }

        public int getHeight() {
            if (this.age() < 5000) return this.height;
            if (this.age() < 6000) return this.height;

            return Math.round(this.progress(6000, 6500) * (this.height + 10)) - 10;
        }

        public float getOpacity() {
            if (this.age() < 5000) return 1;

            return this.progress(5000, 6000);
        }

        public boolean isExpired() {
            return this.age() > 6500;
        }

        private float progress(long from, long to) {
            long age = this.age();
            float linearProgress = MathHelper.clamp(age - from, 0, to - from) / (float) (to - from);
            return (float) ((Math.sin(Math.PI / 2 + linearProgress * Math.PI) + 1) / 2);
        }

        private long age() {
            return System.currentTimeMillis() - startTime;
        }
    }
}
