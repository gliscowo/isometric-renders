package com.glisco.isometricrenders.property;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class Property<T> implements BiConsumer<Property<T>, T> {

    protected T defaultValue;
    protected T value;
    protected final List<BiConsumer<Property<T>, T>> changeListeners;

    public Property(T defaultValue) {
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.changeListeners = new ArrayList<>();
    }

    public static <T> Property<T> of(T defaultValue) {
        return new Property<>(defaultValue);
    }

    public void set(T value) {
        this.value = value;
        this.invokeListeners();
    }

    public Property<T> setDefaultValue(T defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public void setToDefault() {
        this.value = this.defaultValue;
        this.invokeListeners();
    }

    public void listen(BiConsumer<Property<T>, T> listener) {
        this.changeListeners.add(listener);
        listener.accept(this, this.value);
    }

    public T get() {
        return value;
    }

    public void copyFrom(Property<T> source) {
        this.defaultValue = source.defaultValue;
        this.value = source.value;
        this.invokeListeners();
    }

    protected void invokeListeners() {
        this.changeListeners.forEach(tConsumer -> tConsumer.accept(this, this.value));
    }

    @Override
    public void accept(Property<T> tProperty, T t) {
        this.set(t);
    }
}
