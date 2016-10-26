package team.dream.waterquality;

import android.hardware.Camera;

import com.google.zxing.qrcode.detector.FinderPattern;

import java.util.List;

/**
 * Created by Abhilash on 18/07/2016.
 */
public interface DisplayPattern {

    void showFinderPatterns(final List<FinderPattern> patterns, final Camera.Size size, final int color);

    void getCameraPreviewSize(final int height, final int width);
}
