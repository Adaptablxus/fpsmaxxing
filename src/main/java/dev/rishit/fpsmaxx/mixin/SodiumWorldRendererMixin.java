package dev.rishit.fpsmaxx.mixin;

import dev.rishit.fpsmaxx.FPSMaxxConfig;
import dev.rishit.fpsmaxx.FPSMaxxGovernor;
import dev.rishit.fpsmaxx.compat.FPSMaxxCompat;
import net.caffeinemc.mods.sodium.client.util.FogParameters;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Sodium builds its own fog parameters. Cap them to the adaptive radius so the
 * reduced view distance does not expose a hard world edge.
 */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer")
public abstract class SodiumWorldRendererMixin {
	@ModifyVariable(
		method = "setupTerrain",
		at = @At("HEAD"),
		argsOnly = true,
		index = 3,
		remap = false,
		require = 0
	)
	private FogParameters fpsmaxx$capSodiumFog(FogParameters fog) {
		// Iris owns the fog when a shader pack is active; leave its parameters
		// untouched so the pack's atmosphere is preserved.
		if (FPSMaxxCompat.IRIS) {
			return fog;
		}

		FPSMaxxCompat.markSodiumFogHook();

		FPSMaxxConfig config = FPSMaxxConfig.get();
		if (!config.matchFogToVisibleDistance
			|| fog == null
			|| fog == FogParameters.NONE
			|| fog.renderEnd() <= 0.0F
			|| !Float.isFinite(fog.renderEnd())) {
			return fog;
		}

		int cap = FPSMaxxGovernor.visibleDistanceChunks();
		if (cap <= 0) {
			return fog;
		}

		float capBlocks = cap * 16.0F;
		return new FogParameters(
			fog.red(),
			fog.green(),
			fog.blue(),
			fog.alpha(),
			fog.environmentalStart(),
			Math.min(fog.environmentalEnd(), capBlocks),
			fog.renderStart(),
			Math.min(fog.renderEnd(), capBlocks),
			Math.min(fog.cullDistance(), capBlocks)
		);
	}
}
