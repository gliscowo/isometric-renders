package com.glisco.isometricrenders.util;

import com.glisco.isometricrenders.IsometricRenders;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;


public class SystemClipboard {
    public static void copyImageFile (String path) {
        if (MinecraftClient.IS_SYSTEM_MAC) {
            MacOSClipboard.copyFile(path);
        } else {
            final ImageTransferable transferable;
            try {
                transferable = new ImageTransferable(javax.imageio.ImageIO.read(new File(path)));
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(transferable, transferable);
            } catch (IOException e) {
                IsometricRenders.LOGGER.error("Error putting file in clipboard", e);
            }
        }
    }

    public static void copyNativeImage (NativeImage image) {
        try {
            if (MinecraftClient.IS_SYSTEM_MAC) {
                // On macOS, we have to save the image to a file and then have it copied from the FS.
                ImageIO.save(
                    image,
                    new ExportPathSpec(
                        Files.createTempDirectory("IsometricRendersMod").toString(),
                        "render",
                        true
                    )
                ).whenComplete((file, throwable) -> {
                    if (throwable != null) {
                        IsometricRenders.LOGGER.error("Error writing image to temp file", throwable);
                        return;
                    }
                    MacOSClipboard.copyFile(file.getAbsolutePath());
                    file.deleteOnExit();
                });
            } else {
                final ImageTransferable transferable = new ImageTransferable(javax.imageio.ImageIO.read(new ByteArrayInputStream(image.getBytes())));
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(transferable, transferable);
            }
        } catch (IOException e) {
            IsometricRenders.LOGGER.error("Error putting image in clipboard", e);
        }
    }
}
