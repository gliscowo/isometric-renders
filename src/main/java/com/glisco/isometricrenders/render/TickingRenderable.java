package com.glisco.isometricrenders.render;

import com.glisco.isometricrenders.property.PropertyBundle;

public interface TickingRenderable<P extends PropertyBundle> extends Renderable<P> {

    void tick();

}
