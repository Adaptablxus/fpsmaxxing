package dev.rishit.fpsmaxx.mixin;

import dev.rishit.fpsmaxx.FPSMaxxGraphicsPresetSlider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces the vanilla graphics-preset slider with
 * {@link FPSMaxxGraphicsPresetSlider}. Injecting into {@code addBig} instead of
 * {@code VideoSettingsScreen} keeps the change working if another mod also
 * adds options to that screen.
 */
@Mixin(OptionsList.class)
public abstract class OptionsListMixin {
	@Shadow
	@Final
	private OptionsSubScreen screen;

	@Inject(
		method = "addBig(Lnet/minecraft/client/OptionInstance;)V",
		at = @At("HEAD"),
		cancellable = true
	)
	private void fpsmaxx$replaceGraphicsPreset(OptionInstance<?> option, CallbackInfo ci) {
		Minecraft client = Minecraft.getInstance();
		if (option != client.options.graphicsPreset()) {
			return;
		}

		((OptionsList) (Object) this).addBig(new FPSMaxxGraphicsPresetSlider(
			0,
			0,
			310,
			20,
			client.options,
			this.screen
		));
		ci.cancel();
	}
}
