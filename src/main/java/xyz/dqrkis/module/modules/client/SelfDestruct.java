package xyz.dqrkis.module.modules.client;

import xyz.dqrkis.Dqrkis;
import xyz.dqrkis.gui.ClickGui;
import xyz.dqrkis.module.Category;
import xyz.dqrkis.module.Module;
import xyz.dqrkis.module.setting.*;
import xyz.dqrkis.utils.EncryptedString;

@SuppressWarnings("all")
public final class SelfDestruct extends Module {
	public static boolean destruct = false;

	public SelfDestruct() {
		super(EncryptedString.of("Self Destruct"),
				EncryptedString.of("Unloads the client and wipes its module configuration from memory |Credits to lwes for deletion|"),
				-1,
				Category.CLIENT);
	}

	@Override
	public void onEnable() {
		destruct = true;

		Dqrkis.INSTANCE.getModuleManager().getModule(ClickGUI.class).setEnabled(false);
		setEnabled(false);

		Dqrkis.INSTANCE.getProfileManager().saveProfile();

		if (mc.currentScreen instanceof ClickGui) {
			Dqrkis.INSTANCE.guiInitialized = false;
			mc.currentScreen.close();
		}

		for (Module module : Dqrkis.INSTANCE.getModuleManager().getModules()) {
			module.setEnabled(false);

			module.setName(null);
			module.setDescription(null);

			for (Setting<?> setting : module.getSettings()) {
				setting.setName(null);
				setting.setDescription(null);

				if (setting instanceof StringSetting set)
					set.setValue(null);
			}
			module.getSettings().clear();
		}

		Runtime runtime = Runtime.getRuntime();

		for (int i = 0; i <= 10; i++) {
			runtime.gc();
			runtime.runFinalization();

			try {
				Thread.sleep(100 * i);
			} catch (InterruptedException ignored) {
			}
		}
	}
}
