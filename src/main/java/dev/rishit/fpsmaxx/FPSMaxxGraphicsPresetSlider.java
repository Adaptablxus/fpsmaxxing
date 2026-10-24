package dev.rishit.fpsmaxx;

import net.minecraft.client.GraphicsPreset;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

/**
 * Replaces vanilla's graphics-preset slider with one extra position on the far
 * left: <b>FpsMaxx</b>. The vanilla {@link GraphicsPreset} enum is fixed, so
 * the hidden state is stored in {@link FPSMaxxConfig#graphicsPresetFpsMaxx}
 * instead of adding an enum constant.
 *
 * <p>The slider order is FpsMaxx, Fast, Fancy, Fabulous, Custom. Selecting
 * FpsMaxx applies the complete low-end profile, including the mod-side culling
 * and chunk-budget settings that vanilla presets cannot reach.</p>
 */
public final class FPSMaxxGraphicsPresetSlider extends AbstractSliderButton {
	private static final int POSITION_FPSMAXX = 0;
	private static final int POSITION_FAST = 1;
	private static final int POSITION_FANCY = 2;
	private static final int POSITION_FABULOUS = 3;
	private static final int POSITION_CUSTOM = 4;
	private static final int POSITIONS = 5;

	private final Options options;
	private final OptionsSubScreen screen;
	private int lastIndex;

	public FPSMaxxGraphicsPresetSlider(
		int x,
		int y,
		int width,
		int height,
		Options options,
		OptionsSubScreen screen
	) {
		super(x, y, width, height, Component.empty(), sliderValueFor(indexFrom(options)));
		this.options = options;
		this.screen = screen;
		this.lastIndex = indexFrom(options);
		updateMessage();
	}

	private static int indexFrom(Options options) {
		if (FPSMaxxConfig.get().graphicsPresetFpsMaxx) {
			return POSITION_FPSMAXX;
		}

		return switch (options.graphicsPreset().get()) {
			case FAST -> POSITION_FAST;
			case FANCY -> POSITION_FANCY;
			case FABULOUS -> POSITION_FABULOUS;
			case CUSTOM -> POSITION_CUSTOM;
		};
	}

	private static double sliderValueFor(int index) {
		return index / (double) (POSITIONS - 1);
	}

	private int sliderIndex() {
		int index = (int) Math.round(this.value * (POSITIONS - 1));
		return Math.max(POSITION_FPSMAXX, Math.min(POSITION_CUSTOM, index));
	}

	@Override
	protected void updateMessage() {
		int index = sliderIndex();
		setMessage(Component.translatable(labelKey(index)));
		if (index == POSITION_FPSMAXX) {
			String tooltipKey = FPSMaxxPresets.useFreshSmoothProfile()
				? "options.graphics.fpsmaxx.tooltip.freshsmooth"
				: "options.graphics.fpsmaxx.tooltip";
			setTooltip(Tooltip.create(Component.translatable(tooltipKey)));
		} else {
			setTooltip(Tooltip.create(Component.translatable("options.graphics.preset.tooltip")));
		}
	}

	@Override
	protected void applyValue() {
		int index = sliderIndex();

		// Snap the handle to one of the five positions instead of letting it
		// stop between presets.
		this.value = sliderValueFor(index);
		if (index == this.lastIndex) {
			return;
		}
		this.lastIndex = index;

		Minecraft client = Minecraft.getInstance();
		switch (index) {
			case POSITION_FPSMAXX -> FPSMaxxPresets.applyFpsMaxx(client, this.screen);
			case POSITION_FAST -> selectVanilla(client, GraphicsPreset.FAST);
			case POSITION_FANCY -> selectVanilla(client, GraphicsPreset.FANCY);
			case POSITION_FABULOUS -> selectVanilla(client, GraphicsPreset.FABULOUS);
			default -> selectVanilla(client, GraphicsPreset.CUSTOM);
		}
	}

	private void selectVanilla(Minecraft client, GraphicsPreset preset) {
		// Let the vanilla preset apply first so resetModProfile can capture the
		// simulation distance the player just selected.
		this.options.graphicsPreset().set(preset);
		this.options.save();
		FPSMaxxPresets.resetModProfile(client);
	}

	private static String labelKey(int index) {
		return switch (index) {
			case POSITION_FPSMAXX -> "options.graphics.fpsmaxx";
			case POSITION_FAST -> "options.graphics.fast";
			case POSITION_FANCY -> "options.graphics.fancy";
			case POSITION_FABULOUS -> "options.graphics.fabulous";
			default -> "options.graphics.custom";
		};
	}
}
