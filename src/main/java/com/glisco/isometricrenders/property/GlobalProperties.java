package com.glisco.isometricrenders.property;

import com.glisco.isometricrenders.util.FFmpegDispatcher;

public class GlobalProperties {

    //Render Options
    public static int backgroundColor = 0x000000;

    //Export Options
    public static Property<Boolean> unsafe = Property.of(false);
    public static Property<Boolean> saveIntoRoot = Property.of(true);

    public static int exportResolution = 1000;

    // Animation Options
    public static int exportFramerate = 30;
    public static int exportFrames = 60;

    public static FFmpegDispatcher.Format animationFormat = FFmpegDispatcher.Format.APNG;
}
