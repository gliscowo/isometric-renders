package com.glisco.isometricrenders.property;

import com.glisco.isometricrenders.render.Renderable;
import com.glisco.isometricrenders.util.ClientRenderCallback;
import com.glisco.isometricrenders.widget.WidgetColumnBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3f;

public class DefaultPropertyBundle implements PropertyBundle {

    private static final DefaultPropertyBundle INSTANCE = new DefaultPropertyBundle();

    public final IntProperty scale = IntProperty.of(100, 0, 500);
    public final IntProperty rotation = IntProperty.of(135, 0, 360).withRollover();
    public final IntProperty slant = IntProperty.of(30, -90, 90);
    public final IntProperty lightAngle = IntProperty.of(-45, -45, 45);

    public final IntProperty xOffset = IntProperty.of(0, Integer.MIN_VALUE / 2, Integer.MAX_VALUE / 2);
    public final IntProperty yOffset = IntProperty.of(0, Integer.MIN_VALUE / 2, Integer.MAX_VALUE / 2);

    public final IntProperty rotationSpeed = IntProperty.of(0, 0, 100);
    protected float rotationOffset = 0;
    protected boolean rotationOffsetUpdated = false;

    public DefaultPropertyBundle() {
        ClientRenderCallback.EVENT.register(client -> {
            this.rotationOffsetUpdated = false;
        });
    }

    @Override
    public void buildGuiControls(Renderable<?> renderable, WidgetColumnBuilder builder) {
        builder.label("transform_options");

        this.appendIntControls(builder, scale, "scale", 10);
        this.appendIntControls(builder, rotation, "rotation", 45);
        this.appendIntControls(builder, slant, "slant", 30);
        this.appendIntControls(builder, lightAngle, "light_angle", 15);
        this.appendIntControls(builder, rotationSpeed, "rotation_speed", 5);

        // -------

        builder.move(10);
        builder.label("presets");

        builder.button("dimetric", 0, 60, button -> {
            this.rotation.setToDefault();
            this.slant.set(30);
        });
        builder.button("isometric", 65, 60, button -> {
            this.rotation.setToDefault();
            this.slant.set(36);
        });
        builder.nextRow();
    }

    @Override
    public void applyToViewMatrix(MatrixStack modelViewStack) {
        final float scale = this.scale.get() / 100f;
        modelViewStack.scale(scale, scale, scale);

        modelViewStack.translate(this.xOffset.get() / 26000d, this.yOffset.get() / -26000d, 0);

        modelViewStack.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(this.slant.get()));
        modelViewStack.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(this.rotation.get()));

        this.updateAndApplyRotationOffset(modelViewStack);
    }

    public float rotationOffset() {
        return this.rotationOffset;
    }

    protected void updateAndApplyRotationOffset(MatrixStack modelViewStack) {
        if (rotationSpeed.get() != 0) {
            if (!this.rotationOffsetUpdated) {
                rotationOffset += MinecraftClient.getInstance().getLastFrameDuration() * rotationSpeed.get() * .1f;
                this.rotationOffsetUpdated = true;
            }
            modelViewStack.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(rotationOffset));
        } else {
            rotationOffset = 0;
        }
    }

    public static DefaultPropertyBundle get() {
        return INSTANCE;
    }
}
