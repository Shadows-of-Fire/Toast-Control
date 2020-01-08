package shadows.toaster;

import java.util.Arrays;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.toasts.IToast;
import net.minecraft.client.gui.toasts.ToastGui;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

public class BetterGuiToast extends ToastGui {

	public BetterGuiToast() {
		super(Minecraft.getInstance());
		this.toastsQueue = new ControlledDeque();
		this.visible = new BetterToastInstance[ToastConfig.INSTANCE.toastCount.get()];
	}

	@Override
	public void render() {
		if (!this.mc.gameSettings.hideGUI) {
			RenderHelper.disableStandardItemLighting();

			for (int i = 0; i < this.visible.length; ++i) {
				ToastInstance<?> toastinstance = this.visible[i];

				if (toastinstance != null && toastinstance.render(this.mc.getWindow().getScaledWidth(), i)) {
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
			if (ToastConfig.INSTANCE.noSlide.get()) return 1;
			return this.forcedShowTime > ToastConfig.INSTANCE.forceTime.get() && this.visibility == IToast.Visibility.HIDE ? 1F - f : f;
		}

		@Override
		public boolean render(int scaledWidth, int arrayPos) {
			long i = Util.milliTime();

			if (this.animationTime == -1L) {
				this.animationTime = i;
				this.visibility.playSound(BetterGuiToast.this.mc.getSoundHandler());
			}

			if (this.visibility == IToast.Visibility.SHOW && i - this.animationTime <= 600L) {
				this.visibleTime = i;
			}

			RenderSystem.pushMatrix();
			if (ToastConfig.INSTANCE.startLeft.get()) RenderSystem.translatef(-160 + 160 * this.getVisibility(i), arrayPos * 32, 500 + arrayPos);
			else RenderSystem.translatef(scaledWidth - 160F * this.getVisibility(i), arrayPos * 32, 500 + arrayPos);
			RenderSystem.enableBlend();
			RenderSystem.translatef(ToastConfig.INSTANCE.offsetX.get(), ToastConfig.INSTANCE.offsetY.get(), 0);
			IToast.Visibility itoast$visibility = toast.draw(BetterGuiToast.this, i - this.visibleTime);
			RenderSystem.disableBlend();
			RenderSystem.popMatrix();

			if (this.forcedShowTime > ToastConfig.INSTANCE.forceTime.get() && itoast$visibility != this.visibility) {
				this.animationTime = i - ((long) ((1 - this.getVisibility(i)) * 600));
				this.visibility = itoast$visibility;
				this.visibility.playSound(BetterGuiToast.this.mc.getSoundHandler());
			}

			if (this.forcedShowTime > ToastConfig.INSTANCE.forceTime.get()) ToastControl.tracker.remove(this);

			return this.forcedShowTime > ToastConfig.INSTANCE.forceTime.get() && this.visibility == IToast.Visibility.HIDE && i - this.animationTime > 600L;
		}
	}

}
