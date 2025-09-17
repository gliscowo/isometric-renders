package com.glisco.isometricrenders.render;

import com.glisco.isometricrenders.mixin.access.GameRendererAccessor;
import com.glisco.isometricrenders.property.DefaultPropertyBundle;
import com.glisco.isometricrenders.screen.IsometricUI;
import com.glisco.isometricrenders.util.ExportPathSpec;
import io.wispforest.owo.ui.container.FlowLayout;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.render.*;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.HoveredTooltipPositioner;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4fStack;
import org.joml.Vector2i;

import java.util.List;

public class TooltipRenderable extends DefaultRenderable<TooltipRenderable.TooltipPropertyBundle> {

    private final ItemStack stack;

    public TooltipRenderable(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public void emitVertices(MatrixStack matrices, VertexConsumerProvider vertexConsumers, float tickDelta) {
        var client = MinecraftClient.getInstance();

		var imm = client.getBufferBuilders().getEntityVertexConsumers();
		var state = new GuiRenderState();
		var renderer = new GuiRenderer(state, imm, List.of(
				new EntityGuiElementRenderer(imm, client.getEntityRenderDispatcher()),
				new PlayerSkinGuiElementRenderer(imm),
				new BookModelGuiElementRenderer(imm),
				new BannerResultGuiElementRenderer(imm),
				new SignGuiElementRenderer(imm),
				new ProfilerChartGuiElementRenderer(imm)
		));

	    List<TooltipComponent> list = Screen.getTooltipFromItem(client, this.stack).stream().map(Text::asOrderedText).map(TooltipComponent::of).collect(Util.toArrayList());
	    this.stack.getTooltipData().ifPresent(datax -> list.add(list.isEmpty() ? 0 : 1, TooltipComponent.of(datax)));
	    new DrawContext(client, state).drawTooltipImmediately(client.textRenderer, list, 0, 0,
				(screenWidth, screenHeight, x, y, width, height) -> new Vector2i(HoveredTooltipPositioner.INSTANCE.getPosition(screenWidth, screenHeight, x, y, width, height)).add(-12 - width / 2, 12 - height / 2),
				this.stack.get(DataComponentTypes.TOOLTIP_STYLE));

		renderer.render(((GameRendererAccessor)client.gameRenderer).isometric$getFogRenderer().getFogBuffer(FogRenderer.FogType.NONE));
		renderer.close();
    }

    @Override
    public TooltipPropertyBundle properties() {
        return TooltipPropertyBundle.INSTANCE;
    }

    @Override
    public ExportPathSpec exportPath() {
        return ExportPathSpec.of("tooltip", Registries.ITEM.getId(stack.getItem()).getPath());
    }

    public static class TooltipPropertyBundle extends DefaultPropertyBundle {
        public static final TooltipPropertyBundle INSTANCE = new TooltipPropertyBundle();

        @Override
        public void buildGuiControls(Renderable<?> renderable, FlowLayout container) {
            IsometricUI.sectionHeader(container, "transform_options", false);
            IsometricUI.intControl(container, this.scale, "scale", 10);
        }

        @Override
        public void applyToViewMatrix(Matrix4fStack modelViewStack) {
            final float scale = this.scale.get() / 10000f;
            modelViewStack.scale(scale, scale, -scale);

            modelViewStack.translate(this.xOffset.get() / 260f, this.yOffset.get() / -260f, 0);
            modelViewStack.rotate(RotationAxis.POSITIVE_Y.rotationDegrees(180));
            modelViewStack.rotate(RotationAxis.POSITIVE_Z.rotationDegrees(180));
        }
    }
}
