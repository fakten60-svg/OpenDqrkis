package xyz.dqrkis.module.modules.combat;

import xyz.dqrkis.module.Category;
import xyz.dqrkis.module.Module;
import xyz.dqrkis.module.setting.BooleanSetting;
import xyz.dqrkis.module.setting.NumberSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

public final class HitboxExpand extends Module {
	private final BooleanSetting playersOnly = new BooleanSetting("Players Only", true)
			.setDescription("Only expands the hitbox of players");
	private final NumberSetting expand = new NumberSetting("Expand Amount", 0, 1, 0.1, 0.05)
			.setDescription("How much to expand the targeting hitbox");

	public HitboxExpand() {
		super("Hitbox Expand",
				"Expands entity hitboxes so they are easier to hit",
				-1,
				Category.COMBAT);
		addSettings(playersOnly, expand);
	}

	public float getExpand(Entity entity) {
		if (!isEnabled())
			return 0.0F;
		if (playersOnly.getValue() && !(entity instanceof PlayerEntity))
			return 0.0F;
		if (entity == mc.player)
			return 0.0F;
		return expand.getValueFloat();
	}
}
