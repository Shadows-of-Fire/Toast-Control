package dev.shadowsoffire.toastcontrol;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.joml.Matrix3x2fStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

/**
 * Drop-in replacement for vanilla's {@link ToastManager} that adds configurable toast count,
 * alternate positioning (top-down / left side / offset), no-slide mode, and a forced minimum
 * display time.
 */
public class BetterToastManager extends ToastManager {

    /** Tracks the newest-first order of toasts, used to compute stacking when {@code top_down} is on. */
    private final Deque<BetterToastInstance<?>> topDownList = new ArrayDeque<>();

    public BetterToastManager() {
        super(Minecraft.getInstance(), Minecraft.getInstance().options);
    }

    @Override
    public void update() {
        MutableBoolean soundPlayed = new MutableBoolean(false);
        this.visibleToasts.removeIf(toast -> {
            Toast.Visibility prev = toast.visibility;
            toast.update();
            if (toast.visibility != prev && soundPlayed.isFalse()) {
                soundPlayed.setTrue();
                toast.visibility.playSound(this.getMinecraft().getSoundManager());
            }

            if (toast.hasFinishedRendering()) {
                this.occupiedSlots.clear(toast.firstSlotIndex, toast.firstSlotIndex + toast.occupiedSlotCount);
                this.topDownList.remove(toast);
                return true;
            }
            return false;
        });

        if (!this.queued.isEmpty() && this.freeSlotCount() > 0) {
            this.queued.removeIf(toast -> {
                int count = toast.occcupiedSlotCount();
                int idx = this.findFreeSlotsIndex(count);
                if (idx == -1) {
                    return false;
                }

                BetterToastInstance<?> inst = new BetterToastInstance<>(toast, idx, count);
                this.visibleToasts.add(inst);
                this.occupiedSlots.set(idx, idx + count);

                // When stacking from the top, replay the slide on existing toasts so they shift down.
                if (ToastConfig.INSTANCE.topDown.get()) {
                    this.topDownList.forEach(t -> t.animationStartTime = -1L);
                }
                this.topDownList.addFirst(inst);

                SoundEvent sound = toast.getSoundEvent();
                if (sound != null && this.playedToastSounds.add(sound)) {
                    this.getMinecraft().getSoundManager().play(SimpleSoundInstance.forUI(sound, 1.0F, 1.0F));
                }
                return true;
            });
        }

        this.playedToastSounds.clear();
        if (this.nowPlayingToast != null) {
            this.nowPlayingToast.update();
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics) {
        if (!this.getMinecraft().options.hideGui) {
            int screenWidth = graphics.guiWidth();
            if (!this.visibleToasts.isEmpty()) {
                graphics.nextStratum();
            }

            for (ToastManager.ToastInstance<?> toast : this.visibleToasts) {
                toast.extractRenderState(graphics, screenWidth);
            }

            if (this.getMinecraft().options.musicToast().get().renderToast()
                && this.nowPlayingToast != null
                && (this.getMinecraft().screen == null || !(this.getMinecraft().screen instanceof PauseScreen))) {
                this.nowPlayingToast.extractRenderState(graphics, screenWidth);
            }
        }
    }

    @Override
    public void clear() {
        super.clear();
        this.topDownList.clear();
    }

    @Override
    public int findFreeSlotsIndex(int requiredCount) {
        if (this.freeSlotCount() >= requiredCount) {
            int consecutive = 0;
            int max = ToastConfig.INSTANCE.toastCount.get();
            for (int i = 0; i < max; i++) {
                if (this.occupiedSlots.get(i)) {
                    consecutive = 0;
                }
                else if (++consecutive == requiredCount) {
                    return i + 1 - consecutive;
                }
            }
        }
        return -1;
    }

    @Override
    public int freeSlotCount() {
        return ToastConfig.INSTANCE.toastCount.get() - this.occupiedSlots.cardinality();
    }

    public class BetterToastInstance<T extends Toast> extends ToastManager.ToastInstance<T> {

        protected int forcedShowTime = 0;

        protected BetterToastInstance(T toast, int firstSlotIndex, int occupiedSlotCount) {
            super(toast, firstSlotIndex, occupiedSlotCount);
        }

        @Override
        public void update() {
            this.forcedShowTime++;
            long now = Util.getMillis();
            if (this.animationStartTime == -1L) {
                this.animationStartTime = now;
                this.visibility = Toast.Visibility.SHOW;
            }

            if (this.visibility == Toast.Visibility.SHOW && now - this.animationStartTime <= 600L) {
                this.becameFullyVisibleAt = now;
            }

            this.fullyVisibleFor = now - this.becameFullyVisibleAt;
            this.calculateVisiblePortion(now);
            this.getToast().update(BetterToastManager.this, this.fullyVisibleFor);

            Toast.Visibility wanted = this.getToast().getWantedVisibility();
            // Forced display time: refuse to hide until the toast has been shown long enough.
            if (this.forcedShowTime <= ToastConfig.INSTANCE.forceTime.get()) {
                wanted = Toast.Visibility.SHOW;
            }

            if (wanted != this.visibility) {
                this.animationStartTime = now - (long) ((1.0F - this.visiblePortion) * 600.0F);
                this.visibility = wanted;
            }

            boolean wasFinished = this.hasFinishedRendering;
            this.hasFinishedRendering = this.visibility == Toast.Visibility.HIDE && now - this.animationStartTime > 600L;
            if (this.hasFinishedRendering && !wasFinished) {
                this.getToast().onFinishedRendering();
            }
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int screenWidth) {
            if (this.hasFinishedRendering) {
                return;
            }

            T toast = this.getToast();
            int width = toast.width();
            int height = toast.height();

            // Compute the slide portion per-frame (vanilla only updates visiblePortion once per tick),
            // mirroring the old getVisibility() so slides stay smooth at high framerates.
            float portion;
            if (ToastConfig.INSTANCE.noSlide.get()) {
                portion = 1.0F;
            }
            else {
                float f = Mth.clamp((float) (Util.getMillis() - this.animationStartTime) / 600.0F, 0.0F, 1.0F);
                f *= f;
                portion = this.visibility == Toast.Visibility.HIDE ? 1.0F - f : f;
            }

            Matrix3x2fStack pose = graphics.pose();
            pose.pushMatrix();

            if (ToastConfig.INSTANCE.topDown.get()) {
                int trueIdx = this.topDownIndex();
                float x = ToastConfig.INSTANCE.startLeft.get() ? 0 : screenWidth - width;
                pose.translate(x, (trueIdx - 1) * height + height * portion);
            }
            else if (ToastConfig.INSTANCE.startLeft.get()) {
                pose.translate(-width + width * portion, this.firstSlotIndex * height);
            }
            else {
                pose.translate(screenWidth - width * portion, this.firstSlotIndex * height);
            }

            pose.translate(ToastConfig.INSTANCE.offsetX.get(), ToastConfig.INSTANCE.offsetY.get());
            toast.extractRenderState(graphics, BetterToastManager.this.getMinecraft().font, this.fullyVisibleFor);
            pose.popMatrix();
        }

        /** Position of this toast within the newest-first {@link #topDownList}. */
        private int topDownIndex() {
            int trueIdx = 0;
            Iterator<BetterToastInstance<?>> it = BetterToastManager.this.topDownList.iterator();
            while (it.hasNext()) {
                if (it.next() == this) {
                    break;
                }
                trueIdx++;
            }
            return trueIdx;
        }
    }

}
