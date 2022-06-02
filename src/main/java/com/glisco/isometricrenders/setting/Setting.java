package com.glisco.isometricrenders.setting;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class Setting<T> {

    protected T defaultValue;
    protected T value;
    protected final List<BiConsumer<Setting<T>, T>> changeListeners;

    public Setting(T defaultValue) {
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.changeListeners = new ArrayList<>();
    }

    public static <T> Setting<T> of(T defaultValue) {
        return new Setting<>(defaultValue);
    }

    public void set(T value) {
        this.value = value;
        this.invokeListeners();
    }

    public void setToDefault() {
        this.value = this.defaultValue;
        this.invokeListeners();
    }

    public void listen(BiConsumer<Setting<T>, T> listener) {
        this.changeListeners.add(listener);
        listener.accept(this, this.value);
    }

    public T get() {
        return value;
    }

    protected void invokeListeners() {
//        this.changeListeners.removeIf(biConsumerWeakReference -> biConsumerWeakReference.get() == null);
        this.changeListeners.forEach(tConsumer -> tConsumer.accept(this, this.value));
    }
}
