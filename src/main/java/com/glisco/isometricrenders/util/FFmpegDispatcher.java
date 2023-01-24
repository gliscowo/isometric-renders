package com.glisco.isometricrenders.util;

import com.glisco.isometricrenders.IsometricRenders;
import com.glisco.isometricrenders.property.GlobalProperties;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class FFmpegDispatcher {

    @SuppressWarnings("resource")
    public static CompletableFuture<File> assemble(ExportPathSpec target, Path sourcePath, Format format) {
        return FFmpegResolver.getFFmpegCommand().thenApply((Optional<String> ffmpegCommand) -> {
            if (ffmpegCommand.isEmpty()) {
                IsometricRenders.LOGGER.error("Call to assemble despite no FFmpeg command found or configured!");
                return null;
            }

            target.resolveOffset().toFile().mkdirs();

            final var defaultArgs = new ArrayList<>(List.of(new String[]{
                    ffmpegCommand.get(),
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
                }).exceptionally((throwable) -> {
                    IsometricRenders.LOGGER.error("Could not launch ffmpeg", throwable);
                    return null;
                }).join();
            } catch (IOException e) {
                IsometricRenders.LOGGER.error("Could not launch ffmpeg", e);
                return null;
            }
        });
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
