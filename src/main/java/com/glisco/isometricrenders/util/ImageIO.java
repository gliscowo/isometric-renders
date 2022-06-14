
package com.glisco.isometricrenders.util;

import com.glisco.isometricrenders.IsometricRenders;
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
        final var file = input.getFileName().toString().split("\\.");
        final var path = input.getParent();

        var currentPath = path.resolve(String.join(".", file));
        for (int i = 1; Files.exists(currentPath); i++) {
            currentPath = path.resolve(String.join("_" + i + ".", file));
        }

        return currentPath;
    }

}
