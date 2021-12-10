package shadows.toaster;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Iterator;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.toasts.IToast;
import net.minecraft.client.gui.toasts.ToastGui;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

public class BetterGuiToast extends ToastGui {

	private Deque<ToastInstance<?>> topDownList = new ArrayDeque<>();

	public BetterGuiToast() {
		super(Minecraft.getInstance());
		this.queued = new ControlledDeque();
		this.visible = new BetterToastInstance[ToastConfig.INSTANCE.toastCount.get()];
	}

	@Override
	public void render(MatrixStack stack) {
		if (!this.minecraft.options.hideGui) {
			RenderHelper.turnOff();

			for (int i = 0; i < this.visible.length; ++i) {
				ToastInstance<?> toastinstance = this.visible[i];

				if (toastinstance != null && toastinstance.render(this.minecraft.getWindow().getGuiScaledWidth(), i, stack)) {
					this.visible[i] = null;
					topDownList.removeLast();
				}

				if (this.visible[i] == null && !this.queued.isEmpty()) {
					this.visible[i] = new BetterToastInstance<>(this.queued.removeFirst());
					topDownList.addFirst(this.visible[i]);
					topDownList.forEach(t -> t.animationTime = -1L);
				}
			}
		}
	}

	@Override
	public void clear() {
		Arrays.fill(this.visible, null);
		this.queued.clear();
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

		@SuppressWarnings("deprecation")
		@Override
		public boolean render(int scaledWidth, int arrayPos, MatrixStack stack) {
			long i = Util.getMillis();

			if (this.animationTime == -1L) {
				this.animationTime = i;
				this.visibility.playSound(BetterGuiToast.this.minecraft.getSoundManager());
			}

			if (this.visibility == IToast.Visibility.SHOW && i - this.animationTime <= 600L) {
				this.visibleTime = i;
			}

			RenderSystem.pushMatrix();

			if (ToastConfig.INSTANCE.topDown.get()) {
				int trueIdx = 0;
				Iterator<ToastInstance<?>> it = topDownList.iterator();
				while (it.hasNext()) {
					if (it.next() == this) break;
					trueIdx++;
				}
				RenderSystem.translatef(scaledWidth - 160F, (trueIdx - 1) * 32 + 32 * this.getVisibility(i), 500 + arrayPos);
			} else if (ToastConfig.INSTANCE.startLeft.get()) RenderSystem.translatef(-160 + 160 * this.getVisibility(i), arrayPos * 32, 500 + arrayPos);
			else RenderSystem.translatef(scaledWidth - 160F * this.getVisibility(i), arrayPos * 32, 500 + arrayPos);
			RenderSystem.enableBlend();
			RenderSystem.translatef(ToastConfig.INSTANCE.offsetX.get(), ToastConfig.INSTANCE.offsetY.get(), 0);
			IToast.Visibility itoast$visibility = toast.render(stack, BetterGuiToast.this, i - this.visibleTime);
			RenderSystem.disableBlend();
			RenderSystem.popMatrix();

			if (this.forcedShowTime > ToastConfig.INSTANCE.forceTime.get() && itoast$visibility != this.visibility) {
				this.animationTime = i - ((long) ((1 - this.getVisibility(i)) * 600));
				this.visibility = itoast$visibility;
				this.visibility.playSound(BetterGuiToast.this.minecraft.getSoundManager());
			}

			if (this.forcedShowTime > ToastConfig.INSTANCE.forceTime.get()) ToastControl.tracker.remove(this);

			return this.forcedShowTime > ToastConfig.INSTANCE.forceTime.get() && this.visibility == IToast.Visibility.HIDE && i - this.animationTime > 600L;
		}
	}

}
