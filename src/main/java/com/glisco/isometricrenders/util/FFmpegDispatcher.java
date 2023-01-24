package com.glisco.isometricrenders.util;

import com.glisco.isometricrenders.IsometricRenders;
import com.glisco.isometricrenders.property.GlobalProperties;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class FFmpegDispatcher {

    private static String ffmpegCommand = "ffmpeg";

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
            // On macOS, MultiMC-based launchers fail to give the minecraft process a PATH env variable
            // which includes /usr/local/bin, which is the common install location for FFmpeg.
            // So in that case we check `/usr/local/bin` and `/usr/bin` as well as the implicit `ffmpeg`.
            for (Iterator<String> itr =
                 MinecraftClient.IS_SYSTEM_MAC
                         ? List.of("ffmpeg", "/usr/local/bin/ffmpeg", "/usr/bin/ffmpeg").iterator()
                         : List.of("ffmpeg").iterator();
                 itr.hasNext();
            ) {
                ffmpegCommand = itr.next();
                try {
                    final var process = new ProcessBuilder(ffmpegCommand, "-version")
                            .redirectError(ProcessBuilder.Redirect.DISCARD)
                            .start();

                    process.onExit().join();
                    final var output = new String(process.getInputStream().readAllBytes());

                    IsometricRenders.LOGGER.info("FFmpeg detected as \"{}\", version: {}", ffmpegCommand, output.split(" ")[2]);
                    return true;
                } catch (IOException exception) {
                    IsometricRenders.LOGGER.info("No FFmpeg found with command \"{}\" ({})", ffmpegCommand, exception.getMessage());
                }
            }
            IsometricRenders.LOGGER.info("Couldn't find any installed FFmpeg.");
            return false;
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
    public static CompletableFuture<File> assemble(ExportPathSpec target, Path sourcePath, Format format) {
        target.resolveOffset().toFile().mkdirs();

        final var defaultArgs = new ArrayList<>(List.of(new String[]{
                ffmpegCommand,
                "-y",
                "-f", "image2",
                "-framerate", String.valueOf(GlobalProperties.exportFramerate),
                "-i", "seq_%d.png"}));

        if (format.arguments.length != 0) {
            defaultArgs.addAll(Arrays.asList(format.arguments));
        }

        final var animationFile = target.resolveFile(format.extension);
        defaultArgs.add(animationFile.getAbsolutePath());

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

                return animationFile;
            });
        } catch (IOException e) {
            IsometricRenders.LOGGER.error("Could not launch ffmpeg", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    public enum Format {
        APNG("apng", new String[]{"-plays", "0"}),
        GIF("gif", new String[]{"-plays", "0", "-pix_fmt", "yuv420p"}),
        MP4("mp4", new String[]{"-preset", "slow", "-crf", "20", "-pix_fmt", "yuv420p"});

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
