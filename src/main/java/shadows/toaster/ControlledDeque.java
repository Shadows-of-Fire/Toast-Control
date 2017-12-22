package shadows.toaster;

import net.minecraft.client.gui.toasts.AdvancementToast;
import net.minecraft.client.gui.toasts.IToast;
import net.minecraft.client.gui.toasts.RecipeToast;
import net.minecraft.client.gui.toasts.SystemToast;
import net.minecraft.client.gui.toasts.TutorialToast;
import shadows.placebo.util.collections.BlockedDeque;
import shadows.toaster.ToastControl.ToastControlConfig;

public class ControlledDeque extends BlockedDeque<IToast> {

	private static final long serialVersionUID = -5380678178676126928L;

	public boolean isBlocked(IToast toast) {
		if (ToastControlConfig.global) return true;
		if (ToastControlConfig.globalVanilla && isVanillaToast(toast)) return true;
		if (ToastControlConfig.globalModded && !isVanillaToast(toast)) return true;
		return (toast instanceof AdvancementToast && ToastControlConfig.advancements) || (toast instanceof RecipeToast && ToastControlConfig.recipes) || (toast instanceof SystemToast && ToastControlConfig.system) || (toast instanceof TutorialToast && ToastControlConfig.tutorial);
	}

	private boolean isVanillaToast(IToast toast) {
		return toast instanceof AdvancementToast || toast instanceof RecipeToast || toast instanceof SystemToast || toast instanceof TutorialToast;
	}
}