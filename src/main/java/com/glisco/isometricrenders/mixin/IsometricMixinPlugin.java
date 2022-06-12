package com.glisco.isometricrenders.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.awt.*;
import java.util.List;
import java.util.Set;

public class IsometricMixinPlugin implements IMixinConfigPlugin {

    static {
        // We force-initialize AWT here so that we can copy images
        // into the clipboard later. MC specifically enables AWT
        // headless mode which would prevent that
        if (!GraphicsEnvironment.isHeadless()) {
            Toolkit.getDefaultToolkit().getSystemClipboard();
        }
    }

    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
