package com.glisco.isometricrenders.compatibility;

import eu.pb4.polymer.core.api.item.PolymerItemGroupUtils;
import eu.pb4.polymer.core.api.item.PolymerItemUtils;
import eu.pb4.polymer.core.impl.client.InternalClientItemGroup;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public interface PolymerSupport {
    boolean HAS_POLYMER_CORE = FabricLoader.getInstance().isModLoaded("polymer-core");

    @Nullable
    static Identifier getPolymerId(ItemStack stack) {
        if (HAS_POLYMER_CORE) {
            return PolymerItemUtils.getServerIdentifier(stack);
        }
        return null;
    }

    @Nullable
    static Identifier getPolymerId(ItemGroup itemGroup) {
        if (HAS_POLYMER_CORE) {
            //noinspection UnstableApiUsage
            if (itemGroup instanceof InternalClientItemGroup internal) {
                //noinspection UnstableApiUsage
                return internal.getIdentifier();
            }
            return PolymerItemGroupUtils.getId(itemGroup);
        }
        return null;
    }
}
