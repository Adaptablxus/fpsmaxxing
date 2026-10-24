package dev.rishit.fpsmaxx;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * Small screen used when Sodium replaces vanilla's Video Settings screen. It
 * hosts the same five-position FpsMaxx slider, so Sodium and Fresh &amp; Smooth
 * users get the same preset picker.
 */
public final class FPSMaxxPresetScreen extends Screen {
	private final Screen parent;

	public FPSMaxxPresetScreen(Screen parent) {
		super(Component.translatable("fpsmaxx.screen.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int centerX = this.width / 2;
		int sliderWidth = Math.min(310, this.width - 40);

		addRenderableWidget(new StringWidget(centerX - 150, 40, 300, 20, this.title, this.font));
		addRenderableWidget(new FPSMaxxGraphicsPresetSlider(
			centerX - sliderWidth / 2,
			this.height / 2 - 10,
			sliderWidth,
			20,
			this.minecraft.options,
			null
		));
		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
			.bounds(centerX - 100, this.height / 2 + 40, 200, 20)
			.build());
	}

	@Override
	public void onClose() {
		this.minecraft.setScreenAndShow(this.parent);
	}
}
