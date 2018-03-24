package shadows.toaster;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.Minecraft;
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
import shadows.toaster.BetterGuiToast.ToastInstance;

@Mod(modid = ToastControl.MODID, version = ToastControl.VERSION, name = ToastControl.MODNAME, acceptedMinecraftVersions = ToastControl.VERS, clientSideOnly = true, dependencies = ToastControl.DEPS)
public class ToastControl {

	public static final String MODID = "toastcontrol";
	public static final String MODNAME = "Toast Control";
	public static final String VERSION = "1.4.1";
	public static final String VERS = "[1.12, 1.13)";
	public static final String DEPS = "required-after:placebo@[1.2.0,)";
	
	public static final KeyBinding CLEAR = new KeyBinding("key.toastcontrol.clear", Keyboard.KEY_J, "key.toastcontrol.category");
	
	@SubscribeEvent
	public void keys(KeyInputEvent e) {
		if(Keyboard.isKeyDown(CLEAR.getKeyCode()) && CLEAR.isPressed()) Minecraft.getMinecraft().getToastGui().clear();
	}

	@EventHandler
	public void preInit(FMLPreInitializationEvent e) {
		Minecraft.getMinecraft().toastGui = new BetterGuiToast();
		MinecraftForge.EVENT_BUS.register(this);
		if (ToastControlConfig.translucent) ReflectionHelper.setPrivateValue(ResourceLocation.class, IToast.TEXTURE_TOASTS, MODID, "resourceDomain", "field_110626_a");
		if (ToastControlConfig.transparent) ReflectionHelper.setPrivateValue(ResourceLocation.class, IToast.TEXTURE_TOASTS, "textures/gui/toasts2.png", "resourcePath", "field_110625_b");
		ClientRegistry.registerKeyBinding(CLEAR);
	}

	@SubscribeEvent
	public void onConfigChanged(OnConfigChangedEvent event) {
		if (event.getModID().equals(ToastControl.MODID)) ConfigManager.sync(ToastControl.MODID, Config.Type.INSTANCE);
	}

	public static List<ToastInstance> tracker = new ArrayList<>();

	@SubscribeEvent
	public void clientTick(ClientTickEvent e) {
		for (ToastInstance t : tracker)
			t.tick();
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
		@Config.RequiresMcRestart
		public static boolean translucent = true;

		@Config.Name("Transparent Toasts")
		@Config.Comment("If toasts do not draw a background.")
		@Config.RequiresMcRestart
		public static boolean transparent = false;
		
		@Config.Name("Max Toasts Shown")
		@Config.Comment("The maximum number of toasts on the screen at once.  Default 3, Vanilla uses 5.")
		@Config.RequiresMcRestart
		@Config.RangeInt(min = 1, max = 7)
		public static int toastCount = 3;
	}

}