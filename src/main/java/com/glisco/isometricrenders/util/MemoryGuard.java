package com.glisco.isometricrenders.util;

import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.opengl.ATIMeminfo;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.NVXGPUMemoryInfo;

import java.util.ArrayList;
import java.util.List;

public class MemoryGuard {

    private final float maximumLoadFactor;
    private int availableVramMB = 0;
    private int availableRamMB = 0;

    public MemoryGuard(float maximumLoadFactor) {
        this.maximumLoadFactor = maximumLoadFactor;
    }

    public void update() {
        var data = new int[4];
        var caps = GL.getCapabilities();

        if (caps.GL_ATI_meminfo) GL11.glGetIntegerv(ATIMeminfo.GL_TEXTURE_FREE_MEMORY_ATI, data);
        if (caps.GL_NVX_gpu_memory_info) GL11.glGetIntegerv(NVXGPUMemoryInfo.GL_GPU_MEMORY_INFO_CURRENT_AVAILABLE_VIDMEM_NVX, data);
        GL11.glGetError();

        this.availableVramMB = data[0] / 1024;
        this.availableRamMB = (int) ((Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory() + Runtime.getRuntime().freeMemory()) / 1024 / 1024);
    }

    public boolean isSupported() {
        return (GL.getCapabilities().GL_ATI_meminfo || GL.getCapabilities().GL_NVX_gpu_memory_info);
    }

    public List<Text> getStatusTooltip(int memoryMB) {
        final var tooltip = new ArrayList<Text>();

        tooltip.add(this.usageText("vram", memoryMB, this.availableVramMB(), this.canFitInVram(memoryMB)));
        tooltip.add(this.usageText("ram", memoryMB, this.availableRamMB(), this.canFitInRam(memoryMB)));

        if (!this.isSupported()) {
            tooltip.add(Text.of(" "));
            tooltip.add(Translate.gui("no_vram_info_warning").formatted(Formatting.YELLOW));
        }

        if (!this.canFit(memoryMB)) {
            tooltip.add(Translate.gui("vram_ignore").formatted(Formatting.GRAY));
        }

        return tooltip;
    }

    private MutableText usageText(String key, int usage, int available, boolean fits) {
        if (available == 0) available = 1;

        if (fits) {
            return Translate.gui(
                    key,
                    new LiteralText(usage + "").formatted(Formatting.GRAY),
                    new LiteralText(available + "").formatted(Formatting.GRAY),
                    new LiteralText(usage * 100 / available + "%").formatted(Formatting.GRAY)
            );
        } else {
            return Translate.gui(
                    key,
                    new LiteralText(usage + "").formatted(Formatting.RED),
                    new LiteralText(available + "").formatted(Formatting.GRAY),
                    new LiteralText(usage * 100 / available + "%").formatted(Formatting.RED)
            );
        }
    }

    public int availableVramMB() {
        return this.availableVramMB;
    }

    public int availableRamMB() {
        return this.availableRamMB;
    }

    public boolean canFit(int memoryMB) {
        return canFitInVram(memoryMB) && canFitInRam(memoryMB);
    }

    public boolean canFitInRam(int memoryMB) {
        return memoryMB / (float) this.availableRamMB <= this.maximumLoadFactor;
    }

    public boolean canFitInVram(int memoryMB) {
        return memoryMB / (float) this.availableVramMB <= this.maximumLoadFactor;
    }

}
