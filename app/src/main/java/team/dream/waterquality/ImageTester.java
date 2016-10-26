package team.dream.waterquality;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

/**
 * Created by Abhilash on 31/07/2016.
 */

public class ImageTester {

    public interface ImageColor {
        void onImageColor(int i, int r, int g, int b);
    }

    public static Thread getAverageColor(final Bitmap image,
                                         final int patchNum,
                                         final ImageColor heColor){
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                int[] pixels = new int[image.getWidth()*image.getHeight()];
                image.getPixels(pixels,0,0,0,0,image.getWidth(),image.getHeight());
            }
        });
        t.start();
        return t;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Thread getMostCommonColour(final Bitmap image,
                                           final int patchNum,
                                           final ImageColor heColor) {
        Thread t = new Thread(new Runnable() {
            private int rgb;

            @Override
            public void run() {
                int height = image.getHeight();
                int width = image.getWidth();
                Log.v("params","height = "+height+", width = "+width);
                Map m = new HashMap();
                //int boderWid = width / 4;
                //int borderHeight = height / 4;

                for (int i = 0; i < width ; i++) {
                    for (int j = 0; j < height ;j++) {
                        try {
                            rgb = image.getPixel(i, j);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }/*finally{
                            i ++;
                            j ++;
                        }*/
                        //int[] rgbArr = getRGBArr(rgb);

                        // Filter out grays....
                        //if (!isGray(rgbArr))
                        {
                            Integer counter = (Integer) m.get(rgb);
                            if (counter == null)
                                counter = 0;
                            counter++;
                            m.put(rgb, counter);
                        }
                    }
                }
                List list = new LinkedList(m.entrySet());
                Collections.sort(list, new Comparator() {
                    public int compare(Object o1, Object o2) {
                        return ((Comparable) ((Map.Entry) (o1)).getValue())
                                .compareTo(((Map.Entry) (o2)).getValue());
                    }
                });
                Log.v("test","list size = "+list.size());
                if(list.size() != 0){
                    for (Object o: list) {
                        Map.Entry me = (Map.Entry) o;
                        int[] rgb = getRGBArr((Integer) me.getKey());
                        Log.v("detail"," Tile "+patchNum+": R = "+rgb[0]+", G = "+rgb[1]+" B, = "+rgb[2]+", occurances = "+me.getValue());
                    }

                    Map.Entry me = (Map.Entry) list.get(list.size() - 1);
                    int[] rgb = getRGBArr((Integer) me.getKey());
                    heColor.onImageColor(patchNum,rgb[0], rgb[1], rgb[2]);
                }
            }
        });

        t.start();
        return t;
    }



    public static float compareColors(CustomColor c1, CustomColor c2){
        //If you have two Color objects c1 and c2, you can just compare each RGB value from c1 with that of c2.

        int diffRed   = Math.abs(c1.getRed()   - c2.getRed());
        int diffGreen = Math.abs(c1.getGreen() - c2.getGreen());
        int diffBlue  = Math.abs(c1.getBlue()  - c2.getBlue());
        //Those values you can just divide by the amount of difference saturations (255), and you will get the difference between the two.

        float pctDiffRed   = (float)diffRed   / 255;
        float pctDiffGreen = (float)diffGreen / 255;
        float pctDiffBlue   = (float)diffBlue  / 255;
        //After which you can just find the average color difference in percentage.

        return (pctDiffRed + pctDiffGreen + pctDiffBlue) / 3 * 100;
        //Which would give you a difference in percentage between c1 and c2.
    }

    public static double euclideanDistance(CustomColor c1,CustomColor c2){
        return Math.sqrt(
                        Math.pow(c1.getRed() - c2.getRed() , 2) +
                        Math.pow(c1.getGreen() - c2.getGreen() , 2) +
                        Math.pow(c1.getBlue() - c2.getBlue() , 2)
        );
    }

    private static int[] getRGBArr(int pixel) {
        Log.v("rgb",""+pixel);
        int red = (pixel >> 16) & 0xff;
        int green = (pixel >> 8) & 0xff;
        int blue = (pixel) & 0xff;
        return new int[] { red, green, blue };
    }

    private static boolean isGray(int[] rgbArr) {
        int rgDiff = rgbArr[0] - rgbArr[1];
        int rbDiff = rgbArr[0] - rgbArr[2];
        int tolerance = 10;
        if (rgDiff > tolerance || rgDiff < -tolerance)
            if (rbDiff > tolerance || rbDiff < -tolerance) {
                return false;
            }
        return true;
    }
}