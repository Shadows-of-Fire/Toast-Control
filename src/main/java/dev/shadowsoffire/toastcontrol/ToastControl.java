package dev.shadowsoffire.toastcontrol;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.client.gui.components.toasts.RecipeToast;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.TutorialToast;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ToastAddEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = ToastControl.MODID, dist = Dist.CLIENT)
public class ToastControl {

    public static final String MODID = "toastcontrol";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(Identifier.fromNamespaceAndPath(MODID, "category"));
    public static final KeyMapping CLEAR = new KeyMapping("key.toastcontrol.clear", GLFW.GLFW_KEY_J, CATEGORY);
    public static final List<Class<?>> BLOCKED_CLASSES = new ArrayList<>();

    public ToastControl(IEventBus bus, ModContainer container) {
        bus.register(this);
        container.registerConfig(ModConfig.Type.CLIENT, ToastConfig.SPEC);
        bus.register(ToastConfig.class);
        NeoForge.EVENT_BUS.register(Events.class);
    }

    @SubscribeEvent
    public void clientSetup(FMLClientSetupEvent e) {
        Minecraft.getInstance().toastManager = new BetterToastManager();
        handleToastReloc();
        handleBlockedClasses();
    }

    @SubscribeEvent
    public void keyReg(RegisterKeyMappingsEvent e) {
        e.registerCategory(CATEGORY);
        e.register(CLEAR);
    }

    static void handleToastReloc() {
        Identifier[] targets = new Identifier[] { AdvancementToast.BACKGROUND_SPRITE, RecipeToast.BACKGROUND_SPRITE, SystemToast.BACKGROUND_SPRITE, TutorialToast.BACKGROUND_SPRITE };

        if (ToastConfig.isTextureTransparent()) {
            for (Identifier t : targets) {
                change(t, Identifier.fromNamespaceAndPath(MODID, "toast/transparent"));
            }
        }
        else if (ToastConfig.isTextureTranslucent()) {
            String[] paths = new String[] { "advancement", "recipe", "system", "tutorial" };
            for (int i = 0; i < 4; i++) {
                change(targets[i], Identifier.fromNamespaceAndPath(MODID, "toast/translucent/" + paths[i]));
            }
        }
        else {
            String[] paths = new String[] { "advancement", "recipe", "system", "tutorial" };
            for (int i = 0; i < 4; i++) {
                change(targets[i], Identifier.withDefaultNamespace("toast/" + paths[i]));
            }
        }
    }

    static void handleBlockedClasses() {
        BLOCKED_CLASSES.clear();
        for (String s : ToastConfig.INSTANCE.blockedClasses.get()) {
            try {
                Class<?> c = Class.forName(s);
                BLOCKED_CLASSES.add(c);
            }
            catch (ClassNotFoundException e) {
                LOGGER.error("Invalid class string provided to toast control: " + s);
            }
        }
    }

    private static void change(Identifier a, Identifier b) {
        ObfuscationReflectionHelper.setPrivateValue(Identifier.class, a, b.getNamespace(), "namespace");
        ObfuscationReflectionHelper.setPrivateValue(Identifier.class, a, b.getPath(), "path");
    }

    /**
     * Determines if a toast should be prevented from showing, based on the current configuration.
     */
    static boolean isBlocked(Toast toast) {
        if (ToastConfig.INSTANCE.printClasses.get()) {
            LOGGER.info(toast.getClass());
        }

        if (ToastConfig.INSTANCE.global.get() || ToastConfig.INSTANCE.globalVanilla.get() && isVanillaToast(toast)) {
            return true;
        }

        if (ToastConfig.INSTANCE.globalModded.get() && !isVanillaToast(toast)) {
            return true;
        }

        if (BLOCKED_CLASSES.contains(toast.getClass())) {
            return true;
        }

        return toast instanceof AdvancementToast && ToastConfig.INSTANCE.advancements.get()
            || toast instanceof RecipeToast && ToastConfig.INSTANCE.recipes.get()
            || toast instanceof SystemToast && ToastConfig.INSTANCE.system.get()
            || toast instanceof TutorialToast && ToastConfig.INSTANCE.tutorial.get();
    }

    private static boolean isVanillaToast(Toast toast) {
        return toast instanceof AdvancementToast || toast instanceof RecipeToast || toast instanceof SystemToast || toast instanceof TutorialToast;
    }

    public static class Events {
        @SubscribeEvent
        public static void onToast(ToastAddEvent e) {
            if (isBlocked(e.getToast())) {
                e.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void keys(InputEvent.Key e) {
            if (CLEAR.isDown()) {
                Minecraft.getInstance().getToastManager().clear();
            }
        }
    }

}
