package com.glisco.isometricrenders.client.gui;

import com.glisco.isometricrenders.mixin.BiomeAccessAccessor;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.Heightmap;
import net.minecraft.world.WorldView;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.light.LightingProvider;
import net.minecraft.world.dimension.DimensionType;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class CachedWorldView implements WorldView {

    private final WorldView delegate;
    private final BiomeAccess biomeAccess;

    private final HashMap<BlockPos, BlockState> stateCache = new HashMap<>();
    private final HashMap<ChunkKey, Chunk> chunkCache = new HashMap<>();

    public CachedWorldView(WorldView delegate) {
        this.delegate = delegate;
        this.biomeAccess = new BiomeAccess(this, BiomeAccess.hashSeed(((BiomeAccessAccessor)delegate.getBiomeAccess()).isometric_getSeed()), delegate.getDimension().getBiomeAccessType());
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        final BlockPos immutableKey = pos.toImmutable();
        if (stateCache.containsKey(immutableKey)) {
            return stateCache.get(immutableKey);
        } else {
            final BlockState state = delegate.getBlockState(pos);
            stateCache.put(immutableKey, state);
            return state;
        }
    }

    @Nullable
    @Override
    public Chunk getChunk(int chunkX, int chunkZ, ChunkStatus leastStatus, boolean create) {
        final ChunkKey key = new ChunkKey(chunkX, chunkZ);
        if (chunkCache.containsKey(key)) {
            return chunkCache.get(key);
        } else {
            final Chunk chunk = delegate.getChunk(chunkX, chunkZ, leastStatus, create);
            chunkCache.put(key, chunk);
            return chunk;
        }
    }

    @Override
    public boolean isChunkLoaded(int chunkX, int chunkZ) {
        return delegate.isChunkLoaded(chunkX, chunkZ);
    }

    @Override
    public int getTopY(Heightmap.Type heightmap, int x, int z) {
        return delegate.getTopY(heightmap, x, z);
    }

    @Override
    public int getAmbientDarkness() {
        return delegate.getAmbientDarkness();
    }

    @Override
    public BiomeAccess getBiomeAccess() {
        return biomeAccess;
    }

    @Override
    public Biome getGeneratorStoredBiome(int biomeX, int biomeY, int biomeZ) {
        return getGeneratorStoredBiome(biomeX, biomeY, biomeZ);
    }

    @Override
    public boolean isClient() {
        return delegate.isClient();
    }

    @Override
    public int getSeaLevel() {
        return delegate.getSeaLevel();
    }

    @Override
    public DimensionType getDimension() {
        return delegate.getDimension();
    }

    @Override
    public float getBrightness(Direction direction, boolean shaded) {
        return delegate.getBrightness(direction, shaded);
    }

    @Override
    public LightingProvider getLightingProvider() {
        return delegate.getLightingProvider();
    }

    @Override
    public WorldBorder getWorldBorder() {
        return delegate.getWorldBorder();
    }

    @Override
    public Stream<VoxelShape> getEntityCollisions(@Nullable Entity entity, Box box, Predicate<Entity> predicate) {
        return delegate.getEntityCollisions(entity, box, predicate);
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        return delegate.getBlockEntity(pos);
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return delegate.getFluidState(pos);
    }

    @Override
    public Biome getBiomeForNoiseGen(int biomeX, int biomeY, int biomeZ) {
        return WorldView.super.getBiomeForNoiseGen(biomeX, biomeY, biomeZ);
    }

    private static class ChunkKey {

        private final int x;
        private final int y;

        public ChunkKey(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ChunkKey)) return false;
            ChunkKey key = (ChunkKey) o;
            return x == key.x && y == key.y;
        }

        @Override
        public int hashCode() {
            int result = x;
            result = 31 * result + y;
            return result;
        }

    }
}
