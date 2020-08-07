package shadows.toaster;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.network.FMLNetworkConstants;

@Mod(ToastLoader.MODID)
public class ToastLoader {

	public static final String MODID = "toastcontrol";
	public static final Logger LOGGER = LogManager.getLogger(MODID);

	public ToastLoader() {
		if (FMLEnvironment.dist == Dist.CLIENT) {
			ToastControl mod = new ToastControl();
			FMLJavaModLoadingContext.get().getModEventBus().addListener(mod::preInit);
			MinecraftForge.EVENT_BUS.register(ToastConfig.class);
			ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ToastConfig.SPEC);
		} else LOGGER.error("Running on a dedicated server, disabling mod.");
		ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (a, b) -> true));
	}

}
