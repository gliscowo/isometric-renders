package com.glisco.isometricrenders.client;

import com.glisco.isometricrenders.client.gui.IsometricRenderHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.texture.NativeImage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ImageExportThread extends Thread {

    private static ImageExportThread INSTANCE = null;

    private static final List<NativeImage> jobs = new ArrayList<>();
    private static NativeImage currentJob = null;
    private static boolean doExport = true;

    public static void init() {
        if (INSTANCE != null) throw new IllegalStateException("Export Thread is already running!");
        INSTANCE = new ImageExportThread();
        INSTANCE.setName("Image Export Thread");
        INSTANCE.start();
    }

    public static boolean acceptsJobs() {
        return jobs.size() < 10;
    }

    public static void addJob(NativeImage image) {
        if (!acceptsJobs()) return;
        jobs.add(image);
    }

    public static int getJobCount() {
        return jobs.size();
    }

    public static void clearQueue() {
        jobs.clear();
    }

    public static void enableExporting() {
        doExport = true;
    }

    public static void disableExporting() {
        doExport = false;
    }

    public static boolean exportingEnabled() {
        return doExport;
    }

    public static boolean currentlyExporting() {
        return currentJob != null;
    }

    @Override
    public void run() {
        while (true) {

            while (!jobs.isEmpty() && doExport) {
                try {

                    currentJob = jobs.get(0);
                    jobs.remove(0);

                    File file = IsometricRenderHelper.getScreenshotFilename(FabricLoader.getInstance().getGameDir().resolve("screenshots").toFile());

                    IsometricRendersClient.LOGGER.info("Started saving image: {}", file);
                    currentJob.writeFile(file);
                    IsometricRendersClient.LOGGER.info("Finished");

                    currentJob = null;

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }
    }
}
