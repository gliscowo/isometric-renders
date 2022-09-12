
package com.glisco.isometricrenders.util;

import com.glisco.isometricrenders.IsometricRenders;
import com.glisco.isometricrenders.property.GlobalProperties;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.text.Text;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;

public class ImageIO {

    private static final AtomicInteger TASK_COUNT = new AtomicInteger(0);

    public static CompletableFuture<File> save(NativeImage image, ExportPathSpec path) {
        final var future = new CompletableFuture<File>();

        TASK_COUNT.incrementAndGet();
        ForkJoinPool.commonPool().submit(() -> {
            final var imageFile = path.resolveFile("png");

            imageFile.getParentFile().mkdirs();

            try {
                image.writeTo(imageFile);
                IsometricRenders.LOGGER.info("Image " + imageFile.getAbsolutePath() + " saved");
                future.complete(imageFile);
            } catch (IOException e) {
                IsometricRenders.LOGGER.warn("Could not save image " + imageFile.getAbsolutePath(), e);
                future.completeExceptionally(e);
            } finally {
                TASK_COUNT.decrementAndGet();
            }
        });

        return future;
    }

    public static int taskCount() {
        return TASK_COUNT.get();
    }

    public static Text progressText() {
        int jobs = taskCount();
        if (jobs == 0) return Translate.gui("exporter.idle");
        return Translate.gui("exporter.jobs", jobs);
    }

    public static Path next(Path input) {
        final var filename = input.getFileName().toString();

        var separatorIndex = filename.lastIndexOf('.');
        if (separatorIndex == -1) separatorIndex = filename.length();

        final var name = filename.substring(0, separatorIndex);
        final var extension = filename.substring(separatorIndex);

        final var path = input.getParent();

        var currentPath = path.resolve(join(name, extension, 0));
        var lastPath = currentPath;

        for (int i = 1; Files.exists(currentPath); i++) {
            lastPath = currentPath;
            currentPath = path.resolve(join(name, extension, i));
        }

        return GlobalProperties.overwriteLatest.get() ? lastPath : currentPath;
    }

    private static String join(String filename, String extension, int index) {
        return index == 0
                ? filename + extension
                : filename + "_" + index + (extension.isEmpty() ? "" : "_" + extension);
    }

}
