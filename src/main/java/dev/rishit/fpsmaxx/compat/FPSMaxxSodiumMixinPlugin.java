package dev.rishit.fpsmaxx.compat;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * The Sodium mixins live in their own config so they are never even inspected
 * when Sodium is not installed. That keeps the log clean and avoids optional
 * class-loading surprises in large modpacks.
 */
public final class FPSMaxxSodiumMixinPlugin implements IMixinConfigPlugin {
	@Override
	public void onLoad(String mixinPackage) {
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		return FabricLoader.getInstance().isModLoaded("sodium");
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
	}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(
		String targetClassName,
		ClassNode targetClass,
		String mixinClassName,
		IMixinInfo mixinInfo
	) {
	}

	@Override
	public void postApply(
		String targetClassName,
		ClassNode targetClass,
		String mixinClassName,
		IMixinInfo mixinInfo
	) {
	}
}
