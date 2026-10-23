package dev.rishit.fpsmaxx.mixin;

import dev.rishit.fpsmaxx.FPSMaxxConstants;
import net.minecraft.client.server.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * The integrated server compares the client's requested render distance with
 * the value it previously sent. The player-list mixin caps the value that is
 * actually stored, so without this the two never match and the server logs
 * "Changing view distance" every tick. Cap the local value too.
 */
@Mixin(IntegratedServer.class)
public abstract class IntegratedServerMixin {
	@ModifyArg(
		method = "tickServer",
		at = @At(
			value = "INVOKE",
			target = "Ljava/lang/Math;max(II)I",
			ordinal = 0
		),
		index = 1
	)
	private int fpsmaxx$capIntegratedViewDistance(int renderDistance) {
		return Math.min(renderDistance, FPSMaxxConstants.maxSafeRenderDistance());
	}
}
