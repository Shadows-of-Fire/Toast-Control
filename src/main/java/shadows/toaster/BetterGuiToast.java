package shadows.toaster;

import java.util.Arrays;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.toasts.GuiToast;
import net.minecraft.client.gui.toasts.IToast;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.util.math.MathHelper;
import shadows.toaster.ToastControl.ToastControlConfig;

public class BetterGuiToast extends GuiToast {

	protected ToastInstance[] visible = new ToastInstance[5];

	public BetterGuiToast() {
		super(Minecraft.getMinecraft());
		this.toastsQueue = new ControlledDeque();
	}

	@Override
	public void drawToast(ScaledResolution resolution) {
		if (!this.mc.gameSettings.hideGUI) {
			RenderHelper.disableStandardItemLighting();

			for (int i = 0; i < this.visible.length; ++i) {
				ToastInstance toastinstance = this.visible[i];

				if (toastinstance != null && toastinstance.render(resolution.getScaledWidth(), i)) {
					this.visible[i] = null;
				}

				if (this.visible[i] == null && !this.toastsQueue.isEmpty()) {
					this.visible[i] = new ToastInstance(this.toastsQueue.removeFirst());
				}
			}
		}
	}

	@Override
	@Nullable
	public <T extends IToast> T getToast(Class<? extends T> toastClass, Object token) {
		for (ToastInstance toastinstance : this.visible) {
			if (toastinstance != null && toastClass.isAssignableFrom(toastinstance.getToast().getClass()) && toastinstance.getToast().getType().equals(token)) return toastClass.cast(toastinstance.getToast());
		}

		for (IToast itoast : this.toastsQueue) {
			if (toastClass.isAssignableFrom(itoast.getClass()) && itoast.getType().equals(token)) return toastClass.cast(itoast);
		}

		return null;
	}

	@Override
	public void clear() {
		Arrays.fill(this.visible, null);
		this.toastsQueue.clear();
	}

	@Override
	public void add(IToast toast) {
		this.toastsQueue.add(toast);
	}

	@Override
	public Minecraft getMinecraft() {
		return this.mc;
	}

	protected class ToastInstance {
		protected final IToast toast;
		protected long animationTime= -1L;
		protected long visibleTime = -1L;
		protected int forcedShowTime = 0;
		protected IToast.Visibility visibility = IToast.Visibility.SHOW;

		protected ToastInstance(IToast toast) {
			this.toast = toast;
			ToastControl.tracker.add(this);
		}

		public IToast getToast() {
			return this.toast;
		}
		
		public void tick() {
			forcedShowTime++;
		}

		protected float getVisibility(long sysTime) {
			float f = MathHelper.clamp((sysTime - this.animationTime) / 600.0F, 0.0F, 1.0F);
			f = f * f;
			return this.forcedShowTime > ToastControlConfig.forceTime && this.visibility == IToast.Visibility.HIDE ? 1.0F - f : f;
		}

		public boolean render(int scaledWidth, int arrayPos) {
			long i = Minecraft.getSystemTime();

			if (this.animationTime == -1L) {
				this.animationTime = i;
				this.visibility.playSound(BetterGuiToast.this.mc.getSoundHandler());
			}

			if (this.visibility == IToast.Visibility.SHOW && i - this.animationTime <= 600L) {
				this.visibleTime = i;
			}

			GlStateManager.pushMatrix();
			GlStateManager.translate(scaledWidth - 160.0F * this.getVisibility(i), arrayPos * 32, 500 + arrayPos);
			GlStateManager.enableAlpha();
			GlStateManager.enableBlend();
			IToast.Visibility itoast$visibility = this.toast.draw(BetterGuiToast.this, i - this.visibleTime);
			GlStateManager.disableBlend();
			GlStateManager.disableAlpha();
			GlStateManager.popMatrix();

			if (this.forcedShowTime > ToastControlConfig.forceTime && itoast$visibility != this.visibility) {
				this.animationTime = i - ((int) ((1.0F - this.getVisibility(i)) * 600.0F));
				this.visibility = itoast$visibility;
				this.visibility.playSound(BetterGuiToast.this.mc.getSoundHandler());
			}

			if(this.forcedShowTime > ToastControlConfig.forceTime) ToastControl.tracker.remove(this);
			
			return this.forcedShowTime > ToastControlConfig.forceTime && this.visibility == IToast.Visibility.HIDE && i - this.animationTime > 600L;
		}
	}

}
