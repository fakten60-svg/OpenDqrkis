package xyz.dqrkis.module.modules.misc;

import xyz.dqrkis.module.Category;
import xyz.dqrkis.module.Module;
import xyz.dqrkis.module.setting.BooleanSetting;
import xyz.dqrkis.module.setting.NumberSetting;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;

public final class AutoReconnect extends Module {
	private final NumberSetting delay = new NumberSetting("Delay", 0, 60, 3.5, 0.1)
			.setDescription("Seconds to wait before reconnecting");
	private final BooleanSetting hideButtons = new BooleanSetting("Hide Buttons", false)
			.setDescription("Hides the reconnect buttons on the disconnect screen");

	public ServerAddress serverAddress;
	public ServerInfo serverInfo;

	public AutoReconnect() {
		super("Auto Reconnect",
				"Automatically reconnects when disconnected from a server",
				-1,
				Category.MISC);
		addSettings(delay, hideButtons);
	}

	public void recordConnection(ServerAddress address, ServerInfo info) {
		this.serverAddress = address;
		this.serverInfo = info;
	}

	public boolean hasServer() {
		return serverAddress != null;
	}

	public double getDelaySeconds() {
		return delay.getValue();
	}

	public boolean shouldHideButtons() {
		return hideButtons.getValue();
	}
}
