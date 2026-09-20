package xyz.dqrkis.module.modules.misc;

import xyz.dqrkis.event.events.TickListener;
import xyz.dqrkis.module.Category;
import xyz.dqrkis.module.Module;
import xyz.dqrkis.module.setting.BooleanSetting;
import xyz.dqrkis.module.setting.KeybindSetting;
import xyz.dqrkis.module.setting.NumberSetting;
import xyz.dqrkis.utils.ChatUtils;
import xyz.dqrkis.utils.EncryptedString;

public final class SilentHomeSetter extends Module implements TickListener {
	private final KeybindSetting triggerKey = new KeybindSetting(EncryptedString.of("Trigger Key"), 71, false)
			.setDescription(EncryptedString.of("Key to set the home (default: G)"));
	private final BooleanSetting deletePreviousHome = new BooleanSetting(EncryptedString.of("Delete Previous Home"), true);
	private final NumberSetting homeSlot = new NumberSetting(EncryptedString.of("Home Slot"), 1, 5, 1, 1);

	private boolean keyWasPressed;
	private boolean hidingOverlayMessage;
	private int overlayHideTicks;
	private boolean pendingSetHome;
	private int setHomeDelayTicks;
	private long lastSetHomeTime;

	public SilentHomeSetter() {
		super(EncryptedString.of("Silent Home Setter"),
				EncryptedString.of("Sets a home at your current coordinates without saying 'Home Set' in the chat or anywhere else."),
				-1,
				Category.MISC);
		addSettings(triggerKey, deletePreviousHome, homeSlot);
	}

	@Override
	public void onEnable() {
		eventManager.add(TickListener.class, this);
		keyWasPressed = false;
		pendingSetHome = false;
		setHomeDelayTicks = 0;
		lastSetHomeTime = 0L;
		super.onEnable();
	}

	@Override
	public void onDisable() {
		eventManager.remove(TickListener.class, this);
		hidingOverlayMessage = false;
		overlayHideTicks = 0;
		pendingSetHome = false;
		setHomeDelayTicks = 0;
		lastSetHomeTime = 0L;
		super.onDisable();
	}

	@Override
	public void onTick() {
		if (mc.currentScreen != null || mc.player == null)
			return;

		if (hidingOverlayMessage && --overlayHideTicks <= 0)
			hidingOverlayMessage = false;

		if (pendingSetHome && --setHomeDelayTicks <= 0) {
			setHomeAndNotify();
			pendingSetHome = false;
		}

		int key = triggerKey.getKey();
		if (key == -1) {
			keyWasPressed = false;
			return;
		}

		boolean pressed = xyz.dqrkis.utils.KeyUtils.isKeyPressed(key);
		if (pressed && !keyWasPressed) {
			hidingOverlayMessage = true;
			overlayHideTicks = 40;

			if (deletePreviousHome.getValue())
				mc.getNetworkHandler().sendChatCommand("delhome " + homeSlot.getValueInt());

			pendingSetHome = true;
			setHomeDelayTicks = 10;
		}

		keyWasPressed = pressed;
	}

	private void setHomeAndNotify() {
		long now = System.currentTimeMillis();
		if (now - lastSetHomeTime < 500L)
			return;

		lastSetHomeTime = now;
		mc.getNetworkHandler().sendChatCommand("sethome " + homeSlot.getValue());

		ChatUtils.info("Home " + homeSlot.getValueInt() + " set at "
				+ Math.round(mc.player.getX()) + ", "
				+ Math.round(mc.player.getY()) + ", "
				+ Math.round(mc.player.getZ()));
	}

	public boolean isHidingOverlayMessage() {
		return hidingOverlayMessage;
	}
}
