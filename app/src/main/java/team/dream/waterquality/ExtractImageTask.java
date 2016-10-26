package team.dream.waterquality;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.detector.FinderPattern;
import com.google.zxing.qrcode.detector.FinderPatternFinder;
import com.google.zxing.qrcode.detector.FinderPatternInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Abhilash on 05/08/2016.
 */
public class ExtractImageTask extends AsyncTask<String,Void,Bitmap> {
    Context context;
    ProgressDialog dialog;
    File rawImage;
    Bitmap rawBitmap = null;
    BitMatrix bitMatrix;
    FinderPatternFinder finderPatternFinder;
    List<FinderPattern> patterns;
    float averageModuleSize;
    public ExtractImageTask(Context context) {
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
    protected Bitmap doInBackground(String... strings) {
        String rawPhotoPath = strings[0];
            /*
            * Extract Card here
            */

        if(rawPhotoPath != null){
            rawImage = new File(rawPhotoPath);
                /*try {
                    rawBytes = read(rawImage);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if(rawBytes == null){
                    throw new RuntimeException("Could not read image");
                }*/

            if (rawImage.exists()) {
                //BitmapFactory.Options options = new BitmapFactory.Options();
                //options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                //rawBitmap = BitmapFactory.decodeFile(rawImage.getAbsolutePath(),options);
                rawBitmap = BitmapFactory.decodeFile(rawImage.getAbsolutePath());
            } else {
                Toast.makeText(context, "Raw Img not found", Toast.LENGTH_SHORT).show();
                throw new RuntimeException("Raw Img not found");
            }

            int width = rawBitmap.getWidth();
            int height = rawBitmap.getHeight();
            int[] pixels = new int[width * height];
            rawBitmap.getPixels(pixels, 0, width, 0, 0, width, height);
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
                for(int i = 0; i< patterns.size(); i++) {
                    float estimatedModuleSize = patterns.get(i).getEstimatedModuleSize();
                    //Log.v(TAG,"Estimated module size = "+estimatedModuleSize);
                    if (estimatedModuleSize < 2) {
                        Log.e("error","estimatedModuleSize < 2");
                        //Toast.makeText(context, "estimatedModuleSize < 2", Toast.LENGTH_SHORT).show();
                    }
                }

                if(patterns.size() != 4){
                    Log.e("error","Could not detect card, try again");
                    //Toast.makeText(context, "Could not detect card, try again", Toast.LENGTH_SHORT).show();

                }

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

                Collections.sort(patternsCopy, new Comparator<FinderPattern>() {
                    @Override
                    public int compare(FinderPattern lhs, FinderPattern rhs) {
                        if(lhs.getY() == rhs.getY())
                            return 0;
                        return (lhs.getY() < rhs.getY()? -1 : 1);
                    }
                });

                if(patternsCopy.get(0).getX() < patternsCopy.get(1).getX()){
                    width_card = patternsCopy.get(1).getX() - patternsCopy.get(0).getX();
                    height_card = patternsCopy.get(2).getY() - patternsCopy.get(0).getY();
                    Log.v("abcxyz","width height: case1");
                }else{
                    width_card = patternsCopy.get(0).getX() - patternsCopy.get(1).getX();
                    height_card = patternsCopy.get(3).getY() - patternsCopy.get(1).getY();
                    Log.v("abcxyz","width height: case2");
                }

                Bitmap croppedBitmap = Bitmap.createBitmap(rawBitmap,(int)originX,(int)originY,(int)width_card,(int)height_card);
                croppedBitmap = rotateBitmap(croppedBitmap,90);
                croppedBitmap = croppedBitmap.copy(croppedBitmap.getConfig(),true);

                return croppedBitmap;
            }

        }
        else {
            throw new RuntimeException("File paths empty");
        }

            /*
            * Card extraction ends
            */
        return null;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        super.onPostExecute(bitmap);
        if(dialog !=null)
        {
            if(dialog.isShowing())
            {
                dialog.dismiss();
                dialog = null;
            }
        }
    }

    public static Bitmap rotateBitmap(Bitmap source, float angle)
    {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }
}
