package dev.rishit.fpsmaxx.mixin;

import dev.rishit.fpsmaxx.FPSMaxxConstants;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Vanilla clamps the integrated server's view distance to 32 while it generates
 * chunks. Raise that clamp to 64 so singleplayer actually sends the chunks the
 * client asks for.
 */
@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin {
	@ModifyArg(
		method = "setServerViewDistance",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(III)I"),
		index = 2
	)
	private int fpsmaxx$raiseServerViewDistanceClamp(int vanillaMaximum) {
		return FPSMaxxConstants.maxSafeRenderDistance();
	}
}
