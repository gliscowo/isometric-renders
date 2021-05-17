package com.glisco.isometricrenders.client.export;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.io.File;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

public abstract class ExportMetadata<T> {

    protected final T data;
    protected final Identifier name;

    protected ExportMetadata(Identifier name, T data) {
        this.name = name;
        this.data = data;
    }

    public T getData() {
        return data;
    }

    public abstract File getExportFile(Path baseDir);

    public abstract File getExportDirectory(Path baseDir);

    public void batch() throws IllegalStateException {
        throw new IllegalStateException("This export category does not support batch exporting");
    }

    public static Path getNextFolder(File baseDir) {
        int i = 1;
        while (true) {
            File file = new File(baseDir, String.valueOf(i));
            if (!file.exists()) {
                return file.toPath();
            }
            ++i;
        }
    }

    public static File getNextFile(File baseDir, String filename, String extension) {
        int i = 1;
        while (true) {
            File file = new File(baseDir, filename + (i == 1 ? "" : "_" + i) + extension);
            if (!file.exists()) {
                return file;
            }
            ++i;
        }
    }

    public static abstract class BatchExportMetadata<T> extends ExportMetadata<Iterator<T>> {

        protected T next;
        protected Identifier currentId;

        protected Path exportRoot = null;

        protected BatchExportMetadata(Identifier name, Iterator<T> data) {
            super(name, data);
            next = data.hasNext() ? data.next() : null;
            currentId = next != null ? getCurrentId(next) : null;
        }

        public void setup(Path baseDir) {
            exportRoot = getNextFolder(baseDir.resolve("batches/" + getSubdirectory() + "/").toFile());
        }

        public T next() {
            T nextCopy = next;
            next = data.hasNext() ? data.next() : null;
            return next == null ? null : nextCopy;
        }

        public boolean hasNext() {
            return next != null;
        }

        @Override
        public File getExportFile(Path baseDir) {
            return getNextFile(exportRoot.toFile(), currentId.getPath(), ".png");
        }

        @Override
        public File getExportDirectory(Path baseDir) {
            return exportRoot.toFile();
        }

        protected abstract Identifier getCurrentId(T entry);

        protected abstract String getSubdirectory();
    }

    public static class Block extends ExportMetadata<BlockState> {

        private final Identifier blockId;

        public Block(BlockState state) {
            super(new Identifier(""), state);
            blockId = Registry.BLOCK.getId(data.getBlock());
        }

        @Override
        public File getExportFile(Path baseDir) {
            File subDir = baseDir.resolve(blockId.getNamespace() + "/blocks/").toFile();
            String filename = blockId.getPath();

            return getNextFile(subDir, filename, ".png");
        }

        @Override
        public File getExportDirectory(Path baseDir) {
            return baseDir.resolve(blockId.getNamespace() + "/blocks/").toFile();
        }
    }

    public static class BlockBatch extends BatchExportMetadata<BlockState> {

        public BlockBatch(Iterator<BlockState> states) {
            super(new Identifier(""), states);
        }

        @Override
        protected Identifier getCurrentId(BlockState entry) {
            return Registry.BLOCK.getId(entry.getBlock());
        }

        @Override
        protected String getSubdirectory() {
            return "blocks";
        }

    }

    public static class Item extends ExportMetadata<ItemStack> {

        private final Identifier itemID;

        public Item(ItemStack stack) {
            super(new Identifier(""), stack);
            itemID = Registry.ITEM.getId(stack.getItem());
        }

        @Override
        public File getExportFile(Path baseDir) {
            File subDir = baseDir.resolve(itemID.getNamespace() + "/items/").toFile();
            String filename = itemID.getPath();

            return getNextFile(subDir, filename, ".png");
        }

        @Override
        public File getExportDirectory(Path baseDir) {
            return baseDir.resolve(itemID.getNamespace() + "/items/").toFile();
        }
    }

    public static class ItemBatch extends BatchExportMetadata<ItemStack> {

        public ItemBatch(Iterator<ItemStack> stacks) {
            super(new Identifier(""), stacks);
        }

        @Override
        protected Identifier getCurrentId(ItemStack entry) {
            return Registry.ITEM.getId(entry.getItem());
        }

        @Override
        protected String getSubdirectory() {
            return "items";
        }

    }

    public static class Atlas extends ExportMetadata<List<ItemStack>> {

        public Atlas(String name, List<ItemStack> stacks) {
            super(new Identifier(name), stacks);
        }

        @Override
        public File getExportFile(Path baseDir) {
            File subDir = baseDir.resolve("atlases/").toFile();
            String filename = name.getPath();

            return getNextFile(subDir, filename, ".png");
        }

        @Override
        public File getExportDirectory(Path baseDir) {
            return baseDir.resolve("atlases/").toFile();
        }
    }

    public static class Area extends ExportMetadata<BlockState[][][]> {

        public Area(String name, BlockState[][][] states) {
            super(new Identifier(name), states);
        }

        @Override
        public File getExportFile(Path baseDir) {
            File subDir = baseDir.resolve("areas/").toFile();
            String filename = name.getPath();

            return getNextFile(subDir, filename, ".png");
        }

        @Override
        public File getExportDirectory(Path baseDir) {
            return baseDir.resolve("areas/").toFile();
        }
    }

    public static class EntityData extends ExportMetadata<Entity> {

        private final Identifier entityID;

        public EntityData(Entity entity) {
            super(new Identifier(""), entity);
            entityID = Registry.ENTITY_TYPE.getId(entity.getType());
        }

        @Override
        public File getExportFile(Path baseDir) {
            File subDir = baseDir.resolve(entityID.getNamespace() + "/entities/").toFile();
            String filename = entityID.getPath();

            return getNextFile(subDir, filename, ".png");
        }

        @Override
        public File getExportDirectory(Path baseDir) {
            return baseDir.resolve(entityID.getNamespace() + "/entities/").toFile();
        }
    }

}
