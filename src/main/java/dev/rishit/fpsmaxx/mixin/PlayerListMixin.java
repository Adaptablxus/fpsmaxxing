package dev.rishit.fpsmaxx.mixin;

import dev.rishit.fpsmaxx.FPSMaxxConstants;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * The server broadcasts its view distance to the client before ChunkMap clamps
 * it. If the memory guard lowers the loaded distance, the packet must carry the
 * same value or the client would expect chunks the server never sends.
 */
@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
	@ModifyVariable(
		method = "setViewDistance",
		at = @At("HEAD"),
		argsOnly = true,
		index = 1
	)
	private int fpsmaxx$capBroadcastViewDistance(int viewDistance) {
		return Math.min(viewDistance, FPSMaxxConstants.maxSafeRenderDistance());
	}
}
