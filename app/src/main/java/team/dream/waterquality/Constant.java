package team.dream.waterquality;

/**
 * Created by Abhilash on 17/07/2016.
 */
public class Constant {
    public static final double MAX_LUM_LOWER = 150;
    public static final double MAX_LUM_UPPER = 250;
    public static final double MAX_LUM_PERCENTAGE = (MAX_LUM_UPPER / 255d) * 100;
    public static final double MAX_SHADOW_PERCENTAGE = 10;
    public static final double MIN_FOCUS_PERCENTAGE = 70;
    public static final double CONTRAST_DEVIATION_FRACTION = 0.05;
    public static final double CONTRAST_MAX_DEVIATION_FRACTION = 0.20;
    public static final int COUNT_QUALITY_CHECK_LIMIT = 15;
    public static final double CROP_CAMERAVIEW_FACTOR = 0.6;
    public static final double CROP_FINDERPATTERN_FACTOR = 0.75;
    public static final float MAX_TILT_DIFF = 0.03f;
    public static final int PIXEL_MARGIN_STRIP_AREA_WIDTH = 6;
    public static final int PIXEL_MARGIN_STRIP_AREA_HEIGHT = 4;

}

