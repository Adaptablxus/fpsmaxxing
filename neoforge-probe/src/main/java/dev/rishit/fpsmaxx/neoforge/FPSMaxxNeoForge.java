package dev.rishit.fpsmaxx.neoforge;

import com.mojang.blaze3d.platform.InputConstants;
import dev.rishit.fpsmaxx.FPSMaxxBootstrap;
import dev.rishit.fpsmaxx.FPSMaxxConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * NeoForge client entrypoint. All actual optimization work lives in the
 * shared FPSMaxx classes; this class only wires loader events.
 */
@Mod(value = FPSMaxxConstants.MOD_ID, dist = Dist.CLIENT)
public final class FPSMaxxNeoForge {
	private static KeyMapping toggleKey;

	public FPSMaxxNeoForge(IEventBus modEventBus) {
		FPSMaxxBootstrap.init();

		modEventBus.addListener(this::registerKeys);
		NeoForge.EVENT_BUS.addListener(this::registerClientCommands);
		NeoForge.EVENT_BUS.addListener(this::onClientTick);
	}

	private void registerKeys(RegisterKeyMappingsEvent event) {
		toggleKey = new KeyMapping(
			"key.fpsmaxx.toggle",
			InputConstants.KEY_F6,
			KeyMapping.Category.MISC
		);
		event.register(toggleKey);
	}

	private void registerClientCommands(RegisterClientCommandsEvent event) {
		FPSMaxxNeoForgeCommands.register(event.getDispatcher());
	}

	private void onClientTick(ClientTickEvent.Post event) {
		FPSMaxxBootstrap.onClientTick(Minecraft.getInstance(), toggleKey);
	}
}
