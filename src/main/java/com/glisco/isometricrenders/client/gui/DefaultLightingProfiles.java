package com.glisco.isometricrenders.client.gui;

import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3f;

public class DefaultLightingProfiles {

    public static final LightingProfile FLAT = new FlatLightingProfile();
    public static final LightingProfile DEFAULT_DEPTH_LIGHTING = new DefaultDepthLightingProfile();

    private static class FlatLightingProfile extends LightingProfile {

        @Override
        public void setup() {
            IsometricRenderHelper.setupLighting();
        }
    }

    private static class DefaultDepthLightingProfile extends LightingProfile {
        private static final Matrix4f EXTERNAL_MATRIX;

        static {
            EXTERNAL_MATRIX = new Matrix4f();
            EXTERNAL_MATRIX.loadIdentity();
            EXTERNAL_MATRIX.addToLastColumn(new Vec3f(-1, -1, -1));
        }

        @Override
        public void setup() {
            DiffuseLighting.enableGuiDepthLighting();
        }

        @Override
        public void setupForExternal() {
            DiffuseLighting.enableForLevel(EXTERNAL_MATRIX);
        }
    }

}
