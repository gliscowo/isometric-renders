package com.glisco.isometricrenders.render;

import com.glisco.isometricrenders.util.Translate;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.text.Text;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3f;

public class DefaultLightingProfiles {

    public static final LightingProfile FLAT = new FlatLightingProfile();
    public static final LightingProfile DEFAULT_DEPTH_LIGHTING = new DefaultDepthLightingProfile();

    private static class FlatLightingProfile implements LightingProfile {

        @Override
        public void setup() {
            IsometricRenderHelper.setupLighting();
        }

        @Override
        public Text getFriendlyName() {
            return Translate.gui("lighting_profile.flat");
        }
    }

    private static class DefaultDepthLightingProfile implements LightingProfile {
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
        public Text getFriendlyName() {
            return Translate.gui("lighting_profile.default");
        }

        @Override
        public void setupForExternal() {
            DiffuseLighting.enableForLevel(EXTERNAL_MATRIX);
        }
    }

    public static class UserLightingProfile implements LightingProfile {

        private final Matrix4f matrix;
        private final Vec3f vector;

        public UserLightingProfile(float x, float y, float z) {
            this.matrix = new Matrix4f();
            this.matrix.loadIdentity();
            this.matrix.addToLastColumn(new Vec3f(x, y, z));

            this.vector = new Vec3f(x, y, z);
        }

        @Override
        public void setup() {
            DiffuseLighting.enableForLevel(matrix);
        }

        @Override
        public Text getFriendlyName() {
            return Translate.gui("lighting_profile.custom");
        }

        public Vec3f getVector() {
            return vector;
        }
    }

}
