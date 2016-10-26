package team.dream.waterquality;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Camera;
import android.os.Build;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.detector.FinderPattern;
import com.google.zxing.qrcode.detector.FinderPatternFinder;
import com.google.zxing.qrcode.detector.FinderPatternInfo;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Abhilash on 18/07/2016.
 */
public class BaseCameraView extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {
    private String TAG ="CameraActivityCopy";
    private final Camera mCamera;
    private final int finderPatternColor;
    //private final Camera.Size previewSize;
    private MainActivity activity;
    private Camera.Parameters parameters;
    private BitMatrix bitMatrix;
    DisplayPattern listener;
    Display display;
    private List<Camera.Size> mSupportedPreviewSizes;
    private Camera.Size mPreviewSize;


    public BaseCameraView(Context context, Camera camera, Display display) {
        super(context);
        mCamera = camera;

        listener = (DisplayPattern) context;
        this.display = display;

        finderPatternColor = Color.parseColor("#f02cb673");
        //previewSize = camera.getParameters().getPreviewSize();

        //Log.v("size","previewSize height "+previewSize.height+" width = "+previewSize.width );

        try {
            mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
            for(Camera.Size str: mSupportedPreviewSizes)
                Log.d(TAG, str.width + "/" + str.height);

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
        //Log.v(TAG,"Frame received");
        //Camera.Size sizeLocal = null;
        try{
            parameters = camera.getParameters();
            //sizeLocal = parameters.getPreviewSize();
            //Log.v("size","sizeLocal height "+sizeLocal.height+" width = "+sizeLocal.width );
        }
        catch (RuntimeException e){
            e.printStackTrace();
        }

        if(parameters == null || mPreviewSize == null){
            Log.v(TAG,"Frame skipped");
            return;
        }

        //Log.v("abcxyz","PlanarYUVLuminanceSource size. height: "+mPreviewSize.height+", width: "+mPreviewSize.width);

        PlanarYUVLuminanceSource myYUV = new PlanarYUVLuminanceSource(data, mPreviewSize.width,
                mPreviewSize.height, 0, 0,
                //(int) Math.round(mPreviewSize.height * Constant.CROP_FINDERPATTERN_FACTOR),
                mPreviewSize.width,
                mPreviewSize.height,
                false);

        BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(myYUV));

        try {
            bitMatrix = binaryBitmap.getBlackMatrix();
            //Log.v("abcxyz","binaryBitmap size. height: "+binaryBitmap.getHeight()+", width: "+binaryBitmap.getWidth());
            //Log.v("abcxyz","bitmatrix size. height: "+bitMatrix.getHeight()+", width: "+bitMatrix.getWidth());
        } catch (NotFoundException | NullPointerException e) {
            e.printStackTrace();
        }

        if(bitMatrix != null){
            FinderPatternFinder finderPatternFinder = new FinderPatternFinder(bitMatrix);
            try {
                FinderPatternInfo info = finderPatternFinder.find(null);
            } catch (Exception e) {
                // this only means not all patterns (=4) are detected.
                //Log.v(TAG,"not all patterns (=4) are detected in the current frame");
            } finally {
                List<FinderPattern> possibleCenters = finderPatternFinder.getPossibleCenters();
                for(int i = 0; i< possibleCenters.size(); i++) {
                    float estimatedModuleSize = possibleCenters.get(i).getEstimatedModuleSize();
                    //Log.v(TAG,"Estimated module size = "+estimatedModuleSize);
                    if (estimatedModuleSize < 2) {
                        return;
                    }
                }

                if (possibleCenters != null && mPreviewSize != null)
                {
                    if(listener!=null) {
                        //Log.v(TAG,"No. of possible centers = "+possibleCenters.size());
                        listener.showFinderPatterns(possibleCenters, mPreviewSize, finderPatternColor);
                    }
                    else{
                        throw new NullPointerException("DisplayPattern listener is NULL");
                    }
                }
            }
        }


        // old way of obtaining BitMatrix

        /*

        PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(data, size.width, size.height, 0, 0, size.width, size.height, false);
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
        }

        */
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
        /*Camera.Size bestSize = null;
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
        }*/

        //todo uncomment - part 2
        //preview size
        // System.out.println("***best preview size w, h: " + bestSize.width + ", " + bestSize.height);
        //assert bestSize != null;
        //parameters.setPreviewSize(bestSize.width, bestSize.height);


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

        if(mPreviewSize == null){
            throw new NullPointerException("mPreviewSize is null");
        }
        parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);


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

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec,heightMeasureSpec);
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        setMeasuredDimension(width,height);

        if (mSupportedPreviewSizes != null) {
            mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width, height);
            listener.getCameraPreviewSize(mPreviewSize.height,mPreviewSize.width);
            //mPreviewSize = getOptimalPreviewSize2(display.getOrientation(),width,height,mCamera.getParameters());
            Log.v("size","mPreviewSize height "+mPreviewSize.height+" width = "+mPreviewSize.width );
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
    }

    public static Camera.Size getOptimalPreviewSize2(int displayOrientation, int width, int height, Camera.Parameters parameters) {
        final double ASPECT_TOLERANCE=0.1;
        double targetRatio=(double)width / height;
        List<Camera.Size> sizes=parameters.getSupportedPreviewSizes();
        Camera.Size optimalSize=null;
        double minDiff=Double.MAX_VALUE;
        int targetHeight=height;

        if (displayOrientation == 90 || displayOrientation == 270) {
            targetRatio=(double)height / width;
        }

        // Try to find an size match aspect ratio and size

        for (Camera.Size size : sizes) {
            double ratio=(double)size.width / size.height;

            if (Math.abs(ratio - targetRatio) <= ASPECT_TOLERANCE) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize=size;
                    minDiff=Math.abs(size.height - targetHeight);
                }
            }
        }

        // Cannot find the one match the aspect ratio, ignore
        // the requirement

        if (optimalSize == null) {
            minDiff=Double.MAX_VALUE;

            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize=size;
                    minDiff=Math.abs(size.height - targetHeight);
                }
            }
        }

        return(optimalSize);
    }


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

    private static class SizeComparator implements
            Comparator<Camera.Size> {
        @Override
        public int compare(Camera.Size lhs, Camera.Size rhs) {
            int left=lhs.width * lhs.height;
            int right=rhs.width * rhs.height;

            if (left < right) {
                return(-1);
            }
            else if (left > right) {
                return(1);
            }

            return(0);
        }
    }

    public static Camera.Size getBestAspectPreviewSize(int displayOrientation,
                                                       int width,
                                                       int height,
                                                       Camera.Parameters parameters) {
        return(getBestAspectPreviewSize(displayOrientation, width, height,
                parameters, 0.0d));
    }

    public static Camera.Size getBestAspectPreviewSize(int displayOrientation,
                                                       int width,
                                                       int height,
                                                       Camera.Parameters parameters,
                                                       double closeEnough) {
        double targetRatio=(double)width / height;
        Camera.Size optimalSize=null;
        double minDiff=Double.MAX_VALUE;

        if (displayOrientation == 90 || displayOrientation == 270) {
            targetRatio=(double)height / width;
        }

        List<Camera.Size> sizes=parameters.getSupportedPreviewSizes();

        Collections.sort(sizes,
                Collections.reverseOrder(new SizeComparator()));

        for (Camera.Size size : sizes) {
            double ratio=(double)size.width / size.height;

            if (Math.abs(ratio - targetRatio) < minDiff) {
                optimalSize=size;
                minDiff=Math.abs(ratio - targetRatio);
            }

            if (minDiff < closeEnough) {
                break;
            }
        }

        return(optimalSize);
    }

    public void switchFlashMode()
    {
        if(mCamera==null)
            return;
        parameters = mCamera.getParameters();

        String flashmode = mCamera.getParameters().getFlashMode().equals(Camera.Parameters.FLASH_MODE_OFF)?
                Camera.Parameters.FLASH_MODE_TORCH: Camera.Parameters.FLASH_MODE_OFF;
        parameters.setFlashMode(flashmode);

        mCamera.setParameters(parameters);
    }


    //exposure compensation
    public void adjustExposure(int direction) throws RuntimeException
    {
        if(mCamera==null)
            return;

        //parameters = mCamera.getParameters();
        mCamera.cancelAutoFocus();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {

            if(!parameters.getAutoExposureLock()) {
                parameters.setAutoExposureLock(true);
                mCamera.setParameters(parameters);
                System.out.println("***locking auto-exposure. ");
            }
        }


        int compPlus = Math.min(parameters.getMaxExposureCompensation(), Math.round(parameters.getExposureCompensation() + 1));
        int compMinus = Math.max(parameters.getMinExposureCompensation(), Math.round(parameters.getExposureCompensation() - 1));

        if(direction > 0)
        {
            parameters.setExposureCompensation(compPlus);
        }
        else if(direction < 0)
        {
            parameters.setExposureCompensation(compMinus);
        }
        else if(direction == 0) {
            parameters.setExposureCompensation(0);
        }

        //System.out.println("***Exposure compensation index: " + parameters.getExposureCompensation());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {

            if(parameters.getAutoExposureLock()) {
                parameters.setAutoExposureLock(false);
                mCamera.setParameters(parameters);
                System.out.println("***unlocking auto-exposure. ");
            }
        }else {

            mCamera.setParameters(parameters);
        }
    }

    public void setFocusAreas(List<Camera.Area> areas)
    {
        if(mCamera==null)
            return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {

            if(parameters.getMaxNumFocusAreas() > 0 && areas != null && areas.size() > 0) {
                try {
                    //make sure area list does not exceed max num areas allowed
                    int length = Math.min(areas.size(), mCamera.getParameters().getMaxNumFocusAreas());
                    List<Camera.Area> subAreas = areas.subList(0, length);

                    mCamera.cancelAutoFocus();

                    //parameters = mCamera.getParameters();
                    parameters.setFocusAreas(subAreas);
                    mCamera.setParameters(parameters);

                } catch (Exception e) {
                    System.out.println("***Exception setting parameters for focus areas.");
                    e.printStackTrace();

                }
            }
        }
    }
}
