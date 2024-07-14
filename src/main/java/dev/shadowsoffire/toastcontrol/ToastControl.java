package dev.shadowsoffire.toastcontrol;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import dev.shadowsoffire.toastcontrol.BetterToastComponent.BetterToastInstance;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.client.gui.components.toasts.RecipeToast;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.TutorialToast;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = ToastControl.MODID, dist = Dist.CLIENT)
public class ToastControl {

    public static final String MODID = "toastcontrol";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public static final KeyMapping CLEAR = new KeyMapping("key.toastcontrol.clear", GLFW.GLFW_KEY_J, "key.toastcontrol.category");
    public static final List<Class<?>> BLOCKED_CLASSES = new ArrayList<>();

    public static List<BetterToastInstance<?>> tracker = new ArrayList<>();

    public ToastControl(IEventBus bus, ModContainer container) {
        bus.register(this);
        container.registerConfig(ModConfig.Type.CLIENT, ToastConfig.SPEC);
        bus.register(ToastConfig.class);
        NeoForge.EVENT_BUS.register(Events.class);
    }

    @SubscribeEvent
    public void clientSetup(FMLClientSetupEvent e) {
        Minecraft.getInstance().toast = new BetterToastComponent();
        handleToastReloc();
        handleBlockedClasses();
    }

    @SubscribeEvent
    public void keyReg(RegisterKeyMappingsEvent e) {
        e.register(CLEAR);
    }

    static void handleToastReloc() {
        ResourceLocation[] targets = new ResourceLocation[] { AdvancementToast.BACKGROUND_SPRITE, RecipeToast.BACKGROUND_SPRITE, SystemToast.BACKGROUND_SPRITE, TutorialToast.BACKGROUND_SPRITE };

        if (ToastConfig.isTextureTransparent()) {
            for (ResourceLocation t : targets) {
                change(t, ResourceLocation.fromNamespaceAndPath(MODID, "toast/transparent"));
            }
        }
        else if (ToastConfig.isTextureTranslucent()) {
            String[] paths = new String[] { "advancement", "recipe", "system", "tutorial" };
            for (int i = 0; i < 4; i++) {
                change(targets[i], ResourceLocation.fromNamespaceAndPath(MODID, "toast/translucent/" + paths[i]));
            }
        }
        else {
            String[] paths = new String[] { "advancement", "recipe", "system", "tutorial" };
            for (int i = 0; i < 4; i++) {
                change(targets[i], ResourceLocation.withDefaultNamespace("toast/" + paths[i]));
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

    private static void change(ResourceLocation a, ResourceLocation b) {
        ObfuscationReflectionHelper.setPrivateValue(ResourceLocation.class, a, b.getNamespace(), "namespace");
        ObfuscationReflectionHelper.setPrivateValue(ResourceLocation.class, a, b.getPath(), "path");
    }

    public static class Events {
        @SubscribeEvent
        public static void keys(InputEvent.Key e) {
            if (CLEAR.isDown()) {
                Minecraft.getInstance().getToasts().clear();
            }
        }

        @SubscribeEvent
        public static void clientTick(ClientTickEvent.Post e) {
            tracker.removeIf(BetterToastInstance::tick);
        }
    }

}
