package team.dream.waterquality;

/**
 * Created by Abhilash on 03/08/2016.
 */
public class ResultObject {

    private String name, value;
    private boolean valid;

    public ResultObject(String name, String value, boolean valid) {
        this.name = name;
        this.valid = valid;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
