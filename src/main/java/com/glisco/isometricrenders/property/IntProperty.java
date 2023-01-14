package com.glisco.isometricrenders.property;

import net.minecraft.util.math.MathHelper;

public class IntProperty extends Property<Integer> {

    private final int max;
    private final int min;
    private final int span;

    private boolean allowRollover = false;

    private IntProperty(int defaultValue, int min, int max) {
        super(defaultValue);

        this.min = min;
        this.max = max;

        this.span = this.max - this.min;
    }

    public static IntProperty of(int defaultValue, int min, int max) {
        if (min >= max) {
            throw new IllegalArgumentException("'min' must be less than 'max'");
        }

        return new IntProperty(defaultValue, min, max);
    }

    public IntProperty withRollover() {
        this.allowRollover = true;
        return this;
    }

    public void modify(int by) {
        if (allowRollover) {
            this.value += by;
            if (this.value > this.max) this.value -= this.span;
            if (this.value < this.min) this.value += this.span;
        } else {
            this.value = MathHelper.clamp(this.value + by, this.min, this.max);
        }

        this.invokeListeners();
    }

    public double progress() {
        return (this.value - this.min) / (double) this.span;
    }

    public void setFromProgress(double progress) {
        this.value = (int) Math.round(this.min + progress * this.span);
        this.invokeListeners();
    }

    public int max() {
        return max;
    }

    public int min() {
        return min;
    }
}
