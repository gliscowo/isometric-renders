package com.glisco.isometricrenders.client.gui;

import net.minecraft.text.Text;

/**
 * Defines how a given scene is lit
 */
public interface LightingProfile {

    /**
     * This method is called when the scene is being set up
     */
    void setup();

    /**
     * @return the name of this profile for displaying inside the GUI
     */
    Text getFriendlyName();

    /**
     * This method is called when the scene is being set up for use with the external renderer. The default implementation will simply call {@link LightingProfile#setup()}
     */
    default void setupForExternal() {
        setup();
    }

}
