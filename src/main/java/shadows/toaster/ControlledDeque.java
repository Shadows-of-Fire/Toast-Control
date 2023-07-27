package shadows.toaster;

import dev.shadowsoffire.placebo.collections.BlockedDeque;
import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.client.gui.components.toasts.RecipeToast;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.TutorialToast;

@SuppressWarnings("deprecation")
public class ControlledDeque extends BlockedDeque<Toast> {

    private static final long serialVersionUID = -5380678178676126928L;

    @Override
    public boolean isBlocked(Toast toast) {
        if (ToastConfig.INSTANCE.printClasses.get()) ToastControl.LOGGER.info(toast.getClass());
        if (ToastConfig.INSTANCE.global.get() || ToastConfig.INSTANCE.globalVanilla.get() && this.isVanillaToast(toast)) return true;
        if (ToastConfig.INSTANCE.globalModded.get() && !this.isVanillaToast(toast)) return true;
        if (ToastControl.BLOCKED_CLASSES.contains(toast.getClass())) return true;
        return toast instanceof AdvancementToast && ToastConfig.INSTANCE.advancements.get() || toast instanceof RecipeToast && ToastConfig.INSTANCE.recipes.get() || toast instanceof SystemToast && ToastConfig.INSTANCE.system.get()
            || toast instanceof TutorialToast && ToastConfig.INSTANCE.tutorial.get();
    }

    private boolean isVanillaToast(Toast toast) {
        return toast instanceof AdvancementToast || toast instanceof RecipeToast || toast instanceof SystemToast || toast instanceof TutorialToast;
    }
}
