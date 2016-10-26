package team.dream.waterquality;

import android.hardware.Camera;

/**
 * Created by Abhilash on 18/07/2016.
 */
public class TheCamera
{

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        if(c==null) {
            try {
                c = Camera.open(); // attempt to get a Camera instance
            } catch (Exception e) {
                // Camera is not available (in use or does not exist)
            }
        }
        return c; // returns null if camera is unavailable
    }
}

