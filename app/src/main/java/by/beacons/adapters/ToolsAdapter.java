package by.beacons.adapters;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.parse.ParseQuery;
import com.parse.ParseQueryAdapter;

import by.beacons.R;
import by.beacons.app.App;
import by.beacons.models.Tool;

public class ToolsAdapter extends ParseQueryAdapter<Tool>{

    private LayoutInflater mInflater;

    public ToolsAdapter(Context context) {
        super(context, new QueryFactory<Tool>() {
            @Override
            public ParseQuery<Tool> create() {
                ParseQuery<Tool> query = new ParseQuery<Tool>(Tool.class);
                query.include(Tool.BEACON);
                if(!App.getInst().isNetworkAvailable()) {
                    query.fromLocalDatastore();
                }
                return query;
            }
        });
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }



    @Override
    public View getItemView(Tool object, View v, ViewGroup parent) {
        if(v == null) {
            v = mInflater.inflate(R.layout.list_item_tool, parent, false);
            v.setTag(createHolder(v));
        }
        bindInfo((ViewHolder) v.getTag(), object);
        return v;
    }

    private ViewHolder createHolder(View v) {
        ViewHolder holder = new ViewHolder();
        holder.nameTool = (TextView) v.findViewById(R.id.name_tool);
        holder.distanse = (TextView) v.findViewById(R.id.distanse);
        return holder;
    }

    private void bindInfo(ViewHolder holder, Tool item) {
        holder.nameTool.setText(item.getName());
        String distance = item.getBeacon().getDistance() == null ? "" : String.valueOf(item.getBeacon().getDistance());
        holder.distanse.setText(distance);
    }

    private class ViewHolder {
        private TextView nameTool;
        private TextView distanse;

    }

}
