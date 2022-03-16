package com.glisco.isometricrenders.util;

import com.glisco.isometricrenders.IsometricRendersClient;
import com.glisco.isometricrenders.render.IsometricRenderHelper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.text.Text;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import static com.glisco.isometricrenders.util.Translate.gui;

public class ImageExporter extends Thread {

    private static ImageExporter INSTANCE = null;

    private static final List<Job> jobs = new ArrayList<>();
    private static Job currentJob = null;

    public static void init() {
        if (INSTANCE != null) throw new IllegalStateException("Export Thread is already running!");
        INSTANCE = new ImageExporter();
        INSTANCE.setName("Image Export Thread");
        INSTANCE.start();
    }

    public static boolean acceptsJobs() {
        return getJobCount() < 10;
    }

    public static CompletableFuture<File> addJob(NativeImage image, String name) {
        if (!acceptsJobs()) {
            sendErrorMessage();
            return CompletableFuture.completedFuture(null);
        }

        var future = new CompletableFuture<File>();
        jobs.add(new Job(image, name, future));
        synchronized (INSTANCE) {
            INSTANCE.notify();
        }
        return future;
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
        Translate.chat("full_queue");
    }

    @Override
    public synchronized void run() {
        while (true) {

            while (!jobs.isEmpty()) {
                currentJob = jobs.get(0);
                jobs.remove(0);

                exportImage(currentJob);

                currentJob = null;
            }

            try {
                this.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    public static Text getProgressBarText() {
        int jobs = getJobCount();
        if (jobs == 0) return gui("exporter.idle");
        return gui("exporter.jobs", jobs);
    }

    private static void exportImage(Job job) {
        final File renderDir = FabricLoader.getInstance().getGameDir().resolve("renders").toFile();
        File file;
        if (RuntimeConfig.dumpIntoRoot) {
            file = IsometricRenderHelper.getNextFile(renderDir, IsometricRenderHelper.getLastFile(job.name()));
        } else {
            file = IsometricRenderHelper.getNextFile(renderDir, job.name());
        }

        file.getParentFile().mkdirs();

        IsometricRendersClient.LOGGER.info("Started saving image: {}", file);
        try {
            job.image().writeTo(file);
            IsometricRendersClient.LOGGER.info("Finished");
        } catch (IOException e) {
            IsometricRendersClient.LOGGER.error("Saving image failed, stacktrace below");
            e.printStackTrace();
        }
        job.image().close();
        job.future.complete(file);
    }

    public static class Threaded {

        private static ThreadPoolExecutor exporters = null;

        public static void init() {
            exporters = (ThreadPoolExecutor) Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            exporters.setThreadFactory(new ThreadFactoryBuilder().setNameFormat("image-export-worker-%d").build());
            Translate.chat("threaded_export_system_initialized");
        }

        public static CompletableFuture<File> submit(NativeImage image, String name) {
            if (!acceptsNew()) {
                sendErrorMessage();
                return CompletableFuture.completedFuture(null);
            }

            var future = new CompletableFuture<File>();
            exporters.submit(() -> exportImage(new Job(image, name, future)));
            return future;
        }

        public static void finish() {
            exporters.shutdown();
            if (getJobCount() > 0) {
                Translate.chat("threaded_export_system_shutdown_with_job", getJobCount());
            } else {
                Translate.chat("threaded_export_system_shutdown");
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

    private record Job(NativeImage image, String name, CompletableFuture<File> future) {}
}
