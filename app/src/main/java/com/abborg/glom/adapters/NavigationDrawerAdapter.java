package com.abborg.glom.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.abborg.glom.R;
import com.abborg.glom.model.CircleInfo;

import java.util.ArrayList;
import java.util.List;

public class NavigationDrawerAdapter extends RecyclerView.Adapter<NavigationDrawerAdapter.MyViewHolder> {

    private List<CircleInfo> circles;

    private LayoutInflater inflater;

    private Context context;

    public NavigationDrawerAdapter(Context context, List<CircleInfo> data) {
        this.context = context;
        inflater = LayoutInflater.from(context);
        circles = data == null ? new ArrayList<CircleInfo>() : data;
    }

    public void update(List<CircleInfo> list) {
        circles = list;
        notifyDataSetChanged();
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.nav_drawer_row, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        CircleInfo current = circles.get(position);
        holder.title.setText(current.name);
    }

    @Override
    public int getItemCount() {
        return circles == null ? 0 : circles.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        TextView title;

        MyViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.circleTitle);
        }
    }
}
