package team.dream.waterquality;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.hardware.Camera;
import android.media.MediaScannerConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.UiThread;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.common.PerspectiveTransform;
import com.google.zxing.qrcode.detector.FinderPattern;
import com.google.zxing.qrcode.detector.FinderPatternFinder;
import com.google.zxing.qrcode.detector.FinderPatternInfo;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Abhilash on 18/07/2016.
 */

public class MainActivity extends AppCompatActivity implements DisplayPattern, Camera.PictureCallback{

    private FinderPatternIndicatorView finderPatternIndicatorView;
    private BaseCameraView baseCameraView;
    //private BaseCameraViewInitial baseCameraView;
    private ShowFinderPatternRunnable showFinderPatternRunnable;
    private WeakReference<MainActivity> mActivity;
    private CameraScheduledExecutorService cameraScheduledExecutorService;
    private Camera mCamera;
    private WeakReference<Camera> wrCamera;
    private FrameLayout previewLayout;
    private int previewFormat= -1;
    private int previewWidth = 0;
    private int previewHeight = 0;
    private final MyHandler handler = new MyHandler();
    private Button captureButton;
    int scaleHeight = -1;
    int scaleWidth = -1;
    List<FinderPattern> patterns;
    private Bitmap card = null;

    String TAG ="MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        finderPatternIndicatorView = (FinderPatternIndicatorView) findViewById(R.id.activity_cameraFinderPatternIndicatorView);
        Log.v("pattern","height: "+finderPatternIndicatorView.getMeasuredHeight()+" width: "+finderPatternIndicatorView.getMeasuredWidth());
        showFinderPatternRunnable = new ShowFinderPatternRunnable();

        mActivity = new WeakReference<MainActivity>(this);

        cameraScheduledExecutorService = new CameraScheduledExecutorService();

        captureButton = (Button) findViewById(R.id.captureButton);

    }

    private void init() {
        // Create an instance of Camera
        mCamera = TheCamera.getCameraInstance();

        if (mCamera == null) {
            Toast.makeText(this.getApplicationContext(), "Could not instantiate the camera", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            try {
                wrCamera = new WeakReference<Camera>(mCamera);

                // Create our Preview view and set it as the content of our activity.
                Display display = ((WindowManager)getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
                //baseCameraView = new BaseCameraViewInitial(this, mCamera, display);
                baseCameraView = new BaseCameraView(this, mCamera, display);
                //FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,FrameLayout.LayoutParams.WRAP_CONTENT);
                //baseCameraView.setLayoutParams(params);
                previewLayout = (FrameLayout) findViewById(R.id.camera_preview);
                previewLayout.removeAllViews();
                previewLayout.addView(baseCameraView);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void showFinderPatterns(List<FinderPattern> patterns, Camera.Size size, int color) {
        //Log.v(TAG,"showFinderPatterns called");
        if(handler!=null) {
            this.patterns = patterns;
            showFinderPatternRunnable.setPatterns(patterns);
            showFinderPatternRunnable.setSize(size);
            showFinderPatternRunnable.setColor(color);
            handler.post(showFinderPatternRunnable);
        }
    }

    public void onCapture(View v){
        //Toast.makeText(MainActivity.this, "Capture clicked", Toast.LENGTH_SHORT).show();
        if(mCamera != null && baseCameraView != null && previewLayout != null){
            mCamera.takePicture(null,null,this);
        }
    }


    /*@Override
    public void onPictureTaken(byte[] data, Camera camera){
        if(data == null){
            Log.v("abcxyz","onPictureTaken data = null");
            return;
        }
        Toast.makeText(MainActivity.this, "onPictureTaken called", Toast.LENGTH_SHORT).show();
        Camera.Size mPreviewSize = camera.getParameters().getPreviewSize();
        PlanarYUVLuminanceSource myYUV = new PlanarYUVLuminanceSource(data, mPreviewSize.width,
                mPreviewSize.height, 0, 0,
                //(int) Math.round(mPreviewSize.height * Constant.CROP_FINDERPATTERN_FACTOR),
                mPreviewSize.width,
                mPreviewSize.height,
                false);

        BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(myYUV));
        //binaryBitmap = rotateBinaryBitmap(binaryBitmap);
        BitMatrix bitMatrix = null;
        try {
            bitMatrix = binaryBitmap.getBlackMatrix();
            Log.v("abcxyz","MainActivity binaryBitmap size. height: "+binaryBitmap.getHeight()+", width: "+binaryBitmap.getWidth());
            Log.v("abcxyz","MainActivity bitmatrix size. height: "+bitMatrix.getHeight()+", width: "+bitMatrix.getWidth());
        } catch (NotFoundException | NullPointerException e) {
            Log.e("test",e.getMessage());
            e.printStackTrace();
        }

        if(bitMatrix != null){

            FinderPatternFinder finderPatternFinder = new FinderPatternFinder(bitMatrix);
            try {
                FinderPatternInfo info = finderPatternFinder.find(null);
            } catch (Exception e) {
                // this only means not all patterns (=4) are detected.

                Log.v(TAG,"not all patterns (=4) are detected in the current frame");
            } finally {

                List<FinderPattern> possibleCenters = finderPatternFinder.getPossibleCenters();
                Log.v("abcxyz","possibleCenters size = "+ possibleCenters.size());
                *//*for(int i = 0; i< possibleCenters.size(); i++) {
                    float estimatedModuleSize = possibleCenters.get(i).getEstimatedModuleSize();
                    Log.v(TAG,"Estimated module size = "+estimatedModuleSize);
                    if (estimatedModuleSize < 2) {
                        Log.v("abcxyz","called1");
                        return;
                    }
                }*//*

                if (possibleCenters != null && mPreviewSize != null)
                {

                    if(possibleCenters.size() == 4){
                        Log.v("abcxyz","card size: 0-x = "+possibleCenters.get(0).getX()+", 0-y = "+possibleCenters.get(0).getY());
                        Log.v("abcxyz","card size: 1-x = "+possibleCenters.get(1).getX()+", 1-y = "+possibleCenters.get(1).getY());
                        Log.v("abcxyz","card size: 2-x = "+possibleCenters.get(2).getX()+", 2-y = "+possibleCenters.get(2).getY());
                        Log.v("abcxyz","card size: 3-x = "+possibleCenters.get(3).getX()+", 3-y = "+possibleCenters.get(3).getY());
                        Bitmap bitmap = BitmapFactory.decodeByteArray(data , 0, data.length);
                        int width = Math.abs((int) (possibleCenters.get(1).getX() - possibleCenters.get(0).getX()));
                        int height =Math.abs((int) (possibleCenters.get(2).getY() - possibleCenters.get(0).getY()));
                        Bitmap croppedBitmap = Bitmap.createBitmap(bitmap,(int) possibleCenters.get(0).getX(),(int)possibleCenters.get(0).getY(),width,height);

                        File file=new File(Environment.getExternalStorageDirectory()+"/Pictures/WaterQuality");
                        if(!file.isDirectory()){
                            file.mkdir();
                        }

                        file=new File(Environment.getExternalStorageDirectory()+"/Pictures/WaterQuality","latest.png");

                        try
                        {

                            FileOutputStream fileOutputStream=new FileOutputStream(file);
                            croppedBitmap.compress(Bitmap.CompressFormat.PNG,100, fileOutputStream);
                            fileOutputStream.flush();
                            fileOutputStream.close();
                            MediaScannerConnection.scanFile(this, new String[] {file.getAbsolutePath() }, null, null);

                            Intent i = new Intent(this, ProcessImageActivity.class);
                            Bundle bundle = new Bundle();
                            bundle.putString("photoPath", file.getAbsolutePath());
                            i.putExtras(bundle);
                            super.onResume();
                            startActivity(i);

                        }
                        catch(IOException e){
                            e.printStackTrace();
                        }
                        catch(Exception exception)
                        {
                            exception.printStackTrace();
                        }

                    }else {
                        Log.e("test","possibleCenters.size() != 4");
                        Toast.makeText(MainActivity.this, "possibleCenters.size() != 4", Toast.LENGTH_SHORT).show();
                    }

                }
                else{
                    Log.v("abcxyz","possibleCenters == null OR mPreviewSize == null");
                    Toast.makeText(MainActivity.this, "possibleCenters == null OR mPreviewSize == null", Toast.LENGTH_SHORT).show();
                }
            }
        }
        else{
            Log.e("test","bitmatrix is null");
            Toast.makeText(MainActivity.this, "bitmatrix is null", Toast.LENGTH_SHORT).show();
        }
    }*/

    @Override
    public void getCameraPreviewSize(int height, int width) {
        //reversed since initially card is at 90 degrees
        scaleWidth = height;
        scaleHeight = width;
    }

    private class AnalyseImageTask extends AsyncTask<byte[],Void,Void>{
        File calibratedFile = null, rawFile = null, originalFile = null;
        ProgressDialog dialog = null;
        Context context;
        float averageModuleSize = -1;

        BitMatrix bitMatrix;
        FinderPatternFinder finderPatternFinder;
        List<FinderPattern> patterns;
        String json = null;

        public String loadPointsJSONFromAsset() {
            String json = null;
            try {
                InputStream is = getAssets().open("points2.json");
                //InputStream is = getAssets().open("points2_revamped.json");
                int size = is.available();
                byte[] buffer = new byte[size];
                is.read(buffer);
                is.close();
                json = new String(buffer, "UTF-8");
            } catch (IOException ex) {
                ex.printStackTrace();
                return null;
            }
            return json;
        }

        public AnalyseImageTask(Context context) {
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if(dialog == null){
                dialog = new ProgressDialog(context);
                dialog.setCancelable(false);
                dialog.setIndeterminate(true);
                dialog.setMessage("Extracting image...");
                dialog.show();
            }
        }

        @Override
        protected Void doInBackground(byte[]... bytes) {
            MainActivity activity = (MainActivity) context;
            JSONObject obj = null;
            JSONObject calibObject = null;
            JSONArray arr = null;
            float pixelCalibFactor = -1;

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onPause();
                }
            });


            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes[0], 0, bytes[0].length);
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int[] pixels = new int[width * height];
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
            RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));
            try {
                bitMatrix = binaryBitmap.getBlackMatrix();
            } catch (NotFoundException | NullPointerException e) {
                e.printStackTrace();
            }

            if(bitMatrix != null){
                finderPatternFinder = new FinderPatternFinder(bitMatrix);
                try {
                    FinderPatternInfo info = finderPatternFinder.find(null);
                } catch (Exception e) {
                    // this only means not all patterns (=4) are detected.
                    //Log.v(TAG,"not all patterns (=4) are detected in the current frame");
                }
                if(finderPatternFinder == null){
                    throw new RuntimeException("finderPatternFinder is null");
                }

                patterns = finderPatternFinder.getPossibleCenters();

                if(patterns.size() == 4){
                    Log.v("abcxyz","card size: 0-x = "+patterns.get(0).getX()+", 0-y = "+patterns.get(0).getY()+ ", module size = "+patterns.get(0).getEstimatedModuleSize());
                    Log.v("abcxyz","card size: 1-x = "+patterns.get(1).getX()+", 1-y = "+patterns.get(1).getY()+ ", module size = "+patterns.get(1).getEstimatedModuleSize());
                    Log.v("abcxyz","card size: 2-x = "+patterns.get(2).getX()+", 2-y = "+patterns.get(2).getY()+ ", module size = "+patterns.get(2).getEstimatedModuleSize());
                    Log.v("abcxyz","card size: 3-x = "+patterns.get(3).getX()+", 3-y = "+patterns.get(3).getY()+ ", module size = "+patterns.get(3).getEstimatedModuleSize());

                    averageModuleSize = ( patterns.get(0).getEstimatedModuleSize() +
                            patterns.get(1).getEstimatedModuleSize() +
                            patterns.get(2).getEstimatedModuleSize() +
                            patterns.get(3).getEstimatedModuleSize() )/4;

                    float width_card,height_card, originX, originY;

                    List<FinderPattern> patternsCopy = new ArrayList<>(patterns);

                    Collections.sort(patterns, new Comparator<FinderPattern>() {
                        @Override
                        public int compare(FinderPattern lhs, FinderPattern rhs) {
                            if(lhs.getX() == rhs.getX())
                                return 0;
                            return (lhs.getX() < rhs.getX()? -1 : 1);
                        }
                    });

                    if(patterns.get(0).getY() < patterns.get(1).getY()){
                        originX = patterns.get(0).getX();
                        originY = patterns.get(0).getY();

                        Log.v("abcxyz","Origin: case1");
                    }else{
                        originX = patterns.get(1).getX();
                        originY = patterns.get(1).getY();
                        Log.v("abcxyz","Origin: case2");
                    }

                    PointF originPoint = new PointF(originX,originY);




                    Collections.sort(patternsCopy, new Comparator<FinderPattern>() {
                        @Override
                        public int compare(FinderPattern lhs, FinderPattern rhs) {
                            if(lhs.getY() == rhs.getY())
                                return 0;
                            return (lhs.getY() < rhs.getY()? -1 : 1);
                        }
                    });

                    PointF adjacentPoint;
                    if(patternsCopy.get(0).getX() < patternsCopy.get(1).getX()){
                        width_card = patternsCopy.get(1).getX() - patternsCopy.get(0).getX();
                        height_card = patternsCopy.get(2).getY() - patternsCopy.get(0).getY();
                        adjacentPoint = new PointF(patternsCopy.get(1).getX(),patternsCopy.get(1).getY());
                        Log.v("abcxyz","width height: case1");
                    }else{
                        width_card = patternsCopy.get(0).getX() - patternsCopy.get(1).getX();
                        height_card = patternsCopy.get(3).getY() - patternsCopy.get(1).getY();
                        adjacentPoint = new PointF(patternsCopy.get(0).getX(),patternsCopy.get(0).getY());
                        Log.v("abcxyz","width height: case2");
                    }

                    float angle = angleBetweenPoints(originPoint,adjacentPoint);

                    Log.v("abcxyz","card | originX: "+originX+", originY: "+originY+", width: "+width_card+", height: "+height_card);
                    Log.v("abcxyz"," origin point = ("+originPoint.x+","+originPoint.y+") , adjacent point = ("+adjacentPoint.x+","+adjacentPoint.y+"), rotation angle = "+angle);
                    //bitmap = rotateBitmap(bitmap,angle);

                    originalFile=new File(Environment.getExternalStorageDirectory()+"/Pictures/WaterQuality");
                    if(!originalFile.isDirectory()){
                        originalFile.mkdir();
                    }

                    originalFile=new File(Environment.getExternalStorageDirectory()+"/Pictures/WaterQuality","latestOriginal.png");

                    if(originalFile.exists())
                        originalFile.delete();

                    try
                    {
                        FileOutputStream fileOutputStream=new FileOutputStream(originalFile);
                        bitmap.compress(Bitmap.CompressFormat.PNG,100, fileOutputStream);
                        fileOutputStream.flush();
                        fileOutputStream.close();
                    }
                    catch(IOException e){
                        e.printStackTrace();
                    }

                    if(originY+height_card > height){
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                onResume();
                                Toast.makeText(context, "Height Mismatch! Try again!", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        });
                    }


                    if(originX + width_card > width){
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                onResume();
                                Toast.makeText(context, "Width Mismatch Try again!", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        });
                    }

                    Bitmap croppedBitmap = Bitmap.createBitmap(bitmap,(int)originX,(int)originY,(int)width_card,(int)height_card);
                    croppedBitmap = rotateBitmap(croppedBitmap,90);
                    //croppedBitmap = croppedBitmap.copy(croppedBitmap.getConfig(),true);

                    rawFile=new File(Environment.getExternalStorageDirectory()+"/Pictures/WaterQuality");
                    if(!rawFile.isDirectory()){
                        rawFile.mkdir();
                    }

                    rawFile=new File(Environment.getExternalStorageDirectory()+"/Pictures/WaterQuality","latestRaw.png");

                    if(rawFile.exists())
                        rawFile.delete();

                    try
                    {
                        FileOutputStream fileOutputStream=new FileOutputStream(rawFile);
                        croppedBitmap.compress(Bitmap.CompressFormat.PNG,100, fileOutputStream);
                        fileOutputStream.flush();
                        fileOutputStream.close();
                    }
                    catch(IOException e){
                        e.printStackTrace();
                    }


                    json = loadPointsJSONFromAsset();

                    Canvas canvas = new Canvas(croppedBitmap);
                    if(json == null)
                    {
                        throw new RuntimeException("Json config is null");
                    }

                    try {
                        obj = new JSONObject(json);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    if(obj == null)
                    {
                        throw new RuntimeException("Json: obj is null");
                    }

                    try {
                        arr = obj.getJSONArray("points");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    if(arr == null)
                    {
                        throw new RuntimeException("Json: array is null");
                    }

                    try {
                        calibObject = arr.getJSONObject(0);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    if(calibObject == null)
                    {
                        throw new RuntimeException("Json: calibObject is null");
                    }

                    try {
                        int x = calibObject.getInt("x");
                        pixelCalibFactor = height_card/ (float)x;
                        Log.v("abcxyz","pixelCalibFactor = "+height_card+"/"+x+"="+pixelCalibFactor);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    if(pixelCalibFactor == -1)
                    {
                        throw new RuntimeException("Json: pixelCalib is null (-1) ");
                    }
                    for(int i = 1; i < arr.length(); i++){
                        JSONObject tmp = null;
                        float x = -1;
                        float y = -1;
                        try {
                            tmp = arr.getJSONObject(i);
                            Log.v("abcxyz","tmp json = "+ tmp);
                            int tmpX = tmp.getInt("x");
                            int tmpY = tmp.getInt("y");
                            Log.v("abcxyz","json | before calib x = "+tmpX+", y = "+tmpY);
                            x = tmpX * pixelCalibFactor;
                            y = tmpY * pixelCalibFactor;
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        if(tmp == null || x == -1 || y == -1)
                        {
                            throw new RuntimeException("Json: tmp || x || y is null (-1)");
                        }

                        Log.v("abcxyz","json | after calib x = "+x+", y = "+y);

                        Paint paint = new Paint();
                        paint.setAntiAlias(true);
                        paint.setColor(Color.BLUE);

                        int xCenter = (int) x;
                        int yCenter = (int) y;
                        int radius = (int) (1.5*averageModuleSize);
                        //int xSym, ySym;

                        Log.v("coordinates",""+i+", x: "+xCenter+", y: "+yCenter);

                        canvas.drawRect(xCenter-radius,yCenter-radius,xCenter+radius,yCenter+radius,paint);


                        // find points inside a circle of some radius
                        /*for (int xCircle = xCenter - radius ; xCircle <= (xCenter+radius) ; xCircle++)
                        {
                            for (int yCircle = yCenter - radius ; yCircle <= (yCenter+radius) ; yCircle++)
                            {
                                // we don't have to take the square root, it's slow

                                // if circle needed, uncomment the next line
                                //if ((xCircle - xCenter)*(xCircle - xCenter) + (yCircle - yCenter)*(yCircle - yCenter) <= radius*radius)
                                {
                                    //xSym = xCenter - (xCircle - xCenter);
                                    //ySym = yCenter - (xCircle - yCenter);
                                    // (xCircle, yCircle), (xCircle, ySym), (xSym , yCircle), (xSym, ySym) are in the circle
                                    canvas.drawPoint(xCircle,yCircle,paint);
                                    //canvas.drawPoint(xCircle,ySym,paint);
                                    //canvas.drawPoint(xSym,yCircle,paint);
                                    //canvas.drawPoint(xSym,ySym,paint);
                                }
                            }
                        }*/
                        //canvas.drawCircle(x,y,7,paint);
                    }

                    calibratedFile=new File(Environment.getExternalStorageDirectory()+"/Pictures/WaterQuality");
                    if(!calibratedFile.isDirectory()){
                        calibratedFile.mkdir();
                    }

                    calibratedFile=new File(Environment.getExternalStorageDirectory()+"/Pictures/WaterQuality","latestCalibrated.png");

                    if(calibratedFile.exists())
                        calibratedFile.delete();

                    try
                    {
                        FileOutputStream fileOutputStream=new FileOutputStream(calibratedFile);
                        croppedBitmap.compress(Bitmap.CompressFormat.PNG,100, fileOutputStream);
                        fileOutputStream.flush();
                        fileOutputStream.close();


                    }
                    catch(IOException e){
                        e.printStackTrace();
                    }
                }
                else{
                    Log.e("error","Could not detect card, try again");
                    //Toast.makeText(context, "Could not detect card, try again", Toast.LENGTH_SHORT).show();
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onResume();
                            Toast.makeText(context, "Try again!", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if(calibratedFile != null && rawFile != null && originalFile != null){
                MediaScannerConnection.scanFile(context, new String[] {calibratedFile.getAbsolutePath() }, null, null);
                MediaScannerConnection.scanFile(context, new String[] {rawFile.getAbsolutePath() }, null, null);
                MediaScannerConnection.scanFile(context, new String[] {originalFile.getAbsolutePath() }, null, null);
                Intent i = new Intent(context, ProcessImageActivity.class);
                Bundle bundle = new Bundle();
                if(calibratedFile != null && rawFile != null && averageModuleSize != -1)
                {
                    bundle.putString("rawPhotoPath", rawFile.getAbsolutePath());
                    bundle.putString("calibratedPhotoPath", calibratedFile.getAbsolutePath());
                    bundle.putFloat("averageModuleSize",averageModuleSize);
                }
                else
                {
                    Log.v("abcxyz","either of the file is null in onPostExecute");
                }
                i.putExtras(bundle);
                if(dialog !=null)
                {
                    if(dialog.isShowing())
                    {
                        dialog.dismiss();
                        dialog = null;
                    }
                }
                startActivity(i);
            }
            else{
                if(dialog !=null)
                {
                    if(dialog.isShowing())
                    {
                        dialog.dismiss();
                        dialog = null;
                    }
                }
            }
        }
    }




    @Override
    public void onPictureTaken(byte[] bytes, Camera camera) {
        Log.v("abcxyz","UI Thread: "+(Looper.myLooper() == Looper.getMainLooper()));
        new AnalyseImageTask(MainActivity.this).execute(bytes);
    }

    public static Bitmap rotateBitmap(Bitmap source, float angle)
    {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    private float angleBetweenPoints(PointF a, PointF b) {
        float deltaY = b.y - a.y;
        float deltaX = b.x - a.x;
        return (float) Math.toDegrees(Math.atan2(deltaY, deltaX));
    }

    public static BinaryBitmap rotateBinaryBitmap(BinaryBitmap source)
    {
        return source.rotateCounterClockwise();
    }

    public void setPreviewProperties()
    {
        if(mCamera!=null && baseCameraView!=null) {
            previewFormat = mCamera.getParameters().getPreviewFormat();
            previewWidth = mCamera.getParameters().getPreviewSize().width;
            previewHeight = mCamera.getParameters().getPreviewSize().height;
        }
    }

    @Override
    public void onPause() {

        cameraScheduledExecutorService.shutdown();

        if (mCamera != null) {

            mCamera.setOneShotPreviewCallback(null);

            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }

        if(mActivity!=null)
        {
            mActivity.clear();
            mActivity = null;
        }
        if(wrCamera!=null)
        {
            wrCamera.clear();
            wrCamera = null;
        }

        if (baseCameraView != null && previewLayout!=null) {
            previewLayout.removeView(baseCameraView);
            baseCameraView = null;
        }

        Log.d(TAG, "onPause OUT mCamera, mCameraPreview: " + mCamera + ", " + baseCameraView);

        super.onPause();

    }

    @Override
    public void onResume() {
        //OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);

        init();
        super.onResume();


        if(mCamera!=null){
            //mCamera.setOneShotPreviewCallback(this);
            //Log.d(TAG, "preview callback set");
        }

        Log.d(TAG, "onResume OUT mCamera, mCameraPreview: " + mCamera + ", " + baseCameraView);

    }

    private static class MyHandler extends Handler {

//        public MyHandler(CameraActivity activity) {
//
//        }
//
//        @Override
//        public void handleMessage(Message msg) {
//            CameraActivity activity = mActivity.get();
//            if (activity != null) {
//                System.out.println("***got your message.");
//            }
//        }
    }

    private class ShowFinderPatternRunnable implements Runnable
    {
        private int color;
        private List<FinderPattern> patterns;
        private Camera.Size size;
        private WeakReference<FinderPatternIndicatorView> wrFinderPatternIndicatorView =
                new WeakReference<FinderPatternIndicatorView>(finderPatternIndicatorView);

        @Override
        public void run() {
            if(wrFinderPatternIndicatorView != null) {

                wrFinderPatternIndicatorView.get().setColor(color);
                wrFinderPatternIndicatorView.get().showPatterns(patterns, size==null? 0: size.width, size==null? 0: size.height);
            }
        }

        public void setColor(int color) {
            this.color = color;
        }

        public void setPatterns(List<FinderPattern> patterns) {
            this.patterns = patterns;
        }

        public void setSize(Camera.Size size) {
            this.size = size;
        }


    }

    //OPENCV MANAGER
    /*private final BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i("", "OpenCV loaded successfully");

                    init();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }

                break;
            }
        }
    };*/
}
