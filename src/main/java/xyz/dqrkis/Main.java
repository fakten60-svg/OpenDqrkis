package xyz.dqrkis;

import net.fabricmc.api.ModInitializer;

/**
 * Fabric entrypoint. No network access, no remote code loading: this only constructs
 * the client singleton.
 */
public final class Main implements ModInitializer {
	@Override
	public void onInitialize() {
		new Dqrkis();
	}
}
