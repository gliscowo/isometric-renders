package com.glisco.isometricrenders.client;

import com.glisco.isometricrenders.client.gui.IsometricRenderHelper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.Pair;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import static com.glisco.isometricrenders.client.IsometricRendersClient.prefix;

public class ImageExporter extends Thread {

    private static ImageExporter INSTANCE = null;

    private static final List<Pair<NativeImage, String>> jobs = new ArrayList<>();
    private static Pair<NativeImage, String> currentJob = null;

    public static void init() {
        if (INSTANCE != null) throw new IllegalStateException("Export Thread is already running!");
        INSTANCE = new ImageExporter();
        INSTANCE.setName("Image Export Thread");
        INSTANCE.start();
    }

    public static boolean acceptsJobs() {
        return getJobCount() < 10;
    }

    public static void addJob(NativeImage image, String name) {
        if (!acceptsJobs()) {
            sendErrorMessage();
            return;
        }
        jobs.add(new Pair<>(image, name));
    }

    public static int getJobCount() {
        return jobs.size() + (currentJob == null ? 0 : 1) + Threaded.getJobCount();
    }

    public static void clearQueue() {
        jobs.clear();
    }

    public static boolean currentlyExporting() {
        return currentJob != null || Threaded.getJobCount() > 0;
    }

    private static void sendErrorMessage() {
        MinecraftClient.getInstance().player.sendMessage(prefix("§cYour job could not be submitted because the export queue is full!"), false);
    }

    @Override
    public void run() {
        while (true) {

            while (!jobs.isEmpty()) {
                currentJob = jobs.get(0);
                jobs.remove(0);

                exportImage(currentJob);

                currentJob = null;
            }

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    public static String getProgressBarText() {
        int jobs = getJobCount();
        if (jobs == 0) return "Exporter Idle";
        return "Export Jobs: " + jobs;
    }

    private static void exportImage(Pair<NativeImage, String> job) {
        final File renderDir = FabricLoader.getInstance().getGameDir().resolve("renders").toFile();
        File file;
        if (RuntimeConfig.dumpIntoRoot) {
            file = IsometricRenderHelper.getNextFile(renderDir, IsometricRenderHelper.getLastFile(job.getRight()));
        } else {
            file = IsometricRenderHelper.getNextFile(renderDir, job.getRight());
        }

        file.getParentFile().mkdirs();

        IsometricRendersClient.LOGGER.info("Started saving image: {}", file);
        try {
            job.getLeft().writeFile(file);
            IsometricRendersClient.LOGGER.info("Finished");
        } catch (IOException e) {
            IsometricRendersClient.LOGGER.error("Saving image failed, stacktrace below");
            e.printStackTrace();
        }
        job.getLeft().close();
    }

    public static class Threaded {

        private static ThreadPoolExecutor exporters = null;

        public static void init() {
            exporters = (ThreadPoolExecutor) Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            exporters.setThreadFactory(new ThreadFactoryBuilder().setNameFormat("image-export-worker-%d").build());
            MinecraftClient.getInstance().player.sendMessage(prefix("Threaded export system initialized"), false);
        }

        public static void submit(NativeImage image, String name) {
            if (!acceptsNew()) {
                sendErrorMessage();
                return;
            }
            exporters.submit(() -> {
                exportImage(new Pair<>(image, name));
            });
        }

        public static void finish() {
            exporters.shutdown();
            if (getJobCount() > 0) {
                MinecraftClient.getInstance().player.sendMessage(prefix("Threaded export system shutting down with " + getJobCount() + " tasks remaining"), false);
            } else {
                MinecraftClient.getInstance().player.sendMessage(prefix("Threaded export system shutting down"), false);
            }
        }

        public static boolean busy() {
            return exporters != null && !exporters.isShutdown();
        }

        public static boolean acceptsNew() {
            return getJobCount() < 5;
        }

        private static int getJobCount() {
            if (exporters == null) return 0;
            return exporters.getActiveCount() + exporters.getQueue().size();
        }

    }
}
