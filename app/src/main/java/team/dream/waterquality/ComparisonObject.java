package team.dream.waterquality;

/**
 * Created by Abhilash on 02/08/2016.
 */
public class ComparisonObject {

    int column;
    int stripPatchNumber;
    int against;
    Float difference;
    Double distance;

    public ComparisonObject(int column, int stripPatchNumber, int against, Float difference, Double distance) {
        this.column = column;
        this.against = against;
        this.difference = difference;
        this.distance = distance;
        this.stripPatchNumber = stripPatchNumber;
    }

    public int getColumn() {
        return column;
    }

    public void setColumn(int column) {
        this.column = column;
    }

    public int getAgainst() {
        return against;
    }

    public void setAgainst(int against) {
        this.against = against;
    }

    public Float getDifference() {
        return difference;
    }

    public void setDifference(Float difference) {
        this.difference = difference;
    }

    public int getStripPatchNumber() {
        return stripPatchNumber;
    }

    public void setStripPatchNumber(int stripPatchNumber) {
        this.stripPatchNumber = stripPatchNumber;
    }

    public Double getDistance() {
        return distance;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }
}
