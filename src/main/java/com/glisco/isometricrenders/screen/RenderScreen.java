package com.glisco.isometricrenders.screen;

import com.glisco.isometricrenders.IsometricRenders;
import com.glisco.isometricrenders.mixin.access.ParticleManagerAccessor;
import com.glisco.isometricrenders.property.DefaultPropertyBundle;
import com.glisco.isometricrenders.property.GlobalProperties;
import com.glisco.isometricrenders.property.Property;
import com.glisco.isometricrenders.render.DefaultRenderable;
import com.glisco.isometricrenders.render.Renderable;
import com.glisco.isometricrenders.render.RenderableDispatcher;
import com.glisco.isometricrenders.render.TickingRenderable;
import com.glisco.isometricrenders.util.*;
import com.glisco.isometricrenders.widget.NotificationStack;
import com.glisco.isometricrenders.widget.WidgetColumnBuilder;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.ConfirmChatLinkScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.glisco.isometricrenders.property.GlobalProperties.*;
import static com.glisco.isometricrenders.util.Translate.gui;

public class RenderScreen extends Screen {

    private static final Int2ObjectMap<Consumer<DefaultPropertyBundle>> KEYBOARD_CONTROLS = new Int2ObjectOpenHashMap<>();

    static {
        KEYBOARD_CONTROLS.put(GLFW.GLFW_KEY_W, properties -> properties.yOffset.modify(-1000));
        KEYBOARD_CONTROLS.put(GLFW.GLFW_KEY_S, properties -> properties.yOffset.modify(1000));
        KEYBOARD_CONTROLS.put(GLFW.GLFW_KEY_D, properties -> properties.xOffset.modify(1000));
        KEYBOARD_CONTROLS.put(GLFW.GLFW_KEY_A, properties -> properties.xOffset.modify(-1000));

        KEYBOARD_CONTROLS.put(GLFW.GLFW_KEY_UP, properties -> properties.slant.modify(-5));
        KEYBOARD_CONTROLS.put(GLFW.GLFW_KEY_DOWN, properties -> properties.slant.modify(5));
        KEYBOARD_CONTROLS.put(GLFW.GLFW_KEY_LEFT, properties -> properties.rotation.modify(-10));
        KEYBOARD_CONTROLS.put(GLFW.GLFW_KEY_RIGHT, properties -> properties.rotation.modify(10));

        KEYBOARD_CONTROLS.put(GLFW.GLFW_KEY_RIGHT_BRACKET, properties -> properties.scale.modify(10));
        KEYBOARD_CONTROLS.put(GLFW.GLFW_KEY_SLASH, properties -> properties.scale.modify(-10));
    }

    private final MemoryGuard memoryGuard = new MemoryGuard(0.75f);

    private final Renderable<?> renderable;
    private Consumer<File> exportCallback = (file) -> {};

    public final Property<Boolean> playAnimations = Property.of(false);
    public final Property<Boolean> tickParticles = Property.of(true);

    private final NotificationStack notificationStack = new NotificationStack();
    private ButtonWidget exportAnimationButton;

    private boolean drawOnlyBackground = false;
    private boolean captureScheduled = false;
    private boolean guiRebuildScheduled = false;

    private int viewportBeginX;
    private int viewportEndX;

    private final List<Framebuffer> renderedFrames = new ArrayList<>();
    private int remainingAnimationFrames;

    public RenderScreen(Renderable<?> renderable) {
        super(Text.of(""));
        this.renderable = renderable;
        this.memoryGuard.update();
    }

    @Override
    protected void init() {
        this.client.keyboard.setRepeatEvents(true);
        this.viewportBeginX = (int) ((this.width - this.height) * 0.5);
        this.viewportEndX = (int) (this.width - (this.width - this.height) * 0.5) + 1;

        ((ParticleManagerAccessor) MinecraftClient.getInstance().particleManager).isometric$getParticles().clear();
        IsometricRenders.particleRestriction = this.renderable.particleRestriction();

        this.client.options.setPerspective(Perspective.FIRST_PERSON);

        final var leftBuilder = new WidgetColumnBuilder(this::addDrawableChild, 20, 0, viewportBeginX);
        this.renderable.properties().buildGuiControls(this.renderable, leftBuilder);

        // --------

        final var rightBuilder = new WidgetColumnBuilder(this::addDrawableChild, 20, viewportEndX, viewportBeginX);

        rightBuilder.label("render_options");
        var colorField = rightBuilder.labeledTextField("#000000", 50, "background_color");

        colorField.setTextPredicate(s -> s.matches("^#([A-Fa-f\\d]{0,6})$"));
        colorField.setText("#" + String.format("%02X", backgroundColor >> 16) + String.format("%02X", backgroundColor >> 8 & 0xFF) + String.format("%02X", backgroundColor & 0xFF));
        colorField.setChangedListener(s -> {
            if (s.substring(1).length() < 6) return;
            backgroundColor = Integer.parseInt(s.substring(1), 16);
        });

        rightBuilder.propertyCheckbox(playAnimations, "animations");
        rightBuilder.move(-5);
        rightBuilder.propertyCheckbox(tickParticles, "particles");

        rightBuilder.move(10);
        rightBuilder.label("export_options");

        rightBuilder.propertyCheckbox(saveIntoRoot, "dump_into_root");
        rightBuilder.move(-5);
        rightBuilder.propertyCheckbox(overwriteLatest, "overwrite_latest");
        rightBuilder.move(-5);

        final var exportButton = rightBuilder.button("export", 0, 75, button -> captureScheduled = true);
        rightBuilder.button("open_folder", 80, 75, button -> {
            Util.getOperatingSystem().open(this.renderable.exportPath().resolveOffset().toFile());
        });
        rightBuilder.nextRow();

        if (!GraphicsEnvironment.isHeadless()) {
            rightBuilder.move(-5);
            rightBuilder.button("export_to_clipboard", 0, 75, button -> {

                this.notificationStack.add(Translate.gui("copied_to_clipboard"));

                try (var image = RenderableDispatcher.drawIntoImage(this.renderable, 0, exportResolution)) {
                    final var transferable = new ImageTransferable(javax.imageio.ImageIO.read(new ByteArrayInputStream(image.getBytes())));
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(transferable, transferable);
                } catch (IOException e) {
                    IsometricRenders.LOGGER.error("mfw", e);
                }

            });
            rightBuilder.nextRow();
        }

        rightBuilder.move(5);
        var resolutionField = rightBuilder.labeledTextField(String.valueOf(exportResolution), 50, "renderer_resolution");

        resolutionField.setEditableColor(0x00FF00);
        resolutionField.setTextPredicate(s -> s.matches("\\d{0,5}"));
        resolutionField.setChangedListener(s -> {
            if (s.isBlank()) return;
            int resolution = Integer.parseInt(s);

            // Exhibit A
            // ((resolution & (resolution - 1)) != 0)
            // what?

            if ((resolution < 16 || resolution > 16384) && !unsafe.get()) {
                resolutionField.setEditableColor(0xFF0000);
                exportButton.active = false;
            } else {
                resolutionField.setEditableColor(0x00FF00);
                exportResolution = resolution;
                exportButton.active = true;
            }
        });

        // --------

        rightBuilder.move(10);
        rightBuilder.label("animation_options");

        if (FFmpegDispatcher.wasFFmpegDetected()) {
            if (FFmpegDispatcher.ffmpegAvailable()) {
                final var framesField = rightBuilder.labeledTextField(String.valueOf(exportFrames), 30, "animation_frames");
                framesField.setTextPredicate(s -> s.matches("\\d*"));
                framesField.setChangedListener(s -> {
                    if (s.isBlank()) return;
                    exportFrames = Integer.parseInt(s);
                });
                final var framerateField = rightBuilder.labeledTextField(String.valueOf(exportFramerate), 30, "animation_framerate");
                framerateField.setTextPredicate(s -> s.matches("\\d*"));
                framerateField.setChangedListener(s -> {
                    if (s.isBlank()) return;
                    exportFramerate = Integer.parseInt(s);
                });

                this.exportAnimationButton = rightBuilder.button("export_animation", 0, 100, button -> {
                    if (this.memoryGuard.canFit(this.estimateMemoryUsage(exportFrames)) || Screen.hasShiftDown()) {
                        this.remainingAnimationFrames = exportFrames;

                        this.client.getWindow().setFramerateLimit(Integer.parseInt(framerateField.getText()));
                        IsometricRenders.skipNextWorldRender();

                        button.active = false;
                        button.setMessage(Translate.gui("exporting"));
                    }
                }).withTooltip(this, () -> this.memoryGuard.getStatusTooltip(this.estimateMemoryUsage(exportFrames)));

                rightBuilder.button("format." + animationFormat.extension, 105, 35, button -> {
                    animationFormat = animationFormat.next();
                    button.setMessage(Translate.gui("format." + animationFormat.extension));
                });

                rightBuilder.nextRow();
            } else {
                rightBuilder.label("no_ffmpeg_1");
                rightBuilder.label("no_ffmpeg_2");
                rightBuilder.move(-6);
                rightBuilder.label("no_ffmpeg_3").clickAction(labelWidget -> {
                    this.client.setScreen(new ConfirmChatLinkScreen(confirmed -> {
                        if (confirmed) {
                            Util.getOperatingSystem().open("https://ffmpeg.org/download.html");
                        }

                        this.client.setScreen(this);
                    }, "https://ffmpeg.org/download.html", true));
                    return true;
                });
            }
        } else {
            rightBuilder.label("detecting_ffmpeg");
            FFmpegDispatcher.detectFFmpeg().whenComplete((aBoolean, throwable) -> this.guiRebuildScheduled = true);
        }

        rightBuilder.dynamicLabel(() -> {
            return this.remainingAnimationFrames == 0
                    ? Text.empty()
                    : Translate.gui("export_remaining_frames", this.remainingAnimationFrames);
        });

        this.notificationStack.setPosition(viewportEndX - 10, height - 10);
        this.addDrawableChild(this.notificationStack);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (this.guiRebuildScheduled) {
            this.clearAndInit();
            this.guiRebuildScheduled = false;
        }

        if (this.drawOnlyBackground) {
            fill(matrices, 0, 0, this.width, this.height, GlobalProperties.backgroundColor | 255 << 24);
        } else {
            renderBackground(matrices);
        }

        final var window = client.getWindow();
        final var effectiveTickDelta = playAnimations.get() ? client.getTickDelta() : 0;
        RenderableDispatcher.drawIntoActiveFramebuffer(
                this.renderable,
                window.getFramebufferWidth() / (float) window.getFramebufferHeight(),
                effectiveTickDelta
        );

        if (!this.drawOnlyBackground) {
            drawFramingHint(matrices);
            drawGuiBackground(matrices);

            RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, MinecraftClient.IS_SYSTEM_MAC);
            super.render(matrices, mouseX, mouseY, delta);

            fill(matrices, viewportEndX + 160, 45, viewportEndX + 168, 53, GlobalProperties.backgroundColor | 255 << 24);

            client.textRenderer.draw(matrices, gui("hotkeys"), viewportEndX + 12, height - 20, 0xAAAAAA);

            client.textRenderer.draw(matrices, gui("memory_warning1"), 10, height - 60, 0xAAAAAA);
            client.textRenderer.draw(matrices, gui("memory_warning2"), 10, height - 50, 0xAAAAAA);
            client.textRenderer.draw(matrices, gui("memory_warning3"), 10, height - 40, 0xAAAAAA);
            client.textRenderer.draw(matrices, gui("memory_warning4"), 10, height - 30, 0xAAAAAA);
            client.textRenderer.draw(matrices, gui("memory_warning5"), 10, height - 20, 0xAAAAAA);

            if (ImageIO.taskCount() > 1) {
                int exportRootX = viewportBeginX + 10;
                int exportRootY = height - 50;

                DrawableHelper.fill(matrices, exportRootX, exportRootY, exportRootX + 120, exportRootY + 40, 0x90000000);
                client.textRenderer.draw(matrices, ImageIO.progressText(), exportRootX + 10, exportRootY + 10, 0xFFFFFF);

                RenderScreen.drawExportProgressBar(matrices, exportRootX + 10, exportRootY + 25, 100, 50, 10);
            }
        }

        if (this.captureScheduled) {
            ImageIO.save(
                    RenderableDispatcher.drawIntoImage(this.renderable, 0, exportResolution),
                    this.renderable.exportPath()
            ).whenComplete((file, throwable) -> {
                exportCallback.accept(file);
                this.notificationStack.add(
                        () -> Util.getOperatingSystem().open(file),
                        Translate.gui("exported_as"),
                        Text.literal(ExportPathSpec.exportRoot().relativize(file.toPath()).toString())
                );
            });

            this.captureScheduled = false;
        }

        if (this.remainingAnimationFrames > 0) {
            this.renderedFrames.add(RenderableDispatcher.drawIntoTexture(this.renderable, effectiveTickDelta, exportResolution));

            IsometricRenders.skipNextWorldRender();

            if (--this.remainingAnimationFrames == 0) {
                this.client.getWindow().setFramerateLimit(this.client.options.getMaxFps().getValue());

                final var overwriteValue = overwriteLatest.get();
                overwriteLatest.set(false);

                CompletableFuture<File> exportFuture = null;

                for (int i = 0; i < this.renderedFrames.size(); i++) {
                    exportFuture = ImageIO.save(
                            RenderableDispatcher.copyFramebufferIntoImage(this.renderedFrames.get(i)),
                            ExportPathSpec.forced("sequence", "seq_" + i)
                    );
                    this.renderedFrames.get(i).delete();
                }

                this.renderedFrames.clear();

                exportFuture.whenComplete((file, throwable) -> {
                    overwriteLatest.set(overwriteValue);
                    if (throwable != null) return;

                    this.exportAnimationButton.setMessage(Translate.gui("converting"));
                    this.notificationStack.add(Translate.gui("converting_image_sequence"));

                    FFmpegDispatcher.assemble(
                            this.renderable.exportPath(),
                            ExportPathSpec.exportRoot().resolve("sequence/"),
                            animationFormat
                    ).whenComplete((animationFile, animationThrowable) -> {
                        this.exportAnimationButton.active = true;
                        this.exportAnimationButton.setMessage(Translate.gui("export_animation"));

                        this.notificationStack.add(
                                () -> Util.getOperatingSystem().open(animationFile),
                                Translate.gui("animation_saved"),
                                Text.literal(ExportPathSpec.exportRoot().relativize(animationFile.toPath()).toString())
                        );
                    });
                });
            }
        }
    }

    private int estimateMemoryUsage(int frames) {
        return (int) ((exportResolution * exportResolution * 4L * frames) / 1024L / 1024L);
    }

    private void drawFramingHint(MatrixStack matrices) {
        fill(matrices, viewportBeginX + 5, 0, viewportEndX - 5, 5, 0x90000000);
        fill(matrices, viewportBeginX + 5, height - 5, viewportEndX - 5, height, 0x90000000);
        fill(matrices, viewportBeginX, 0, viewportBeginX + 5, height, 0x90000000);
        fill(matrices, viewportEndX - 5, 0, viewportEndX, height, 0x90000000);
    }

    private void drawGuiBackground(MatrixStack matrices) {
        fill(matrices, 0, 0, viewportBeginX, height, 0x90000000);
        fill(matrices, viewportEndX, 0, width, height, 0x90000000);
    }

    @Override
    public void tick() {
        if (this.client.world.getTime() % 40 == 0) {
            this.memoryGuard.update();
        }

        if (playAnimations.get() && this.renderable instanceof TickingRenderable tickable) {
            IsometricRenders.beginRenderableTick();
            tickable.tick();
            IsometricRenders.endRenderableTick();
        }
    }

    private boolean isInViewport(double mouseX) {
        return mouseX > viewportBeginX && mouseX < viewportEndX;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!(this.renderable.properties() instanceof DefaultPropertyBundle properties)) return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);

        if (isInViewport(mouseX)) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
                double xScaling = (100d / properties.scale.get()) * (this.client.getWindow().getWidth() / (float) this.client.getWindow().getScaledWidth());
                double yScaling = (100d / properties.scale.get()) * (this.client.getWindow().getHeight() / (float) this.client.getWindow().getScaledHeight());

                properties.xOffset.modify((int) (50 * deltaX * xScaling));
                properties.yOffset.modify((int) (50 * deltaY * yScaling));
                return true;
            } else if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                properties.rotation.modify((int) (deltaX * 2));
                return true;
            } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                properties.slant.modify((int) (deltaY * 2));
                return true;
            }
        }

        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!(this.renderable.properties() instanceof DefaultPropertyBundle properties)) return super.mouseClicked(mouseX, mouseY, button);

        if (isInViewport(mouseX) && Screen.hasControlDown()) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
                properties.xOffset.setToDefault();
                properties.yOffset.setToDefault();
            } else if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                properties.rotation.setToDefault();
            } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                properties.slant.setToDefault();
            }
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!(this.renderable.properties() instanceof DefaultPropertyBundle properties)) return super.mouseScrolled(mouseX, mouseY, amount);

        if (isInViewport(mouseX)) {
            properties.scale.modify((int) (amount * Math.max(1, properties.scale.get() * 0.075)));
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) return true;

        if (keyCode == GLFW.GLFW_KEY_F12) {
            this.captureScheduled = true;
        } else if (keyCode == GLFW.GLFW_KEY_F10) {
            this.drawOnlyBackground = !this.drawOnlyBackground;
        } else if (KEYBOARD_CONTROLS.containsKey(keyCode) && this.renderable instanceof DefaultRenderable) {
            KEYBOARD_CONTROLS.get(keyCode).accept((DefaultPropertyBundle) this.renderable.properties());
        }
        return true;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void removed() {
        this.renderable.dispose();
        this.client.keyboard.setRepeatEvents(false);
        IsometricRenders.particleRestriction = ParticleRestriction.always();
        this.client.getWindow().setFramerateLimit(this.client.options.getMaxFps().getValue());
    }

    public void scheduleCapture() {
        this.captureScheduled = true;
    }

    public void setExportCallback(Consumer<File> exportCallback) {
        this.exportCallback = exportCallback;
    }

    public static void drawExportProgressBar(MatrixStack matrices, int x, int y, int drawWidth, int barWidth, double speed) {
        int end = x + drawWidth + barWidth;

        int offset = (int) (System.currentTimeMillis() / speed % (drawWidth + barWidth));

        int endWithOffset = x + offset;
        if (endWithOffset > end) endWithOffset = end;

        fill(matrices, Math.max(x + offset - barWidth, x), y, Math.min(endWithOffset, x + drawWidth), y + 2, 0xFF00FF00);
    }
}
