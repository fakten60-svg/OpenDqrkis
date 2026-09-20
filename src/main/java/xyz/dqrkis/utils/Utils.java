package xyz.dqrkis.utils;

import xyz.dqrkis.module.modules.client.ClickGUI;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;

import java.awt.*;

import static xyz.dqrkis.Dqrkis.mc;

public final class Utils {

	public static Color getMainColor(int alpha, int increment) {
		int red = ClickGUI.red.getValueInt();
		int green = ClickGUI.green.getValueInt();
		int blue = ClickGUI.blue.getValueInt();

		if (ClickGUI.rainbow.getValue()) {
			return ColorUtils.getBreathingRGBColor(increment, alpha);
		} else {
			if (ClickGUI.breathing.getValue()) {
				return ColorUtils.getMainColor(new Color(red, green, blue, alpha), increment, 20);
			} else {
				return new Color(red, green, blue, alpha);
			}
		}
	}

	public static int getPing(Entity player) {
		if (mc.getNetworkHandler().getConnection() == null) return 0;

		PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry((player.getUuid()));
		if (playerListEntry == null) return 0;
		return playerListEntry.getLatency();
	}
}
