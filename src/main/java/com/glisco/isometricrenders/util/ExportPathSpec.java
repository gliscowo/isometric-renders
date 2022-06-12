package com.glisco.isometricrenders.util;

import com.glisco.isometricrenders.property.GlobalProperties;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;

import java.io.File;
import java.nio.file.Path;

public record ExportPathSpec(String rootOffset, String filename, boolean ignoreSaveIntoRoot) {

    public static ExportPathSpec of(String rootOffset, String filename) {
        return new ExportPathSpec(rootOffset, filename, false);
    }

    public static ExportPathSpec forced(String rootOffset, String filename) {
        return new ExportPathSpec(rootOffset, filename, true);
    }

    public static ExportPathSpec ofIdentified(Identifier id, String type) {
        return new ExportPathSpec(id.getNamespace() + "/" + type, id.getPath(), false);
    }

    // -----

    public Path resolveOffset() {
        return exportRoot().resolve(this.effectiveOffset());
    }

    public File resolveFile(String extension) {
        return ImageIO.next(exportRoot().resolve(this.effectiveOffset()).resolve(this.filename + "." + extension)).toFile();
    }

    public ExportPathSpec relocate(String newOffset) {
        return new ExportPathSpec(newOffset, this.filename, this.ignoreSaveIntoRoot);
    }

    private String effectiveOffset() {
        return rootOffset.isEmpty() || (!this.ignoreSaveIntoRoot && GlobalProperties.saveIntoRoot.get()) ? "./" : rootOffset + "/";
    }

    // -----

    public static Path exportRoot() {
        return FabricLoader.getInstance().getGameDir().resolve("renders");
    }
}
