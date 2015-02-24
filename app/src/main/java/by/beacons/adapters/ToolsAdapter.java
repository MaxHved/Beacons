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
import by.beacons.models.Beacon;
import by.beacons.models.Tool;

public class ToolsAdapter extends ParseQueryAdapter<Tool>{

    private LayoutInflater mInflater;

    private boolean mIsShowExtandInfo;

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


    public boolean isShowExtandInfo() {
        return mIsShowExtandInfo;
    }

    public void setShowExtandInfo(boolean isShowExtandInfo) {
        mIsShowExtandInfo = isShowExtandInfo;
        notifyDataSetChanged();
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
        holder.distance = (TextView) v.findViewById(R.id.distanse);
        holder.isEnterRegion = (TextView) v.findViewById(R.id.is_enter);
        return holder;
    }

    private void bindInfo(ViewHolder holder, Tool item) {
        holder.nameTool.setText(item.getName());
        if(mIsShowExtandInfo) {
            Beacon beacon = item.getBeacon();
            String distance = beacon.getDistance() != null && beacon.isEnterRegion() ? String.format("%.3f Meters", item.getBeacon().getDistance()) : "";
            holder.distance.setText(distance);
            holder.isEnterRegion.setText(item.getBeacon().isEnterRegion() ? "Enter" : "Leave"); //TODO
        } else {
            holder.distance.setText("");
            holder.isEnterRegion.setText("");
        }
    }

    private class ViewHolder {
        private TextView nameTool;
        private TextView distance;
        private TextView isEnterRegion;

    }

}
