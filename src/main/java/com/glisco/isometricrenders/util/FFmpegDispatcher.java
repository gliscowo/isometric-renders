package com.glisco.isometricrenders.util;

import com.glisco.isometricrenders.IsometricRenders;
import com.glisco.isometricrenders.property.GlobalProperties;
import net.minecraft.util.Util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class FFmpegDispatcher {

    private static Boolean ffmpegDetected = null;

    public static boolean wasFFmpegDetected() {
        return ffmpegDetected != null;
    }

    public static boolean ffmpegAvailable() {
        return ffmpegDetected != null && ffmpegDetected;
    }

    public static CompletableFuture<Boolean> detectFFmpeg() {
        if (ffmpegDetected != null) {
            return CompletableFuture.completedFuture(ffmpegDetected);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                final var process = new ProcessBuilder("ffmpeg", "-version")
                        .redirectError(ProcessBuilder.Redirect.DISCARD)
                        .start();

                process.onExit().join();
                final var output = new String(process.getInputStream().readAllBytes());

                IsometricRenders.LOGGER.info("FFmpeg detected, version: {}", output.split(" ")[2]);
                return true;
            } catch (IOException exception) {
                IsometricRenders.LOGGER.info("Did not detect FFmpeg for reason: {}", exception.getMessage());
                return false;
            }
        }, Util.getMainWorkerExecutor()).whenComplete((result, throwable) -> {
            if (throwable != null) {
                ffmpegDetected = false;
                IsometricRenders.LOGGER.warn("Could not complete FFmpeg detection", throwable);
            } else {
                ffmpegDetected = result;
            }
        });
    }

    @SuppressWarnings("resource")
    public static CompletableFuture<Boolean> assemble(ExportPathSpec target, Path sourcePath, Format format) {
        if (!sourcePath.resolve("seq.png").toFile().renameTo(sourcePath.resolve("seq_0.png").toFile())) {
            IsometricRenders.LOGGER.warn("Could not rename first image sequence element - first frame will be missing");
        }

        target.resolveOffset().toFile().mkdirs();

        final var defaultArgs = new ArrayList<>(List.of(new String[]{
                "ffmpeg",
                "-f", "image2",
                "-framerate", String.valueOf(GlobalProperties.exportFramerate),
                "-i", "seq_%d.png",
                "-plays", "0"}));

        if (format.arguments.length != 0) {
            defaultArgs.addAll(Arrays.asList(format.arguments));
        }

        defaultArgs.add(target.resolveFile(format.extension).getAbsolutePath());

        final var process = new ProcessBuilder(defaultArgs)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .directory(sourcePath.toFile());

        try {
            return process.start().onExit().thenApply(exited -> {
                try {
                    Files.list(sourcePath)
                            .filter(path -> path.getFileName().toString().matches("seq_\\d+\\.png"))
                            .forEach(deletePath -> {
                                try {
                                    Files.delete(deletePath);
                                } catch (IOException e) {
                                    IsometricRenders.LOGGER.warn("Could not clean up sequence directory", e);
                                }
                            });
                } catch (IOException e) {
                    IsometricRenders.LOGGER.warn("Could not clean up sequence directory", e);
                }

                return exited.exitValue() == 0;
            });
        } catch (IOException e) {
            IsometricRenders.LOGGER.error("Could not launch ffmpeg", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    public enum Format {
        APNG("apng", new String[]{"-plays", "0"}),
        GIF("gif", new String[]{"-plays", "0"}),
        MP4("mp4", new String[]{"-preset", "slow", "-crf", "20"});

        public final String extension;
        public final String[] arguments;

        Format(String extension, String[] arguments) {
            this.extension = extension;
            this.arguments = arguments;
        }

        public Format next() {
            return switch (this) {
                case MP4 -> APNG;
                case APNG -> GIF;
                case GIF -> MP4;
            };
        }
    }

}
