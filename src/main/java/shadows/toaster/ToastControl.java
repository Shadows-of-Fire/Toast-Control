package shadows.toaster;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.toasts.GuiToast.ToastInstance;
import net.minecraft.client.gui.toasts.IToast;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import shadows.toaster.BetterGuiToast.BetterToastInstance;

@SuppressWarnings("deprecation")
@Mod(modid = ToastControl.MODID, version = ToastControl.VERSION, name = ToastControl.MODNAME, clientSideOnly = true, dependencies = ToastControl.DEPS)
public class ToastControl {

	public static final String MODID = "toastcontrol";
	public static final String MODNAME = "Toast Control";
	public static final String VERSION = "1.8.1";
	public static final String DEPS = "required-after:placebo@[1.6,)";
	public static final Logger LOGGER = LogManager.getLogger(MODID);

	public static final KeyBinding CLEAR = new KeyBinding("key.toastcontrol.clear", Keyboard.KEY_J, "key.toastcontrol.category");
	public static final ResourceLocation TRANSLUCENT = new ResourceLocation(MODID, "textures/gui/toasts.png");
	public static final ResourceLocation TRANSPARENT = new ResourceLocation(MODID, "textures/gui/toasts2.png");
	public static final ResourceLocation ORIGINAL = new ResourceLocation("textures/gui/toasts.png");

	@SubscribeEvent
	public void keys(KeyInputEvent e) {
		if (CLEAR.isKeyDown() && CLEAR.isPressed()) Minecraft.getMinecraft().getToastGui().clear();
	}

	@EventHandler
	public void preInit(FMLPreInitializationEvent e) {
		Minecraft.getMinecraft().toastGui = new BetterGuiToast();
		MinecraftForge.EVENT_BUS.register(this);
		handleToastReloc();
		handleBlockedClasses();
		ClientRegistry.registerKeyBinding(CLEAR);
	}

	private static void handleToastReloc() {
		ResourceLocation target = IToast.TEXTURE_TOASTS;
		if (ToastControlConfig.translucent) change(target, TRANSLUCENT);
		if (ToastControlConfig.transparent) change(target, TRANSPARENT);
		else if (!ToastControlConfig.translucent && !ToastControlConfig.transparent) change(target, ORIGINAL);
	}

	private static void change(ResourceLocation a, ResourceLocation b) {
		ReflectionHelper.setPrivateValue(ResourceLocation.class, a, b.getNamespace(), "namespace", "field_110626_a");
		ReflectionHelper.setPrivateValue(ResourceLocation.class, a, b.getPath(), "path", "field_110625_b");
	}

	public static final List<Class<?>> BLOCKED_CLASSES = new ArrayList<>();

	private static void handleBlockedClasses() {
		BLOCKED_CLASSES.clear();
		for (String s : ToastControlConfig.blockedClasses) {
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
		for (BetterToastInstance<?> t : tracker)
			t.tick();
	}

	@SubscribeEvent
	public void onConfigChanged(OnConfigChangedEvent event) {
		if (event.getModID().equals(ToastControl.MODID)) {
			ConfigManager.sync(ToastControl.MODID, Config.Type.INSTANCE);
			handleToastReloc();
			handleBlockedClasses();
			((BetterGuiToast) Minecraft.getMinecraft().toastGui).visible = new ToastInstance[ToastControlConfig.toastCount];
			LOGGER.info("Toast control config reloaded.");
		}
	}

	@Config(modid = ToastControl.MODID, category = "Toast Types")
	public static class ToastControlConfig {

		@Config.Name("Disable Advancements")
		@Config.Comment("If advancement toasts are blocked. Enabling will block ALL advancements.")
		public static boolean advancements = true;

		@Config.Name("Disable Recipes")
		@Config.Comment("If recipe unlock toasts are blocked. Blocks \"you have unlocked a new recipe\" toasts.")
		public static boolean recipes = true;

		@Config.Name("Disable System Toasts")
		@Config.Comment("If system toasts are blocked. This is used only for the narrator toggle notification right now.")
		public static boolean system = false;

		@Config.Name("Disable Tutorials")
		@Config.Comment("If tutorial toasts are blocked. Blocks useless things like use WASD to move.")
		public static boolean tutorial = true;

		@Config.Name("Disable All Vanilla")
		@Config.Comment("If all vanilla toasts are blocked.  Includes advancements, recipes, system, and tutorials.")
		public static boolean globalVanilla = false;

		@Config.Name("Disable All Non-Vanilla")
		@Config.Comment("If all non-vanilla toasts are blocked.  Blocks all toasts that do not extend vanilla classes.")
		public static boolean globalModded = false;

		@Config.Name("Disable All")
		@Config.Comment("If all toasts are blocked.")
		public static boolean global = false;

		@Config.Name("Forced Display Time")
		@Config.Comment("How long (in ticks) to force a toast to show for.  Higher is longer.")
		public static int forceTime = 0;

		@Config.Name("Translucent Toasts")
		@Config.Comment("If toasts are slightly translucent.")
		public static boolean translucent = true;

		@Config.Name("Transparent Toasts")
		@Config.Comment("If toasts do not draw a background.")
		public static boolean transparent = false;

		@Config.Name("Max Toasts Shown")
		@Config.Comment("The maximum number of toasts on the screen at once.  Default 3, Vanilla uses 5.")
		@Config.RangeInt(min = 1, max = 7)
		public static int toastCount = 3;

		@Config.Name("Print Toast Classnames")
		@Config.Comment("A debug config to print the class of each toast that tries to enter the GUI.  Useful for finding classes to block.")
		public static boolean printClasses = false;

		@Config.Name("Blacklisted Classes")
		@Config.Comment("A Class-specific blacklist for toasts.  Insert class names.")
		public static String[] blockedClasses = new String[0];

		@Config.Name("Toast X Offset")
		@Config.Comment("The amount to offset a toast in the x axis.")
		public static int offsetX = 0;
		
		@Config.Name("Toast X Percentage")
		@Config.Comment("Whether the Toast X Offset is a percentage out of 100.")
		public static boolean percentageX = false;

		@Config.Name("Toast Y Offset")
		@Config.Comment("The amount to offset a toast in the y axis.")
		public static int offsetY = 0;
		
		@Config.Name("Toast Y Percentage")
		@Config.Comment("Whether the Toast Y Offset is a percentage out of 100.")
		public static boolean percentageY = false;

		@Config.Name("Disable Transitions")
		@Config.Comment("Set to true to disable toasts sliding in to view.")
		public static boolean noSlide = false;

		@Config.Name("Transition from Left")
		@Config.Comment("Set to true to change the transition to start from the left.")
		public static boolean startLeft = false;
	}

}
