package com.glisco.isometricrenders.client.gui;

/**
 * Defines how a given scene is lit
 */
public abstract class LightingProfile {

    /**
     * This method is called when the scene is being set up
     */
    public abstract void setup();

    /**
     * This method is called when the scene is being set up for use with the external renderer. The default implementation will simply call {@link LightingProfile#setup()}
     */
    public void setupForExternal(){
        setup();
    }

}
