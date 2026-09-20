package xyz.dqrkis.managers;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import xyz.dqrkis.Dqrkis;
import xyz.dqrkis.module.Module;
import xyz.dqrkis.module.setting.*;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Stores the module configuration as plain JSON inside the game's config directory
 * (".minecraft/config/dqrkis.json").
 *
 * Previous revisions wrote to a randomly named folder ("UJHfsGGjbPfVZ") in the system
 * temp directory, and on non-Windows systems directly into the user's home directory.
 * That behaviour has been removed: configuration now lives next to the game and nowhere else.
 */
public final class ProfileManager {
	private static final String FILE_NAME = "dqrkis.json";

	private final Gson g = new Gson();
	private final Path profilePath = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
	private JsonObject profile;

	public void loadProfile() {
		try {
			if (!Files.isRegularFile(profilePath))
				return;

			profile = g.fromJson(Files.readString(profilePath), JsonObject.class);
			if (profile == null)
				return;

			for (Module module : Dqrkis.INSTANCE.getModuleManager().getModules()) {
				JsonElement moduleJson = profile.get(String.valueOf(Dqrkis.INSTANCE.getModuleManager().getModules().indexOf(module)));
				if (moduleJson == null || !moduleJson.isJsonObject())
					continue;
				JsonObject moduleConfig = moduleJson.getAsJsonObject();

				JsonElement enabledJson = moduleConfig.get("enabled");
				if (enabledJson == null || !enabledJson.isJsonPrimitive())
					continue;

				if (enabledJson.getAsBoolean())
					module.setEnabled(true);

				for (Setting<?> setting : module.getSettings()) {
					JsonElement settingJson = moduleConfig.get(String.valueOf(module.getSettings().indexOf(setting)));
					if (settingJson == null)
						continue;

					if (setting instanceof BooleanSetting booleanSetting) {
						booleanSetting.setValue(settingJson.getAsBoolean());
					} else if (setting instanceof ModeSetting<?> modeSetting) {
						modeSetting.setModeIndex(settingJson.getAsInt());
					} else if (setting instanceof NumberSetting numberSetting) {
						numberSetting.setValue(settingJson.getAsDouble());
					} else if (setting instanceof KeybindSetting keybindSetting) {
						keybindSetting.setKey(settingJson.getAsInt());
						if (keybindSetting.isModuleKey())
							module.setKey(settingJson.getAsInt());
					} else if (setting instanceof StringSetting stringSetting) {
						stringSetting.setValue(settingJson.getAsString());
					} else if (setting instanceof MinMaxSetting minMaxSetting) {
						if (settingJson.isJsonObject()) {
							JsonObject minMaxObject = settingJson.getAsJsonObject();
							minMaxSetting.setMinValue(minMaxObject.get("1").getAsDouble());
							minMaxSetting.setMaxValue(minMaxObject.get("2").getAsDouble());
						}
					}
				}
			}
		} catch (Exception ignored) {
		}
	}

	public void saveProfile() {
		try {
			Files.createDirectories(profilePath.getParent());
			profile = new JsonObject();

			for (Module module : Dqrkis.INSTANCE.getModuleManager().getModules()) {
				JsonObject moduleConfig = new JsonObject();
				moduleConfig.addProperty("enabled", module.isEnabled());

				for (Setting<?> setting : module.getSettings()) {
					String key = String.valueOf(module.getSettings().indexOf(setting));

					if (setting instanceof BooleanSetting booleanSetting) {
						moduleConfig.addProperty(key, booleanSetting.getValue());
					} else if (setting instanceof ModeSetting<?> modeSetting) {
						moduleConfig.addProperty(key, modeSetting.getModeIndex());
					} else if (setting instanceof NumberSetting numberSetting) {
						moduleConfig.addProperty(key, numberSetting.getValue());
					} else if (setting instanceof KeybindSetting keybindSetting) {
						moduleConfig.addProperty(key, keybindSetting.getKey());
					} else if (setting instanceof StringSetting stringSetting) {
						moduleConfig.addProperty(key, stringSetting.getValue());
					} else if (setting instanceof MinMaxSetting minMaxSetting) {
						JsonObject minMaxObject = new JsonObject();
						minMaxObject.addProperty("1", minMaxSetting.getMinValue());
						minMaxObject.addProperty("2", minMaxSetting.getMaxValue());

						moduleConfig.add(key, minMaxObject);
					}
				}

				profile.add(String.valueOf(Dqrkis.INSTANCE.getModuleManager().getModules().indexOf(module)), moduleConfig);
			}
			Files.writeString(profilePath, g.toJson(profile));
		} catch (Exception ignored) {
		}
	}
}
