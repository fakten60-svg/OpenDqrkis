package xyz.dqrkis.module;


public enum Category {
	COMBAT("Combat"),
	MISC("Misc"),
	RENDER("Render"),
	CLIENT("Client"),
	CART("Cart PvP");
	public final CharSequence name;

	Category(CharSequence name) {
		this.name = name;
	}
}
