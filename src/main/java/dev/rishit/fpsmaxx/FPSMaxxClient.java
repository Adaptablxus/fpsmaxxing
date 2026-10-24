package dev.rishit.fpsmaxx;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;

public final class FPSMaxxClient implements ClientModInitializer {
	private static KeyMapping toggleKey;

	@Override
	public void onInitializeClient() {
		FPSMaxxBootstrap.init();
		FPSMaxxCommands.register();

		toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.fpsmaxx.toggle",
			InputConstants.KEY_F6,
			KeyMapping.Category.MISC
		));

		ClientTickEvents.END_CLIENT_TICK.register(client ->
			FPSMaxxBootstrap.onClientTick(client, toggleKey)
		);
	}
}
