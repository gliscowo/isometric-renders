package com.glisco.isometricrenders.render;

import com.glisco.isometricrenders.IsometricRenders;
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
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.storage.NbtWriteView;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;

public class EntityRenderable extends DefaultRenderable<DefaultPropertyBundle> implements TickingRenderable<DefaultPropertyBundle> {

    private final MinecraftClient client = MinecraftClient.getInstance();
    private final Entity entity;

    public EntityRenderable(Entity entity) {
        this.entity = entity;
    }

    public static EntityRenderable of(EntityType<?> type, @Nullable NbtCompound nbt) {
        final var client = MinecraftClient.getInstance();

        if (nbt == null) {
            nbt = new NbtCompound();
        }

        nbt.putString("id", EntityType.getId(type).toString());

        final var entity = EntityType.loadEntityWithPassengers(nbt, client.world, SpawnReason.LOAD, Function.identity());
        entity.updatePosition(client.player.getX(), client.player.getY(), client.player.getZ());

        return new EntityRenderable(entity);
    }

    public static EntityRenderable copyOf(Entity source) {
        final var client = MinecraftClient.getInstance();

		var logging = new ErrorReporter.Logging(source.getErrorReporterContext(), IsometricRenders.LOGGER);
	    var view = NbtWriteView.create(logging, source.getRegistryManager());
		source.writeData(view);
		var nbt = view.getNbt();
	    logging.close();
		nbt.putString("id", EntityType.getId(source.getType()).toString());


        final var entity = EntityType.loadEntityWithPassengers(nbt, client.world, SpawnReason.LOAD, Function.identity());
        applyToEntityAndPassengers(entity, Entity::tick);

        return new EntityRenderable(entity);
    }

    @Override
    public void prepare() {
        client.getEntityRenderDispatcher().setRenderShadows(false);
    }

    @Override
    public void emitVertices(MatrixStack matrices, VertexConsumerProvider vertexConsumers, float tickDelta) {
        matrices.push();

        matrices.translate(0, -.5 * this.entity.getHeight(), 0);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180));

        var properties = this.properties();
        this.entity.setHeadYaw(properties.yaw.get());
        if (entity instanceof LivingEntity living) living.lastHeadYaw = properties.yaw.get();
        this.entity.lastYaw = properties.yaw.get();

        this.entity.setPitch(properties.pitch.get());
        this.entity.lastPitch = properties.pitch.get();

        final MutableObject<Vec3d> offset = new MutableObject<>(Vec3d.ZERO);

        applyToEntityAndPassengers(this.entity, entity -> {
            entity.setPos(client.player.getX(), client.player.getY(), client.player.getZ());
            if (entity.hasVehicle()) {
                offset.setValue(offset.getValue().add(entity.getVehicle().getPassengerRidingPos(entity).subtract(entity.getPos())));
            }

            var offsetPos = offset.getValue();
            matrices.push();
            client.getEntityRenderDispatcher().render(entity, offsetPos.getX(), offsetPos.getY(), offsetPos.getZ(), tickDelta, matrices, vertexConsumers, LightmapTextureManager.MAX_LIGHT_COORDINATE);
            matrices.pop();
        });

        if (vertexConsumers instanceof VertexConsumerProvider.Immediate immediate) {
            immediate.draw();
        }

        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-180));
        matrices.translate(0, 1.65, 0);

        this.renderParticles(matrices.peek().getPositionMatrix(), tickDelta);

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
                Registries.ENTITY_TYPE.getId(this.entity.getType()),
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
