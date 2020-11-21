package shadows.toaster;

import java.util.Arrays;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.toasts.GuiToast;
import net.minecraft.client.gui.toasts.IToast;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.util.math.MathHelper;
import shadows.toaster.ToastControl.ToastControlConfig;

public class BetterGuiToast extends GuiToast {

	public BetterGuiToast() {
		super(Minecraft.getMinecraft());
		this.toastsQueue = new ControlledDeque();
		this.visible = new BetterToastInstance[ToastControlConfig.toastCount];
	}

	@Override
	public void drawToast(ScaledResolution resolution) {
		if (!this.mc.gameSettings.hideGUI) {
			RenderHelper.disableStandardItemLighting();

			for (int i = 0; i < this.visible.length; ++i) {
				ToastInstance<?> toastinstance = this.visible[i];

				if (toastinstance != null && toastinstance.render(resolution.getScaledWidth(), resolution.getScaledHeight(), i)) {
					this.visible[i] = null;
				}

				if (this.visible[i] == null && !this.toastsQueue.isEmpty()) {
					this.visible[i] = new BetterToastInstance<>(this.toastsQueue.removeFirst());
				}
			}
		}
	}

	@Override
	public void clear() {
		Arrays.fill(this.visible, null);
		this.toastsQueue.clear();
	}

	public class BetterToastInstance<T extends IToast> extends ToastInstance<T> {

		protected int forcedShowTime = 0;

		protected BetterToastInstance(T toast) {
			super(toast);
			ToastControl.tracker.add(this);
		}

		public void tick() {
			forcedShowTime++;
		}

		protected float getVisibility(long sysTime) {
			float f = MathHelper.clamp((sysTime - this.animationTime) / 600F, 0F, 1F);
			f = f * f;
			if (ToastControlConfig.noSlide) return 1;
			return this.forcedShowTime > ToastControlConfig.forceTime && this.visibility == IToast.Visibility.HIDE ? 1F - f : f;
		}

		@Override
		public boolean render(int scaledWidth, int arrayPos) {
			return render(scaledWidth, -1, arrayPos);
		}
		
		public boolean render(int scaledWidth, int scaledHeight, int arrayPos) {
			long i = Minecraft.getSystemTime();

			if (this.animationTime == -1L) {
				this.animationTime = i;
				this.visibility.playSound(BetterGuiToast.this.mc.getSoundHandler());
			}

			if (this.visibility == IToast.Visibility.SHOW && i - this.animationTime <= 600L) {
				this.visibleTime = i;
			}

			GlStateManager.pushMatrix();
			if (ToastControlConfig.startLeft) GlStateManager.translate(-160 + 160 * this.getVisibility(i), arrayPos * 32, 500 + arrayPos);
			else GlStateManager.translate(scaledWidth - 160F * this.getVisibility(i), arrayPos * 32, 500 + arrayPos);
			GlStateManager.enableBlend();
			GlStateManager.translate(ToastControlConfig.percentageX ? scaledWidth * 0.01 * ToastControlConfig.offsetX : ToastControlConfig.offsetX, ToastControlConfig.percentageY && scaledHeight != -1 ? scaledHeight * 0.01 * ToastControlConfig.offsetY : ToastControlConfig.offsetY, 0);
			IToast.Visibility itoast$visibility = toast.draw(BetterGuiToast.this, i - this.visibleTime);
			GlStateManager.disableBlend();
			GlStateManager.popMatrix();

			if (this.forcedShowTime > ToastControlConfig.forceTime && itoast$visibility != this.visibility) {
				this.animationTime = i - ((long) ((1 - this.getVisibility(i)) * 600));
				this.visibility = itoast$visibility;
				this.visibility.playSound(BetterGuiToast.this.mc.getSoundHandler());
			}

			if (this.forcedShowTime > ToastControlConfig.forceTime) ToastControl.tracker.remove(this);

			return this.forcedShowTime > ToastControlConfig.forceTime && this.visibility == IToast.Visibility.HIDE && i - this.animationTime > 600L;
		}
	}

}
