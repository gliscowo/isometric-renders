package com.glisco.isometricrenders.render;

import com.glisco.isometricrenders.property.DefaultPropertyBundle;
import com.glisco.isometricrenders.property.IntProperty;
import com.glisco.isometricrenders.screen.IsometricUI;
import com.glisco.isometricrenders.util.ExportPathSpec;
import com.glisco.isometricrenders.util.ParticleRestriction;
import io.wispforest.owo.ui.container.FlowLayout;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.registry.Registry;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class EntityRenderable extends DefaultRenderable<DefaultPropertyBundle> implements TickingRenderable<DefaultPropertyBundle> {

    private final MinecraftClient client = MinecraftClient.getInstance();
    private final Entity entity;

    public EntityRenderable(Entity entity) {
        this.entity = entity;
    }

    public static EntityRenderable of(EntityType<?> type, @Nullable NbtCompound nbt) {
        final var client = MinecraftClient.getInstance();

        final var entity = type.create(client.world);
        if (nbt != null) entity.readNbt(nbt);
        entity.updatePosition(client.player.getX(), client.player.getY(), client.player.getZ());

        return new EntityRenderable(entity);
    }

    public static EntityRenderable copyOf(Entity source) {
        final var client = MinecraftClient.getInstance();

        final var entity = source.getType().create(client.world);
        entity.copyFrom(source);
        entity.tick();

        return new EntityRenderable(entity);
    }

    @Override
    public void prepare() {
        client.getEntityRenderDispatcher().setRenderShadows(false);
    }

    @Override
    public void emitVertices(MatrixStack matrices, VertexConsumerProvider vertexConsumers, float tickDelta) {
        matrices.push();

        matrices.translate(0, 0.1 + this.entity.getHeight() * -0.5d, 0);
        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(180));

        var properties = this.properties();
        this.entity.setHeadYaw(properties.yaw.get());
        if (entity instanceof LivingEntity living) living.prevHeadYaw = properties.yaw.get();
        this.entity.prevYaw = properties.yaw.get();

        this.entity.setPitch(properties.pitch.get());
        this.entity.prevPitch = properties.pitch.get();

        final MutableFloat y = new MutableFloat();

        applyToEntityAndPassengers(this.entity, entity -> {
            entity.setPos(client.player.getX(), client.player.getY(), client.player.getZ());
            y.add(entity.hasVehicle() ? entity.getVehicle().getMountedHeightOffset() + entity.getHeightOffset() : 0);

            client.getEntityRenderDispatcher().render(entity, 0, y.floatValue(), 0, 0, tickDelta, matrices, vertexConsumers, LightmapTextureManager.MAX_LIGHT_COORDINATE);
        });

        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(-180));
        matrices.translate(0, 1.65, 0);

        client.particleManager.renderParticles(matrices,
                (VertexConsumerProvider.Immediate) vertexConsumers,
                client.gameRenderer.getLightmapTextureManager(),
                getParticleCamera(),
                tickDelta
        );

        matrices.pop();
    }

    @Override
    public void cleanUp() {
        client.getEntityRenderDispatcher().setRenderShadows(true);
    }

    @Override
    public EntityPropertyBundle properties() {
        return EntityPropertyBundle.INSTANCE;
    }

    @Override
    public ParticleRestriction<?> particleRestriction() {
        return ParticleRestriction.duringTick();
    }

    @Override
    public ExportPathSpec exportPath() {
        return ExportPathSpec.ofIdentified(
                Registry.ENTITY_TYPE.getId(this.entity.getType()),
                "entity"
        );
    }

    @Override
    public void tick() {
        applyToEntityAndPassengers(this.entity, entity -> {
            if (entity instanceof PlayerEntity) return;
            client.world.tickEntity(entity);
        });
    }

    private static void applyToEntityAndPassengers(Entity entity, Consumer<Entity> action) {
        action.accept(entity);
        if (entity.getPassengerList().isEmpty()) return;
        for (Entity e : entity.getPassengerList()) applyToEntityAndPassengers(e, action);
    }

    public static class EntityPropertyBundle extends DefaultPropertyBundle {

        public static final EntityPropertyBundle INSTANCE = new EntityPropertyBundle();

        public final IntProperty yaw = IntProperty.of(0, -180, 180).withRollover();
        public final IntProperty pitch = IntProperty.of(0, -90, 90).withRollover();

        private EntityPropertyBundle() {}

        @Override
        public void buildGuiControls(Renderable<?> renderable, FlowLayout container) {
            super.buildGuiControls(renderable, container);

            IsometricUI.sectionHeader(container, "entity_pose", true);

            IsometricUI.intControl(container, yaw, "entity_pose.yaw", 15);
            IsometricUI.intControl(container, pitch, "entity_pose.pitch", 5);
        }
    }
}
