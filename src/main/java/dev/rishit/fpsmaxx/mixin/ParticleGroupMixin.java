package dev.rishit.fpsmaxx.mixin;

import dev.rishit.fpsmaxx.FPSMaxxConfig;
import dev.rishit.fpsmaxx.FPSMaxxGovernor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The particle-add hook stops new particles outside the cull radius. Particles
 * that were created before the radius shrank (or that drifted away) still tick
 * and occupy memory, so remove them once they are no longer visible. This is
 * cheap and keeps particle-heavy scenes from getting slower over time.
 */
@Mixin(ParticleGroup.class)
public abstract class ParticleGroupMixin {
	@Inject(method = "tickParticle", at = @At("HEAD"), cancellable = true, require = 1)
	private void fpsmaxx$removeFarParticle(Particle particle, CallbackInfo ci) {
		FPSMaxxConfig config = FPSMaxxConfig.get();
		float scale = config.adaptiveParticles ? FPSMaxxGovernor.particleScale() : 1.0F;
		int cullDistance = config.particleCullDistance <= 0
			? 0
			: Math.max(8, Math.round(config.particleCullDistance * scale));
		if (cullDistance <= 0) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		Entity camera = client.getCameraEntity();
		if (camera == null) {
			camera = client.player;
		}
		if (camera == null) {
			return;
		}

		AABB box = particle.getBoundingBox();
		double centerX = (box.minX + box.maxX) * 0.5D;
		double centerY = (box.minY + box.maxY) * 0.5D;
		double centerZ = (box.minZ + box.maxZ) * 0.5D;
		double dx = centerX - camera.getX();
		double dy = centerY - camera.getY();
		double dz = centerZ - camera.getZ();

		if (dx * dx + dy * dy + dz * dz > (double) cullDistance * cullDistance) {
			particle.remove();
			ci.cancel();
		}
	}
}
