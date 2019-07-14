package shadows.toaster;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Predicates;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.toasts.ToastGui.ToastInstance;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.config.ModConfig;

@EventBusSubscriber(modid = ToastControl.MODID)
public class ToastConfig {

	public static final ForgeConfigSpec SPEC;
	public static final ToastConfig INSTANCE;
	static {
		Pair<ToastConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(ToastConfig::new);
		SPEC = specPair.getRight();
		INSTANCE = specPair.getLeft();
	}

	public final BooleanValue advancements;
	public final BooleanValue recipes;
	public final BooleanValue system;
	public final BooleanValue tutorial;
	public final BooleanValue globalVanilla;
	public final BooleanValue globalModded;
	public final BooleanValue global;
	public final ConfigValue<List<? extends String>> blockedClasses;

	public final IntValue forceTime;
	public final BooleanValue translucent;
	public final BooleanValue transparent;
	public final IntValue toastCount;
	public final IntValue offsetX;
	public final IntValue offsetY;
	public final BooleanValue noSlide;
	public final BooleanValue startLeft;

	public final BooleanValue printClasses;

	public ToastConfig(ForgeConfigSpec.Builder build) {
		build.comment("Server configuration").push("client").push("blocked_toasts");

		advancements = build.comment("If advancement toasts are blocked.").define("advancements", true);
		recipes = build.comment("If recipe toasts are blocked.").define("recipes", true);
		system = build.comment("If system toasts are blocked.").define("system", false);
		tutorial = build.comment("If tutorial toasts are blocked.").define("tutorial", true);
		globalVanilla = build.comment("If all vanilla toasts are blocked.").define("global_vanilla", false);
		globalModded = build.comment("If all non-vanilla toasts are blocked.").define("global_modded", false);
		global = build.comment("If all toasts are blocked.").define("global", false);
		blockedClasses = build.comment("Toast Classes that are blocked from being shown.").defineList("blocked_classes", new ArrayList<String>(), Predicates.alwaysTrue());

		build.pop().push("visual_options");
		forceTime = build.comment("How long a toast must be on the screen for, in ticks.  Use 0 to use the default time.").defineInRange("force_time", 0, 0, 4000);
		translucent = build.comment("If toasts are translucent.").define("translucent", true);
		transparent = build.comment("If toasts are transparent.  Overrides translucency.").define("transparent", false);
		toastCount = build.comment("How many toasts will be displayed on screen at once.").defineInRange("toast_count", 3, 1, 7);
		offsetX = build.comment("The X offset for toasts to be drawn at.").defineInRange("x_offset", 0, -8192, 8192);
		offsetY = build.comment("The Y offset for toasts to be drawn at.").defineInRange("y_offset", 0, -8192, 8192);
		noSlide = build.comment("If toasts automatically pop into the screen without animations.").define("no_slide", false);
		startLeft = build.comment("If toasts show on the left of the screen.").define("start_left", false);

		build.pop().push("debug");
		printClasses = build.comment("If toast classes are printed when they are shown.").define("print_classes", false);
		build.pop().pop();
	}

	@SubscribeEvent
	public static void onLoad(ModConfig.Loading e) {
		if (e.getConfig().getModId().equals(ToastControl.MODID)) {
			ToastControl.handleToastReloc();
			ToastControl.handleBlockedClasses();
			((BetterGuiToast) Minecraft.getInstance().toastGui).visible = new ToastInstance[INSTANCE.toastCount.get()];
			ToastControl.LOGGER.info("Toast control config reloaded.");
		}
	}

}
