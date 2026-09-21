package xyz.dqrkis.module.modules.misc;

import xyz.dqrkis.module.Category;
import xyz.dqrkis.module.Module;

public final class NoBreakDelay extends Module {
	public NoBreakDelay() {
		super("No Break Delay",
				"Removes the break delay from mining blocks",
				-1,
				Category.MISC);
	}
}
