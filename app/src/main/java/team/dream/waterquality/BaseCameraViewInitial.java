package team.dream.waterquality;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Camera;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.detector.FinderPattern;
import com.google.zxing.qrcode.detector.FinderPatternFinder;
import com.google.zxing.qrcode.detector.FinderPatternInfo;

import java.io.IOException;
import java.util.List;

/**
 * Created by Abhilash on 18/07/2016.
 */
public class BaseCameraViewInitial extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private final Camera mCamera;
    private final int finderPatternColor;
    //private final Camera.Size previewSize;
    private MainActivity activity;
    private Camera.Parameters parameters;
    String TAG ="CameraActivityCopy";
    private BitMatrix bitMatrix;
    DisplayPattern listener;
    Display display;
    //private List<Camera.Size> mSupportedPreviewSizes;
    //private Camera.Size mPreviewSize;

    public BaseCameraViewInitial(Context context, Camera camera, Display display) {
        super(context);
        mCamera = camera;
        listener = (DisplayPattern) context;
        finderPatternColor = Color.parseColor("#f02cb673");
        //previewSize = camera.getParameters().getPreviewSize();
        this.display = display;

        try {
            //mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
            //for(Camera.Size str: mSupportedPreviewSizes)
            //    Log.d(TAG, str.width + "/" + str.height);
            activity = (MainActivity) context;
        }
        catch (ClassCastException e)
        {
            throw new ClassCastException("must have CameraActivity as Context.");
        }
        catch (Exception e){
            e.printStackTrace();
        }
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        /* A basic Camera preview class */
        SurfaceHolder mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Log.v(TAG,"Frame received");
        Camera.Size size = null;
        try{
            parameters = camera.getParameters();
            size = parameters.getPreviewSize();
        }
        catch (RuntimeException e){
            e.printStackTrace();
        }

        if(parameters == null || size == null){
            Log.v(TAG,"Frame skipped");
            return;
        }

        PlanarYUVLuminanceSource myYUV = new PlanarYUVLuminanceSource(data, size.width,
                size.height, 0, 0,
                (int) Math.round(size.height * Constant.CROP_FINDERPATTERN_FACTOR),
                size.height,
                false);

        BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(myYUV));

        try {
            bitMatrix = binaryBitmap.getBlackMatrix();
        } catch (NotFoundException | NullPointerException e) {
            e.printStackTrace();
        }

        if(bitMatrix != null){
            FinderPatternFinder finderPatternFinder = new FinderPatternFinder(bitMatrix);
            try {
                FinderPatternInfo info = finderPatternFinder.find(null);
            } catch (Exception e) {
                // this only means not all patterns (=4) are detected.
                Log.v(TAG,"not all patterns (=4) are detected in the current frame");
            }
            finally {
                List<FinderPattern> possibleCenters = finderPatternFinder.getPossibleCenters();
                for(int i = 0; i< possibleCenters.size(); i++) {
                    float estimatedModuleSize = possibleCenters.get(i).getEstimatedModuleSize();
                    Log.v(TAG,"Estimated module size = "+estimatedModuleSize);
                    if (estimatedModuleSize < 2) {
                        return;
                    }
                }

                if (possibleCenters != null && size != null)
                {
                    if(listener!=null) {
                        Log.v(TAG,"No. of possible centers = "+possibleCenters.size());
                        listener.showFinderPatterns(possibleCenters, size, finderPatternColor);
                    }
                }
            }
        }


        // old way of obtaining BitMatrix

        /*PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(data, size.width, size.height, 0, 0, size.width, size.height, false);
        HybridBinarizer hybBin = new HybridBinarizer(source);
        BinaryBitmap bitmap = new BinaryBitmap(hybBin);

        BitMatrix bitMatrix = null;
        try {
            bitMatrix = bitmap.getBlackMatrix();
            final FinderPatternFinder finder = new FinderPatternFinder(bitMatrix);
            FinderPatternInfo info = finder.find(null);

            Log.v(TAG,"Top Left: X: "+info.getTopLeft().getX()+" Y: "+info.getTopLeft().getY());
            Log.v(TAG,"Top Right: X: "+info.getTopRight().getX()+" Y: "+info.getTopRight().getY());
            Log.v(TAG,"Bottom Left: X: "+info.getBottomLeft().getX()+" Y: "+info.getBottomLeft().getY());
            Log.v(TAG,"Bottom Right: X: "+info.getBottomRight().getX()+" Y: "+info.getBottomRight().getY());
        } catch (NotFoundException e) {
            e.printStackTrace();
        }*/
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            mCamera.setPreviewDisplay(holder);
        } catch (Exception e) {
            Log.d("", "Error setting camera preview: " + e.getMessage());
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (holder.getSurface() == null){
            // preview surface does not exist
            return;
        }

        if(mCamera == null)
        {
            //Camera was released
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e){
            // ignore: tried to stop a non-existent preview
        }


        // set preview size and make any resize, rotate or
        // reformatting changes here

        try {
            parameters = mCamera.getParameters();
        }
        catch (Exception e)
        {
            e.printStackTrace();

        }
        if(parameters == null)
        {
            return;
        }

        //todo uncomment - part 1
        Camera.Size bestSize = null;
        List<Camera.Size> sizes = mCamera.getParameters().getSupportedPreviewSizes();
        int maxWidth = 0;
        for(Camera.Size size: sizes) {
            System.out.println("***supported preview sizes w, h: " + size.width + ", " + size.height);
            if(size.width>1300)
                continue;
            if (size.width > maxWidth) {
                bestSize = size;
                maxWidth = size.width;
            }
        }

        //todo uncomment - part 2
        //preview size
        // System.out.println("***best preview size w, h: " + bestSize.width + ", " + bestSize.height);
        assert bestSize != null;
        parameters.setPreviewSize(bestSize.width, bestSize.height);


        //portrait mode
        //mCamera.setDisplayOrientation(90);

        if(display != null){
            if(display.getRotation() == Surface.ROTATION_0)
            {
                mCamera.setDisplayOrientation(90);
            }

            if(display.getRotation() == Surface.ROTATION_90)
            {
            }

            if(display.getRotation() == Surface.ROTATION_180)
            {
            }

            if(display.getRotation() == Surface.ROTATION_270)
            {
                mCamera.setDisplayOrientation(180);
            }
        }
        else{
            mCamera.setDisplayOrientation(90);
        }

        //if(mPreviewSize == null){
        //    throw new NullPointerException("mPreviewSize is null");
        //}

        //parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);


        boolean canAutoFocus = false;
        boolean disableContinuousFocus = true;
        List<String> modes = parameters.getSupportedFocusModes();
        for(String s: modes) {

            System.out.println("***supported focus modes: " + s);

            if(s.equals(Camera.Parameters.FOCUS_MODE_AUTO))
            {
                canAutoFocus = true;

            }
            if(s.equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))
            {
                disableContinuousFocus = false;
            }
        }

        try {
            CameraConfigurationUtils.setFocus(parameters, canAutoFocus, disableContinuousFocus, false);

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        //white balance
        if(parameters.getWhiteBalance()!=null)
        {
            //TODO check if this optimise the code
            parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
        }

        try {
            mCamera.setParameters(parameters);
        } catch (Exception e){
            Log.d("", "Error setting camera parameters: " + e.getMessage());
        }

        try {
            mCamera.setPreviewDisplay(holder);
            activity.setPreviewProperties();
            mCamera.setPreviewCallback(this);
            //mCamera.setOneShotPreviewCallback(this);
            mCamera.startPreview();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*@Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        setMeasuredDimension(width,height);

        if (mSupportedPreviewSizes != null) {
            mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width, height);
        }

        float ratio;
        if(mPreviewSize.height >= mPreviewSize.width)
            ratio = (float) mPreviewSize.height / (float) mPreviewSize.width;
        else
            ratio = (float) mPreviewSize.width / (float) mPreviewSize.height;

        float camHeight = (int) (width * ratio);
        float newCamHeight;
        float newHeightRatio;

        if (camHeight < height) {
            newHeightRatio = (float) height / (float) mPreviewSize.height;
            newCamHeight = (newHeightRatio * camHeight);
            Log.d(TAG, camHeight + " " + height + " " + mPreviewSize.height + " " + newHeightRatio + " " + newCamHeight);
            setMeasuredDimension((int) (width * newHeightRatio), (int) newCamHeight);
            Log.d(TAG, mPreviewSize.width + " | " + mPreviewSize.height + " | ratio - " + ratio + " | H_ratio - " + newHeightRatio + " | A_width - " + (width * newHeightRatio) + " | A_height - " + newCamHeight);
        } else {
            newCamHeight = camHeight;
            setMeasuredDimension(width, (int) newCamHeight);
            Log.d(TAG, mPreviewSize.width + " | " + mPreviewSize.height + " | ratio - " + ratio + " | A_width - " + (width) + " | A_height - " + newCamHeight);
        }
    }*/

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) h / w;

        if (sizes == null)
            return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.height / size.width;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;

            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }

        return optimalSize;
    }
}
