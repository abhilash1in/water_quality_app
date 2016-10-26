package team.dream.waterquality;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Abhilash on 03/08/2016.
 */
public class ResultAdapter extends RecyclerView.Adapter<ResultAdapter.ViewHolder> {

    List<ResultObject> results;
    Context context;

    public class ViewHolder extends RecyclerView.ViewHolder{
        TextView name,value;
        ImageView img;

        public ViewHolder(View itemView) {
            super(itemView);
            name = (TextView) itemView.findViewById(R.id.name);
            value = (TextView) itemView.findViewById(R.id.value);
            img = (ImageView) itemView.findViewById(R.id.img);
        }
    }

    public ResultAdapter(Context context, List<ResultObject> results) {
        this.context = context;
        this.results = results;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.results_item, parent, false);

        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ResultObject result = results.get(position);

        holder.name.setText(result.getName());
        holder.value.setText("~ "+result.getValue());
        if(result.isValid()){
            holder.img.setImageResource(R.drawable.ok);
        }
        else{
            holder.img.setImageResource(R.drawable.caution);
        }
    }

    @Override
    public int getItemCount() {
        return results.size();
    }
}
