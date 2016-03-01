package com.abborg.glom.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.abborg.glom.R;
import com.abborg.glom.model.CircleInfo;

import java.util.Collections;
import java.util.List;

/**
 * Created by Boat on 17/9/58.
 */
public class NavigationDrawerAdapter extends RecyclerView.Adapter<NavigationDrawerAdapter.MyViewHolder> {

    List<CircleInfo> data = Collections.emptyList();

    private LayoutInflater inflater;

    private Context context;

    public NavigationDrawerAdapter(Context context, List<CircleInfo> data) {
        this.context = context;
        inflater = LayoutInflater.from(context);
        this.data = data;
    }

    public void delete(int position) {
        data.remove(position);
        notifyItemRemoved(position);
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.nav_drawer_row, parent, false);
        MyViewHolder holder = new MyViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        CircleInfo current = data.get(position);
        int memberCount = current.numUsers;
        holder.title.setText(current.title + " (" + memberCount + ")");
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        TextView title;

        public MyViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.circleTitle);
        }
    }
}
