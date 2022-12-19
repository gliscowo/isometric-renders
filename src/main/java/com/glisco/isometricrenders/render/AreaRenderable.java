package com.glisco.isometricrenders.render;

import com.glisco.isometricrenders.property.DefaultPropertyBundle;
import com.glisco.isometricrenders.property.Property;
import com.glisco.isometricrenders.screen.IsometricUI;
import com.glisco.isometricrenders.util.ExportPathSpec;
import com.glisco.isometricrenders.util.ParticleRestriction;
import com.glisco.isometricrenders.util.Translate;
import com.mojang.blaze3d.systems.RenderSystem;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.worldmesher.WorldMesh;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class AreaRenderable extends DefaultRenderable<AreaRenderable.AreaPropertyBundle> {

    private final MinecraftClient client = MinecraftClient.getInstance();

    private final WorldMesh mesh;
    private final int ySize;
    private final int xSize;
    private final int zSize;

    public AreaRenderable(WorldMesh mesh) {
        this.mesh = mesh;

        final var dimensions = mesh.dimensions();
        this.xSize = (int) dimensions.getXLength() + 1;
        this.ySize = (int) dimensions.getYLength() + 1;
        this.zSize = (int) dimensions.getZLength() + 1;
    }

    public static AreaRenderable of(BlockPos origin, BlockPos end) {
        final WorldMesh.Builder builder = new WorldMesh.Builder(MinecraftClient.getInstance().world, origin, end);
        if (AreaPropertyBundle.INSTANCE.freezeEntities.get()) {
            builder.freezeEntities();
        }
        return new AreaRenderable(builder.build());
    }

    @Override
    public void emitVertices(MatrixStack matrices, VertexConsumerProvider vertexConsumers, float tickDelta) {
        if (!mesh.canRender()) {
            mesh.scheduleRebuild();
            return;
        }

        matrices.loadIdentity();
        matrices.translate(-xSize / 2f, -ySize / 2f, -zSize / 2f);

        final var blockEntities = mesh.getRenderInfo().getBlockEntities();
        blockEntities.forEach((blockPos, entity) -> {
            matrices.push();
            matrices.translate(blockPos.getX(), blockPos.getY(), blockPos.getZ());
            client.getBlockEntityRenderDispatcher().render(entity, 0, matrices, vertexConsumers);
            matrices.pop();
        });


        super.draw(RenderSystem.getModelViewMatrix());

        final var effectiveDelta = mesh.entitiesFrozen() ? 0 : client.getTickDelta();
        final var entities = mesh.getRenderInfo().getEntities();
        entities.forEach((vec3d, entry) -> {
            if (!mesh.entitiesFrozen()) {
                vec3d = entry.entity().getLerpedPos(effectiveDelta).subtract(mesh.startPos().getX(), mesh.startPos().getY(), mesh.startPos().getZ());
            }

            client.getEntityRenderDispatcher().render(entry.entity(), vec3d.x, vec3d.y, vec3d.z, entry.entity().getYaw(effectiveDelta), effectiveDelta, matrices, vertexConsumers, entry.light());
            super.draw(RenderSystem.getModelViewMatrix());
        });

        var diff = Vec3d.of(mesh.startPos()).subtract(client.player.getPos());
        matrices.translate(-diff.x, -diff.y + 1.65, -diff.z);

        client.particleManager.renderParticles(matrices,
                (VertexConsumerProvider.Immediate) vertexConsumers,
                client.gameRenderer.getLightmapTextureManager(),
                getParticleCamera(),
                tickDelta
        );
    }

    @Override
    public void draw(Matrix4f modelViewMatrix) {
        if (!mesh.canRender()) return;

        super.draw(modelViewMatrix);

        final var meshStack = new MatrixStack();
        meshStack.multiplyPositionMatrix(modelViewMatrix);
        meshStack.translate(-xSize / 2f, -ySize / 2f, -zSize / 2f);
        this.mesh.render(meshStack);
    }

    @Override
    public AreaPropertyBundle properties() {
        return AreaPropertyBundle.INSTANCE;
    }

    @Override
    public ParticleRestriction<?> particleRestriction() {
        final var dimensions = this.mesh.dimensions();
        return ParticleRestriction.inArea(new Box(
                dimensions.minX,
                dimensions.minY,
                dimensions.minZ,
                dimensions.maxX + 1,
                dimensions.maxY + 1,
                dimensions.maxZ + 1
        ));
    }

    @Override
    public ExportPathSpec exportPath() {
        return ExportPathSpec.of("area_renders", "area_render");
    }

    public static class AreaPropertyBundle extends DefaultPropertyBundle {

        private static final AreaPropertyBundle INSTANCE = new AreaPropertyBundle();

        public final Property<Boolean> freezeEntities = Property.of(true);

        @Override
        public void buildGuiControls(Renderable<?> renderable, FlowLayout container) {
            super.buildGuiControls(renderable, container);
            final var mesh = ((AreaRenderable) renderable).mesh;

            IsometricUI.sectionHeader(container, "mesh_controls", true);

            IsometricUI.dynamicLabel(container, () -> {
                var meshStatus = Translate.gui("mesh_status");
                if (!mesh.getState().isBuildStage) {
                    meshStatus.append(Translate.gui("mesh_ready").formatted(Formatting.GREEN));
                } else {
                    meshStatus.append(Translate.gui(
                            mesh.getState() == WorldMesh.MeshState.BUILDING ? "mesh_building" : "mesh_rebuilding",
                            (int) (mesh.getBuildProgress() * 100)
                    ).formatted(Formatting.RED));
                }
                return meshStatus;
            });

            IsometricUI.booleanControl(container, this.freezeEntities, "freeze_entities");
            this.freezeEntities.listen((booleanProperty, aBoolean) -> {
                mesh.setFreezeEntities(aBoolean);
            });

            container.child(Components.button(Translate.gui("rebuild_mesh"), (ButtonComponent button) -> mesh.scheduleRebuild())
                    .horizontalSizing(Sizing.fixed(80))
                    .margins(Insets.top(5)));
        }

        @Override
        public void applyToViewMatrix(MatrixStack modelViewStack) {
            final float scale = this.scale.get() / 1000f;
            modelViewStack.scale(scale, scale, scale);

            modelViewStack.translate(this.xOffset.get() / 2600d, this.yOffset.get() / -2600d, 0);

            modelViewStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(this.slant.get()));
            modelViewStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(this.rotation.get()));

            this.updateAndApplyRotationOffset(modelViewStack);
        }
    }
}
