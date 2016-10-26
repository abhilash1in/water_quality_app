package team.dream.waterquality;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ResultActivity extends AppCompatActivity {

    RecyclerView resultsView;
    List<ResultObject> results;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        resultsView = (RecyclerView) findViewById(R.id.results);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        resultsView.setLayoutManager(mLayoutManager);

        results = new ArrayList<>();

        String resultsString = loadResultsJsonStringFromFile(this);
        try {
            JSONObject resultsObject = new JSONObject(resultsString);
            JSONArray resultsArray = resultsObject.getJSONArray("results");
            for(int n = 0; n < resultsArray.length(); n++){
                JSONObject r = resultsArray.getJSONObject(n);
                ResultObject result = new ResultObject(r.getString("name"),r.getString("value"),true);
                results.add(result);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        ResultAdapter resultAdapter= new ResultAdapter(this,results);
        resultsView.setAdapter(resultAdapter);
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
}
