package dev.shadowsoffire.toastcontrol;

import java.util.ArrayDeque;

import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.client.gui.components.toasts.RecipeToast;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.TutorialToast;

public class ControlledDeque extends ArrayDeque<Toast> {

    public boolean isBlocked(Toast toast) {
        if (ToastConfig.INSTANCE.printClasses.get()) {
            ToastControl.LOGGER.info(toast.getClass());
        }

        if (ToastConfig.INSTANCE.global.get() || ToastConfig.INSTANCE.globalVanilla.get() && this.isVanillaToast(toast)) {
            return true;
        }

        if (ToastConfig.INSTANCE.globalModded.get() && !this.isVanillaToast(toast)) {
            return true;
        }

        if (ToastControl.BLOCKED_CLASSES.contains(toast.getClass())) {
            return true;
        }

        return toast instanceof AdvancementToast && ToastConfig.INSTANCE.advancements.get()
            || toast instanceof RecipeToast && ToastConfig.INSTANCE.recipes.get()
            || toast instanceof SystemToast && ToastConfig.INSTANCE.system.get()
            || toast instanceof TutorialToast && ToastConfig.INSTANCE.tutorial.get();
    }

    @Override
    public void addFirst(Toast t) {
        if (this.isBlocked(t)) return;
        super.addFirst(t);
    }

    @Override
    public void addLast(Toast t) {
        if (this.isBlocked(t)) return;
        super.addLast(t);
    }

    @Override
    public boolean add(Toast t) {
        this.addLast(t);
        return !this.isBlocked(t);
    }

    @Override
    public boolean offerFirst(Toast t) {
        this.addFirst(t);
        return !this.isBlocked(t);
    }

    @Override
    public boolean offerLast(Toast t) {
        this.addLast(t);
        return !this.isBlocked(t);
    }

    private boolean isVanillaToast(Toast toast) {
        return toast instanceof AdvancementToast || toast instanceof RecipeToast || toast instanceof SystemToast || toast instanceof TutorialToast;
    }

}
