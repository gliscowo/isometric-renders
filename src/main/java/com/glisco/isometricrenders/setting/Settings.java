package com.glisco.isometricrenders.setting;

import com.glisco.isometricrenders.render.DefaultLightingProfiles;
import com.glisco.isometricrenders.render.LightingProfile;

public class Settings {

    //Transform Options
    public static IntSetting rotation = IntSetting.of(225, 0, 360).withRollover();
    public static IntSetting angle = IntSetting.of(30, -90, 90);
    public static IntSetting renderScale = IntSetting.of(150, 1, 450);
    public static IntSetting renderHeight = IntSetting.of(0, 0, 600);

    //Atlas Options
    public static IntSetting atlasColumns = IntSetting.of(12, 1, 1000);
    public static IntSetting atlasShift = IntSetting.of(0, -450, 450);
    public static IntSetting atlasHeight = IntSetting.of(0, -450, 450);
    public static IntSetting atlasScale = IntSetting.of(25, 0, 100);

    //Render Options
    public static int backgroundColor = 0x0000ff;
    public static LightingProfile lightingProfile = DefaultLightingProfiles.FLAT;

    //Export Options
    public static Setting<Boolean> useExternalRenderer = Setting.of(true);
    public static Setting<Boolean> allowMultipleNonThreadedJobs = Setting.of(false);
    public static Setting<Boolean> allowInsaneResolutions = Setting.of(false);
    public static Setting<Boolean> dumpIntoRoot = Setting.of(false);
    public static int exportResolution = 2048;

    public static boolean toggleInsaneResolutions() {
        allowInsaneResolutions.set(!allowInsaneResolutions.get());
        return allowInsaneResolutions.get();
    }

}
