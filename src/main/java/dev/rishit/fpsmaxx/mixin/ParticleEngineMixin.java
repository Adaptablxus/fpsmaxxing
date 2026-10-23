package dev.rishit.fpsmaxx.mixin;

import dev.rishit.fpsmaxx.FPSMaxxConfig;
import dev.rishit.fpsmaxx.FPSMaxxGovernor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Queue;

/**
 * Particles are cheap individually but add up quickly during rain, fires,
 * explosions and large farms. Rejecting far-away and over-budget particles
 * before they enter the tick/render queues keeps frame time stable on a weak
 * CPU without touching gameplay logic.
 */
@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {
	@Shadow
	private Map<ParticleRenderType, ParticleGroup<?>> particles;

	@Shadow
	private Queue<Particle> particlesToAdd;

	@Inject(method = "add", at = @At("HEAD"), cancellable = true)
	private void fpsmaxx$limitParticles(Particle particle, CallbackInfo ci) {
		FPSMaxxConfig config = FPSMaxxConfig.get();
		Minecraft client = Minecraft.getInstance();
		Entity camera = client.getCameraEntity();
		if (camera == null) {
			camera = client.player;
		}
		if (camera == null) {
			return;
		}

		float scale = config.adaptiveParticles ? FPSMaxxGovernor.particleScale() : 1.0F;
		int cullDistance = config.particleCullDistance <= 0
			? 0
			: Math.max(8, Math.round(config.particleCullDistance * scale));
		int particleCap = config.maxParticles <= 0
			? 0
			: Math.max(100, Math.round(config.maxParticles * scale));

		Vec3 center = particle.getBoundingBox().getCenter();
		double dx = center.x - camera.getX();
		double dy = center.y - camera.getY();
		double dz = center.z - camera.getZ();
		double distanceSq = dx * dx + dy * dy + dz * dz;

		if (cullDistance > 0 && distanceSq > (double) cullDistance * cullDistance) {
			ci.cancel();
			return;
		}

		if (particleCap <= 0 || distanceSq <= 64.0D) {
			return;
		}

		int live = this.particlesToAdd.size();
		for (ParticleGroup<?> group : this.particles.values()) {
			live += group.size();
		}
		if (live >= particleCap) {
			ci.cancel();
		}
	}
}
