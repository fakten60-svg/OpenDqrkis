package xyz.dqrkis.mixin;

import xyz.dqrkis.module.modules.render.NameHider;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChatHud.class)
public class ChatHudMixin {

	/**
	 * Target signature for 1.21.11:
	 * {@code ChatHud.addMessage(Text, MessageSignatureData, MessageIndicator)}.
	 *
	 * Both parameter types moved in this version. The old descriptor referenced
	 * {@code net.minecraft.message.MessageSignatureData} and {@code net.minecraft.chat.MessageIndicator},
	 * which no longer exist, so the injection target could not be resolved and the whole mixin
	 * config (required = true) failed to apply.
	 */
	@ModifyVariable(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
			at = @At("HEAD"), ordinal = 0, argsOnly = true)
	private Text dqrkis$modifyChatMessage(Text message) {
		NameHider nameHider = NameHider.get();
		if (nameHider == null || !nameHider.shouldHideInChat())
			return message;

		String filtered = nameHider.filter(message.getString());
		return filtered.equals(message.getString()) ? message : Text.literal(filtered);
	}
}
