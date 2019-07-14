package shadows.toaster;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.toasts.IToast;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.InputEvent.KeyInputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import shadows.placebo.util.ReflectionHelper;
import shadows.toaster.BetterGuiToast.BetterToastInstance;

@Mod(ToastControl.MODID)
public class ToastControl {

	public static final String MODID = "toastcontrol";
	public static final Logger LOGGER = LogManager.getLogger(MODID);

	public static final KeyBinding CLEAR = new KeyBinding("key.toastcontrol.clear", GLFW.GLFW_KEY_J, "key.toastcontrol.category");
	public static final ResourceLocation TRANSLUCENT = new ResourceLocation(MODID, "textures/gui/toasts.png");
	public static final ResourceLocation TRANSPARENT = new ResourceLocation(MODID, "textures/gui/toasts2.png");
	public static final ResourceLocation ORIGINAL = new ResourceLocation("textures/gui/toasts.png");

	public ToastControl() {
		FMLJavaModLoadingContext.get().getModEventBus().register(this);
		MinecraftForge.EVENT_BUS.register(this);
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ToastConfig.SPEC);
	}

	@SubscribeEvent
	public void keys(KeyInputEvent e) {
		if (CLEAR.isKeyDown() && CLEAR.isPressed()) Minecraft.getInstance().getToastGui().clear();
	}

	@SubscribeEvent
	public void preInit(FMLClientSetupEvent e) {
		Minecraft.getInstance().toastGui = new BetterGuiToast();
		MinecraftForge.EVENT_BUS.register(this);
		handleToastReloc();
		handleBlockedClasses();
		ClientRegistry.registerKeyBinding(CLEAR);
	}

	static void handleToastReloc() {
		ResourceLocation target = IToast.TEXTURE_TOASTS;
		if (ToastConfig.INSTANCE.translucent.get()) change(target, TRANSLUCENT);
		if (ToastConfig.INSTANCE.transparent.get()) change(target, TRANSPARENT);
		else if (!ToastConfig.INSTANCE.translucent.get() && !ToastConfig.INSTANCE.transparent.get()) change(target, ORIGINAL);
	}

	private static void change(ResourceLocation a, ResourceLocation b) {
		ReflectionHelper.setPrivateValue(ResourceLocation.class, a, b.getNamespace(), "namespace", "field_110626_a");
		ReflectionHelper.setPrivateValue(ResourceLocation.class, a, b.getPath(), "path", "field_110625_b");
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