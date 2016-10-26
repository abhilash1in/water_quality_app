package team.dream.waterquality;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


public class ProcessImageActivity extends AppCompatActivity {

    ImageView capturedImage;
    Bitmap rawBitmap = null;
    File rawImage = null;
    float averageModuleSize = -1;
    List<CustomColor> detectedColors;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_process_image);
        capturedImage = (ImageView) findViewById(R.id.capturedImage);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String calibratedPhotoPath = extras.getString("calibratedPhotoPath");
            String rawPhotoPath = extras.getString("rawPhotoPath");
            averageModuleSize = extras.getFloat("averageModuleSize");
            if (calibratedPhotoPath != null && rawPhotoPath != null && averageModuleSize != -1) {
                if (!calibratedPhotoPath.equals("") && !rawPhotoPath.equals("")) {
                    File calibratedImage = new File(calibratedPhotoPath);
                    rawImage = new File(rawPhotoPath);
                    if (calibratedImage.exists()) {
                        //Bitmap myBitmap = BitmapFactory.decodeFile(image.getAbsolutePath());
                        //int nh = (int) ( myBitmap.getHeight() * (512.0 / myBitmap.getWidth()) );
                        //Bitmap scaled = Bitmap.createScaledBitmap(myBitmap,512,nh,true);
                        //capturedImage.setImageBitmap(myBitmap);

                        //final Point displySize = getDisplaySize(getWindowManager().getDefaultDisplay());
                        //final int size = (int) Math.ceil(Math.sqrt(displySize.x * displySize.y));
                        Picasso.with(this)
                                .load(calibratedImage)
                                //.resize(size, size)
                                //.centerInside()
                                .memoryPolicy(MemoryPolicy.NO_CACHE)
                                .into(capturedImage);
                    } else {
                        Toast.makeText(ProcessImageActivity.this, "Calibrated Img not found", Toast.LENGTH_SHORT).show();
                    }
                    if (rawImage.exists()) {
                        //BitmapFactory.Options options = new BitmapFactory.Options();
                        //options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                        //rawBitmap = BitmapFactory.decodeFile(rawImage.getAbsolutePath(),options);

                        rawBitmap = BitmapFactory.decodeFile(rawImage.getAbsolutePath());
                    } else {
                        Toast.makeText(ProcessImageActivity.this, "Raw Img not found", Toast.LENGTH_SHORT).show();
                    }
                    //File filePath = getFileStreamPath(photoPath);
                    //Toast.makeText(ProcessImageActivity.this, filePath.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                    //Bitmap bitmap  = BitmapFactory.decodeByteArray(photo, 0, photo.length);
                    //capturedImage.setImageBitmap(bitmap);
                } else {
                    throw new RuntimeException("File paths empty");
                }
            } else {
                throw new RuntimeException("File paths or averageModuleSize null");
            }

        } else {
            throw new RuntimeException("No extras received");
        }
    }

    public void okayButton(View v) {

        if (rawBitmap != null) {
            //Picasso.with(this).load(rawImage).memoryPolicy(MemoryPolicy.NO_CACHE).into(capturedImage);
            getColors(rawBitmap);
        } else
            throw new RuntimeException("rawBitmap null");
    }


    private void getColors(Bitmap rawBitmap) {

        ProgressDialog dialog = null;

        if (dialog == null) {
            dialog = new ProgressDialog(this);
            dialog.setCancelable(false);
            dialog.setIndeterminate(true);
            dialog.setMessage("Analysing colors...");
            dialog.show();
        }

        float width, height;
        String json;
        JSONObject obj = null;
        JSONObject calibObject = null;
        JSONArray arr = null;
        float pixelCalibFactor = -1;

        width = rawBitmap.getWidth();
        height = rawBitmap.getHeight();


        json = loadPointsJSONFromAsset();
        if (json == null) {
            throw new RuntimeException("Json config is null");
        }

        try {
            obj = new JSONObject(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (obj == null) {
            throw new RuntimeException("Json: obj is null");
        }

        try {
            arr = obj.getJSONArray("points");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (arr == null) {
            throw new RuntimeException("Json: array is null");
        }

        try {
            calibObject = arr.getJSONObject(0);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (calibObject == null) {
            throw new RuntimeException("Json: calibObject is null");
        }

        //height before rotation = width after rotation
        try {
            int x = calibObject.getInt("x");
            pixelCalibFactor = width / (float) x;
            Log.v("abcxyz", "pixelCalibFactor = " + width + "/" + x + "=" + pixelCalibFactor + ", width = " + height);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (pixelCalibFactor == -1) {
            throw new RuntimeException("Json: pixelCalib is null (-1) ");
        }

        detectedColors = new ArrayList<>(50);
        for (int i = 0; i < 50; i++)
            detectedColors.add(null);

        for (int i = 1; i < arr.length(); i++) {
            final int j = i;
            JSONObject tmp = null;
            float x = -1;
            float y = -1;
            try {
                tmp = arr.getJSONObject(i);
                Log.v("abcxyz", "tmp2 json = " + tmp);
                int tmpX = tmp.getInt("x");
                int tmpY = tmp.getInt("y");
                Log.v("abcxyz", "json | before calib x = " + tmpX + ", y = " + tmpY);
                x = tmpX * pixelCalibFactor;
                y = tmpY * pixelCalibFactor;
                Log.v("abcxyz","json | after calib x = "+x+", y = "+y);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (tmp == null || x == -1 || y == -1) {
                throw new RuntimeException("Json: tmp || x || y is null (-1)");
            }

            int xCenter = (int) x;
            int yCenter = (int) y;
            int radius = (int) (averageModuleSize);
            Log.v("abczyz", "avg module size = " + averageModuleSize);

            Log.v("coordinates", "" + j + ", x: " + xCenter + ", y: " + yCenter);

            /*
            //attempt 3
            Palette.from(Bitmap.createBitmap(rawBitmap,xCenter-radius,yCenter-radius,2*radius,2*radius)).generate(new Palette.PaletteAsyncListener() {
                public void onGenerated(Palette p) {
                    // Use generated instance
                    Palette.Swatch swatch = p.getVibrantSwatch();
                    if(swatch!=null){ // mind it, it can be null sometime.
                        int color = swatch.getRgb();
                        int[] colorArr = getRGBArr(color);
                        Log.v("common color"," Tile "+j+": R = "+colorArr[0]+", G = "+colorArr[1]+" B, = "+colorArr[2]);
                    }
                    else{
                        Log.v("common color"," Tile "+j+": null ");
                    }
                }
            });
            */

            /*
            //attempt 4
            Palette p = Palette.from(Bitmap.createBitmap(rawBitmap,xCenter-radius,yCenter-radius,2*radius,2*radius)).generate();
            Palette.Swatch swatch = p.getMutedSwatch();
            if(swatch!=null){ // mind it, it can be null sometime.
                int color = swatch.getRgb();
                int[] colorArr = getRGBArr(color);
                Log.v("common color"," Tile "+j+": R = "+colorArr[0]+", G = "+colorArr[1]+" B, = "+colorArr[2]);
            }
            else{
                Log.v("common color"," Tile "+j+": null ");
            }
            */

            /*
            //attempt 1
            int color = getDominantColor(Bitmap.createBitmap(rawBitmap,xCenter-radius,yCenter-radius,2*radius,2*radius));
            int[] colorArr = getRGBArr(color);
            Log.v("common color"," Tile "+i+": R = "+colorArr[0]+", G = "+colorArr[1]+" B, = "+colorArr[2]);
            */

            //attempt 2
            try {
                ImageTester.getMostCommonColour(
                        Bitmap.createBitmap(rawBitmap, xCenter - radius, yCenter - radius, radius, radius),
                        i, new ImageTester.ImageColor() {
                            @Override
                            public void onImageColor(int i, int r, int g, int b) {
                                CustomColor color = new CustomColor(i, r, g, b);
                                detectedColors.add(j, color);
                                Log.v("common color", " Tile " + color.getPatchNumber() + ": R = " + color.getRed() + ", G = " + color.getGreen() + " B, = " + color.getBlue());
                            }
                        }).join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        String positionJson = loadPositionsJSONFromAsset();
        if (positionJson == null) {
            throw new RuntimeException("positionJson is null");
        }

        JSONObject positionObj = null;
        JSONObject positions = null;
        try {
            positionObj = new JSONObject(positionJson);
            positions = positionObj.getJSONObject("positions");
        } catch (JSONException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }


        int count = 1;

        List<ComparisonObject> differences = new ArrayList<>(6);
        differences.add(0, null);

        List<ComparisonObject> minimumByDistance = new ArrayList<>(6);
        minimumByDistance.add(0, null);
        List<ComparisonObject> minimumByDifference = new ArrayList<>(6);
        minimumByDifference.add(0, null);


        for (int i = 37; i <= 42; i++) {
            JSONArray jsonArray = null;
            try {
                jsonArray = positions.getJSONArray("" + i);
            } catch (JSONException e) {
                e.printStackTrace();
                throw new RuntimeException(e.getMessage());
            }

            if (jsonArray == null) {
                throw new RuntimeException("jsonArray == null");
            }


            for (int j = 0; j < jsonArray.length(); j++) {
                int n = -1;
                try {
                    n = jsonArray.getInt(j);
                } catch (JSONException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e.getMessage());
                }

                if (n == -1) {
                    throw new RuntimeException("n value not found");
                }

                CustomColor lhs = detectedColors.get(i);
                CustomColor rhs = detectedColors.get(n);
                float difference = ImageTester.compareColors(lhs, rhs);
                double euclideanDistance = ImageTester.euclideanDistance(lhs, rhs);
                differences.add(new ComparisonObject(j + 1, i, n, difference, euclideanDistance));

                Log.v("compare", "Difference between patch " + lhs.getPatchNumber() + " and " + rhs.getPatchNumber() + " = " + difference + " for column = " + (j + 1));
                Log.v("distance", "Euclidean Distance between patch " + lhs.getPatchNumber() + " and " + rhs.getPatchNumber() + " = " + euclideanDistance + " for column = " + (j + 1));
            }

            count = differences.size() - 1;

            ComparisonObject minComparisonObjectByDistance = differences.get(count - jsonArray.length() + 1);
            ComparisonObject minComparisonObjectByDifference = differences.get(count - jsonArray.length() + 1);

            Log.v("zzzz", "j = " + (count - jsonArray.length() + 2));
            Log.v("zzzz", "count = " + count);

            for (int j = count - jsonArray.length() + 2; j <= count; j++) {
                if (differences.get(j).getDistance() < minComparisonObjectByDistance.getDistance())
                    minComparisonObjectByDistance = differences.get(j);
            }

            for (int j = count - jsonArray.length() + 2; j <= count; j++) {
                if (differences.get(j).getDifference() < minComparisonObjectByDifference.getDifference())
                    minComparisonObjectByDifference = differences.get(j);
            }

            minimumByDifference.add(minComparisonObjectByDifference);
            minimumByDistance.add(minComparisonObjectByDistance);
        }

        String valuesJsonString = loadValuesJSONFromAsset();

        if (valuesJsonString == null) {
            throw new RuntimeException("values json is null");
        }

        JSONObject valuesJsonObject = null;

        try {
            valuesJsonObject = new JSONObject(valuesJsonString);
        } catch (JSONException e) {
            e.printStackTrace();
            throw new RuntimeException("invalid values json");
        }

        JSONObject values = null;

        try {
            values = valuesJsonObject.getJSONObject("values");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (values == null) {
            throw new RuntimeException("values is null");
        }

        JSONArray results = new JSONArray();
        for (int i = 1; i <= 6; i++) {
            JSONArray value;
            try {
                value = values.getJSONArray("" + i);
            } catch (JSONException e) {
                e.printStackTrace();
                throw new RuntimeException("wrong value format");
            }

            Log.v("aaaa","value = "+value);

            double result = -1;
            ComparisonObject tempDifference = minimumByDifference.get(i);
            ComparisonObject tempDistance = minimumByDistance.get(i);
            try {
                //result = value.getDouble(tempDifference.getColumn());
                result = ((Number)value.get(tempDifference.getColumn() - 1)).doubleValue();
            } catch (JSONException e) {
                e.printStackTrace();
                throw new RuntimeException("result is null");
            }

            JSONObject resultObject = new JSONObject();
            try {
                resultObject.put("name", getChemicalName(i));
                resultObject.put("value", result);
            } catch (JSONException e) {
                e.printStackTrace();
                throw new RuntimeException("invalid Json");
            }
            results.put(resultObject);

            Log.v("results", "" + tempDifference.getStripPatchNumber() + " against " + tempDifference.getAgainst() + ", column = " + tempDifference.getColumn() + ", minimum difference= " + tempDifference.getDifference());
            Log.v("results", "" + tempDistance.getStripPatchNumber() + " against " + tempDistance.getAgainst() + ", column = " + tempDistance.getColumn() + ", minimum distance= " + tempDistance.getDistance());
        }


        try {
            JSONObject finalObject = new JSONObject().put("results",results);
            writeResultsJsonToAsset(this,finalObject.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (dialog != null) {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        }

        startActivity(new Intent(ProcessImageActivity.this,ResultActivity.class));
    }

    public String getChemicalName(int n) {
        String value;
        switch (n) {
            case 1:
                value = "Nitrate ppm (mg/L)";
                break;
            case 2:
                value =  "Nitrite ppm (mg/L)";
                break;
            case 3:
                value =  "Total Hardness ppm (GH)";
                break;
            case 4:
                value =  "Total Chlorine ppm (mg/L)";
                break;
            case 5:
                value =  "Total Alkalinity ppm (KH)";
                break;
            case 6:
                value =  "pH";
                break;
            default:
                value =  "Unknown chemical";
        }
        return value;
    }

    public static int getDominantColor(Bitmap bitmap) {
        Bitmap newBitmap = Bitmap.createScaledBitmap(bitmap, 1, 1, true);
        final int color = newBitmap.getPixel(0, 0);
        newBitmap.recycle();
        return color;
    }

    private static int[] getRGBArr2(int pixel) {
        int red = Color.red(pixel);
        int green = Color.green(pixel);
        int blue = Color.blue(pixel);
        int alpha = Color.alpha(pixel);
        return new int[]{red, green, blue, alpha};
    }

    private static int[] getRGBArr(int pixel) {
        int red = (pixel >> 16) & 0xff;
        int green = (pixel >> 8) & 0xff;
        int blue = (pixel) & 0xff;
        return new int[]{red, green, blue};

    }

    public void cancelButton(View v) {
        startActivity(new Intent(ProcessImageActivity.this, MainActivity.class));
    }

    private void writeResultsJsonToAsset(Context context, String mJsonResponse) {
        String fileName = "water_quality_result";
        try {
            FileWriter file = new FileWriter(context.getFilesDir().getPath() + "/" + fileName);
            Log.v("file", "Path " + context.getFilesDir().getPath() + "/" + fileName);
            file.write(mJsonResponse);
            file.flush();
            file.close();
        } catch (IOException e) {
            Log.e("TAG", "Error in Writing: " + e.getLocalizedMessage());
        }
    }

    private String loadResultsJsonStringFromFile(Context context) {
        String fileName = "water_quality_result";
        try {
            File f = new File(context.getFilesDir().getPath() + "/" + fileName);
            //check whether file exists
            FileInputStream is = new FileInputStream(f);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            return new String(buffer);
        } catch (IOException e) {
            Log.e("TAG", "Error in Reading: " + e.getLocalizedMessage());
            return null;
        }
    }

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

    public String loadPositionsJSONFromAsset() {
        String json = null;
        try {
            InputStream is = getAssets().open("positions.json");
            //InputStream is = getAssets().open("positions_revamped.json");
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

    public String loadValuesJSONFromAsset() {
        String json = null;
        try {
            InputStream is = getAssets().open("values.json");
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

    public Point getDisplaySize(Display display) {
        Point size = new Point();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            display.getSize(size);
        } else {
            int width = display.getWidth();
            int height = display.getHeight();
            size = new Point(width, height);
        }

        return size;
    }


}

