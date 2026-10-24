package dev.rishit.fpsmaxx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Small loader bridge used by code that must run on both Fabric and NeoForge.
 * Reflection keeps the shared classes free of compile-time loader dependencies.
 */
public final class FPSMaxxPlatform {
	private static final Logger LOGGER = LoggerFactory.getLogger(FPSMaxxConstants.MOD_ID);
	private static final String LOADER = detectLoader();

	private FPSMaxxPlatform() {
	}

	public static String loader() {
		return LOADER;
	}

	public static boolean isFabric() {
		return "fabric".equals(LOADER);
	}

	public static boolean isNeoForge() {
		return "neoforge".equals(LOADER);
	}

	public static Path configDir() {
		try {
			if (isFabric()) {
				Object loader = Class.forName("net.fabricmc.loader.api.FabricLoader")
					.getMethod("getInstance")
					.invoke(null);
				return (Path) loader.getClass().getMethod("getConfigDir").invoke(loader);
			}
			if (isNeoForge()) {
				Object configured = Class.forName("net.neoforged.fml.loading.FMLPaths")
					.getField("CONFIGDIR")
					.get(null);
				try {
					Method get = configured.getClass().getMethod("get");
					return (Path) get.invoke(configured);
				} catch (NoSuchMethodException ignored) {
					return (Path) configured;
				}
			}
		} catch (ReflectiveOperationException | ClassCastException exception) {
			LOGGER.warn("Could not resolve the config directory for {}; using ./config", LOADER, exception);
		}
		return Path.of("config");
	}

	public static boolean isModLoaded(String modId) {
		try {
			if (isFabric()) {
				Object loader = Class.forName("net.fabricmc.loader.api.FabricLoader")
					.getMethod("getInstance")
					.invoke(null);
				return (boolean) loader.getClass().getMethod("isModLoaded", String.class).invoke(loader, modId);
			}
			if (isNeoForge()) {
				Object modList = Class.forName("net.neoforged.fml.ModList")
					.getMethod("get")
					.invoke(null);
				if (modList == null) {
					return false;
				}
				return (boolean) modList.getClass().getMethod("isLoaded", String.class).invoke(modList, modId);
			}
		} catch (ReflectiveOperationException exception) {
			LOGGER.debug("Could not check whether {} is loaded", modId, exception);
		}
		return false;
	}

	public static String modVersion(String modId) {
		try {
			if (isFabric()) {
				Object loader = Class.forName("net.fabricmc.loader.api.FabricLoader")
					.getMethod("getInstance")
					.invoke(null);
				Object container = loader.getClass().getMethod("getModContainer", String.class)
					.invoke(loader, modId);
				if (container instanceof Optional<?> optional && optional.isPresent()) {
					Object metadata = optional.get().getClass().getMethod("getMetadata").invoke(optional.get());
					Object version = metadata.getClass().getMethod("getVersion").invoke(metadata);
					return version.getClass().getMethod("getFriendlyString").invoke(version).toString();
				}
			}
			if (isNeoForge()) {
				Object modList = Class.forName("net.neoforged.fml.ModList")
					.getMethod("get")
					.invoke(null);
				if (modList == null) {
					return "dev";
				}
				Object container = modList.getClass().getMethod("getModContainerById", String.class)
					.invoke(modList, modId);
				if (container instanceof Optional<?> optional && optional.isPresent()) {
					Object info = optional.get().getClass().getMethod("getModInfo").invoke(optional.get());
					Object version = info.getClass().getMethod("getVersion").invoke(info);
					return version.toString();
				}
			}
		} catch (ReflectiveOperationException exception) {
			LOGGER.debug("Could not read the version of {}", modId, exception);
		}
		return "dev";
	}

	private static String detectLoader() {
		if (classPresent("net.fabricmc.loader.api.FabricLoader")) {
			return "fabric";
		}
		if (classPresent("net.neoforged.fml.ModList")) {
			return "neoforge";
		}
		return "unknown";
	}

	public static boolean classPresent(String name) {
		try {
			Class.forName(name, false, FPSMaxxPlatform.class.getClassLoader());
			return true;
		} catch (ClassNotFoundException ignored) {
			return false;
		}
	}
}
