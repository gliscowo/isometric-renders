package com.glisco.isometricrenders.util;

import com.glisco.isometricrenders.IsometricRenders;
import net.minecraft.util.Util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

// Much of this resolution logic is adopted from ReplayMod, and it attempts to user-proof the process of locating the FFmpeg binary.
// I've opted to implement it in a more generic, less hard-coded way than ReplayMod, though, to make it dead simple to add
// new search locations if the need should ever arise.

public class FFmpegResolver {

    private static final Pattern VERSION_REGEX = Pattern.compile("^(ffmpeg version [^ ]+)", Pattern.CASE_INSENSITIVE);

    private static final String EXE_NAME_DEFAULT = "ffmpeg";
    private static final String EXE_NAME_WINDOWS = "ffmpeg.exe";

    private static final List<String> LOCATIONS_DEFAULT = List.of(
            "./ffmpeg", // Check inside .minecraft first.
            "ffmpeg"    // If it's not there, try finding it in the PATH.
    );

    private static final List<String> LOCATIONS_WINDOWS = List.of(
            "./ffmpeg.exe", // EXE placed directly in .minecraft directory.
            "./ffmpeg/bin/ffmpeg.exe", // Installed in .minecraft/ffmpeg/ as per ReplayMod.
            "ffmpeg" // Attempt system PATH location (not likely to work).
    );

    private static final List<String> LOCATIONS_MAC = List.of(
            "./ffmpeg", // FFmpeg executable in .minecraft directory.
            "ffmpeg",   // FFmpeg in (potentially unreliable) system PATH.
            "/usr/local/bin/ffmpeg", // Common install location for FFmpeg.
            "/usr/bin/ffmpeg",       // Another common install location for FFmpeg.
            "/opt/local/Cellar/ffmpeg",   // If installed with homebrew but not linked,
            "/opt/homebrew/Cellar/ffmpeg" // it should be in one of these two locations.
    );

    private static final String EXE_NAME = switch(Util.getOperatingSystem()) {
        case WINDOWS -> EXE_NAME_WINDOWS;
        default      -> EXE_NAME_DEFAULT;
    };

    private static final List<String> LOCATIONS = switch (Util.getOperatingSystem()) {
        case WINDOWS -> LOCATIONS_WINDOWS;
        case OSX     -> LOCATIONS_MAC;
        default      -> LOCATIONS_DEFAULT;
    };

    private static Boolean ffmpegAvailable = null;

    private static String ffmpegCommand = null;

    private static String ffmpegCommandOverride = null;

    public static CompletableFuture<Boolean> setCommandOverride (String command) {
        ffmpegCommandOverride = command;
        return detectFFmpeg(true);
    }

    public static Boolean alreadyChecked () {
        return ffmpegAvailable != null;
    }

    public static Boolean isFFmpegAvailable () {
        return ffmpegAvailable != null && ffmpegAvailable;
    }

    public static CompletableFuture<Optional<String>> getFFmpegCommand () {
        return getFFmpegCommand(false);
    }
    public static CompletableFuture<Optional<String>> getFFmpegCommand (Boolean retry) {
        if (ffmpegAvailable != null && !retry) {
            if (ffmpegAvailable) {
                return CompletableFuture.completedFuture(Optional.of(ffmpegCommand));
            } else {
                return CompletableFuture.completedFuture(Optional.empty());
            }
        }

        return detectFFmpeg(retry).thenApply((Boolean isAvailable) -> {
            if (isAvailable) {
                return Optional.of(ffmpegCommand);
            }
            return Optional.empty();
        });
    }

    private static String getVersionString (String command) {
        try {
            final Process process = new ProcessBuilder(command, "-version")
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();

            process.onExit().join();

            final String output = new String(process.getInputStream().readAllBytes());

            final Matcher info = VERSION_REGEX.matcher(output);

            if (!info.find() || info.group(1) == null) {
                IsometricRenders.LOGGER.info("{} does not seem to be an FFmpeg executable. (Irregular result for `ffmpeg  -version`: {})", command, output);
                return null;
            }

            return info.group(1);
        } catch (IOException exception) {
            IsometricRenders.LOGGER.info("Attempting to run {} produced error: {}", command, exception.getMessage());
            return null;
        }
    }

    public static CompletableFuture<Boolean> detectFFmpeg () {
        return detectFFmpeg(false);
    }
    public static CompletableFuture<Boolean> detectFFmpeg (Boolean retry) {
        if (ffmpegAvailable != null && !retry) {
            return CompletableFuture.completedFuture(ffmpegAvailable);
        }
        return CompletableFuture.supplyAsync(() -> {
            IsometricRenders.LOGGER.info("Attempting to locate FFmpeg.");

            // If an override is configured, only check that.
            if (ffmpegCommandOverride != null && ffmpegCommandOverride.length() > 0) {
                IsometricRenders.LOGGER.info("Checking version for configured location {}...", ffmpegCommandOverride);

                final String version = getVersionString(ffmpegCommandOverride);
                if (version == null) return false;

                IsometricRenders.LOGGER.info("FFmpeg detected as \"{}\": {}", ffmpegCommandOverride, version);
                ffmpegCommand = ffmpegCommandOverride;
                return true;

            }

            // Check all pre-defined install paths
            for (final String command : LOCATIONS) {
                IsometricRenders.LOGGER.info("Checking {}...", command);
                if (command.equals("ffmpeg") || Paths.get(command).toFile().exists()) {
                    if (command.equals("ffmpeg")) {
                        IsometricRenders.LOGGER.info("Skipping file existence check, blindly checking if command exists in system PATH...");
                    } else {
                        IsometricRenders.LOGGER.info("File exists, checking version...");
                    }

                    final String version = getVersionString(command);

                    if (version == null) continue;

                    IsometricRenders.LOGGER.info("FFmpeg detected as \"{}\": {}", command, version);
                    ffmpegCommand = command;
                    return true;
                }
            }

            // Last ditch effort, per ReplayMod, recursively search .minecraft for an FFmpeg executable (in case it's nested weirdly).
            try (Stream<Path> files = Files.walk(Paths.get(""))) {
                for (
                    Path path : files.filter((path) -> path.getFileName().toString().equals(EXE_NAME)).toList()
                ) {
                    final String command = path.toAbsolutePath().toString();

                    IsometricRenders.LOGGER.info("Found {}, checking version...", command);

                    final String version = getVersionString(command);

                    if (version == null) continue;

                    IsometricRenders.LOGGER.info("FFmpeg detected as \"{}\": {}", command, version);
                    ffmpegCommand = command;
                    return true;
                }
            } catch (IOException e) {
                IsometricRenders.LOGGER.warn("Error trying to walk file tree to find FFmpeg in .minecraft directory:", e);
            }

            return false;
        }, Util.getMainWorkerExecutor()).handle((detected, exception) -> {
            if (exception != null) {
                IsometricRenders.LOGGER.warn("Error during FFmpeg detection", exception);
                ffmpegAvailable = false;
            } else {
                ffmpegAvailable = detected;
            }
            return ffmpegAvailable;
       });
    }
}
