package shadows.toaster;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Iterator;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.util.Mth;

public class BetterToastComponent extends ToastComponent {

	private Deque<ToastInstance<?>> topDownList = new ArrayDeque<>();

	public BetterToastComponent() {
		super(Minecraft.getInstance());
		this.queued = new ControlledDeque();
		this.visible = new BetterToastInstance[ToastConfig.INSTANCE.toastCount.get()];
	}

	@Override
	public void render(PoseStack stack) {
		if (!this.minecraft.options.hideGui) {

			for (int i = 0; i < this.visible.length; ++i) {
				ToastInstance<?> toastinstance = this.visible[i];

				if (toastinstance != null && toastinstance.render(this.minecraft.getWindow().getGuiScaledWidth(), i, stack)) {
					this.visible[i] = null;
					topDownList.removeLast();
				}

				if (this.visible[i] == null && !this.queued.isEmpty()) {
					this.visible[i] = new BetterToastInstance<>(this.queued.removeFirst());
					topDownList.forEach(t -> t.animationTime = -1L);
					topDownList.addFirst(this.visible[i]);
				}
			}
		}
	}

	@Override
	public void clear() {
		Arrays.fill(this.visible, null);
		this.queued.clear();
	}

	public class BetterToastInstance<T extends Toast> extends ToastInstance<T> {

		protected int forcedShowTime = 0;

		protected BetterToastInstance(T toast) {
			super(toast);
			ToastControl.tracker.add(this);
		}

		public void tick() {
			forcedShowTime++;
		}

		protected float getVisibility(long sysTime) {
			float f = Mth.clamp((sysTime - this.animationTime) / 600F, 0F, 1F);
			f = f * f;
			if (ToastConfig.INSTANCE.noSlide.get()) return 1;
			return this.forcedShowTime > ToastConfig.INSTANCE.forceTime.get() && this.visibility == Toast.Visibility.HIDE ? 1F - f : f;
		}

		@SuppressWarnings("deprecation")
		@Override
		public boolean render(int scaledWidth, int arrayPos, PoseStack pStack) {
			long i = Util.getMillis();

			if (this.animationTime == -1L) {
				this.animationTime = i;
				this.visibility.playSound(BetterToastComponent.this.minecraft.getSoundManager());
			}

			if (this.visibility == Toast.Visibility.SHOW && i - this.animationTime <= 600L) {
				this.visibleTime = i;
			}

			PoseStack stack = RenderSystem.getModelViewStack();
			stack.pushPose();
			if (ToastConfig.INSTANCE.topDown.get()) {
				int trueIdx = 0;
				Iterator<ToastInstance<?>> it = topDownList.iterator();
				while (it.hasNext()) {
					if (it.next() == this) break;
					trueIdx++;
				}
				stack.translate(scaledWidth - this.toast.width(), (trueIdx - 1) * this.toast.height() + this.toast.height() * this.getVisibility(i), 800 + arrayPos);
			} else if (ToastConfig.INSTANCE.startLeft.get()) stack.translate(-this.toast.width() + this.toast.width() * this.getVisibility(i), arrayPos * this.toast.height(), 800 + arrayPos);
			else stack.translate(scaledWidth - this.toast.width() * this.getVisibility(i), arrayPos * this.toast.height(), 800 + arrayPos);
			stack.translate(ToastConfig.INSTANCE.offsetX.get(), ToastConfig.INSTANCE.offsetY.get(), 0);
			RenderSystem.applyModelViewMatrix();
			Toast.Visibility itoast$visibility = toast.render(pStack, BetterToastComponent.this, i - this.visibleTime);
			stack.popPose();
			RenderSystem.applyModelViewMatrix();

			if (this.forcedShowTime > ToastConfig.INSTANCE.forceTime.get() && itoast$visibility != this.visibility) {
				this.animationTime = i - ((long) ((1 - this.getVisibility(i)) * 600));
				this.visibility = itoast$visibility;
				this.visibility.playSound(BetterToastComponent.this.minecraft.getSoundManager());
				if (ToastConfig.INSTANCE.topDown.get()) {
					ToastControl.tracker.remove(this);
					return true;
				}
			}

			if (this.forcedShowTime > ToastConfig.INSTANCE.forceTime.get()) ToastControl.tracker.remove(this);

			return this.forcedShowTime > ToastConfig.INSTANCE.forceTime.get() && this.visibility == Toast.Visibility.HIDE && i - this.animationTime > 600L;
		}
	}

}
