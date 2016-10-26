package team.dream.waterquality;

/**
 * Created by Abhilash on 02/08/2016.
 */
public class CustomColor {
    private int i,R,G,B;

    public CustomColor(int i, int r, int g, int b) {
        this.i = i;
        R = r;
        G = g;
        B = b;
    }

    public CustomColor(int r, int g, int b) {
        R = r;
        G = g;
        B = b;
    }

    public int getPatchNumber() {
        return i;
    }

    public int getRed() {
        return R;
    }

    public int getGreen() {
        return G;
    }

    public int getBlue() {
        return B;
    }
}