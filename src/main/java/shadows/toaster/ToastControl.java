package shadows.toaster;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.InputEvent.KeyInputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import shadows.toaster.BetterToastComponent.BetterToastInstance;

public class ToastControl {

	public static final String MODID = ToastLoader.MODID;
	public static final Logger LOGGER = ToastLoader.LOGGER;
	public static final KeyMapping CLEAR = new KeyMapping("key.toastcontrol.clear", GLFW.GLFW_KEY_J, "key.toastcontrol.category");
	public static final ResourceLocation TRANSLUCENT = new ResourceLocation(MODID, "textures/gui/toasts.png");
	public static final ResourceLocation TRANSPARENT = new ResourceLocation(MODID, "textures/gui/toasts2.png");
	public static final ResourceLocation ORIGINAL = new ResourceLocation("textures/gui/toasts.png");

	@SubscribeEvent
	public void keys(KeyInputEvent e) {
		if (CLEAR.isDown()) Minecraft.getInstance().getToasts().clear();
	}

	public void preInit(FMLClientSetupEvent e) {
		Minecraft.getInstance().toast = new BetterToastComponent();
		MinecraftForge.EVENT_BUS.register(this);
		handleToastReloc();
		handleBlockedClasses();
	}

	static void handleToastReloc() {
		ResourceLocation target = Toast.TEXTURE;
		if (ToastConfig.INSTANCE.translucent.get()) change(target, TRANSLUCENT);
		if (ToastConfig.INSTANCE.transparent.get()) change(target, TRANSPARENT);
		else if (!ToastConfig.INSTANCE.translucent.get() && !ToastConfig.INSTANCE.transparent.get()) change(target, ORIGINAL);
	}

	private static void change(ResourceLocation a, ResourceLocation b) {
		ObfuscationReflectionHelper.setPrivateValue(ResourceLocation.class, a, b.getNamespace(), "f_135804_");
		ObfuscationReflectionHelper.setPrivateValue(ResourceLocation.class, a, b.getPath(), "f_135805_");
	}

	public static final List<Class<?>> BLOCKED_CLASSES = new ArrayList<>();

	static void handleBlockedClasses() {
		BLOCKED_CLASSES.clear();
		for (String s : ToastConfig.INSTANCE.blockedClasses.get()) {
			try {
				Class<?> c = Class.forName(s);
				BLOCKED_CLASSES.add(c);
			} catch (ClassNotFoundException e) {
				LOGGER.error("Invalid class string provided to toast control: " + s);
			}
		}
	}

	public static List<BetterToastInstance<?>> tracker = new ArrayList<>();

	@SubscribeEvent
	public void clientTick(ClientTickEvent e) {
		if (e.phase == Phase.END) for (BetterToastInstance<?> t : tracker)
			t.tick();
	}

}