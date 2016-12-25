package com.abborg.glom.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.abborg.glom.R;
import com.abborg.glom.model.CircleInfo;
import com.abborg.glom.model.NavMenuHeader;
import com.abborg.glom.model.NavMenuItem;
import com.abborg.glom.utils.CircleTransform;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import static com.abborg.glom.model.NavMenuItem.TYPE_HEADER;

/**
 * Adapter that supports sticky header like iOS list on the side navigation bar
 */
public class NavMenuAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<NavMenuItem> items;

    private Context context;

    public NavMenuAdapter(Context ctx, List<CircleInfo> circles) {
        context = ctx;
        items = new ArrayList<>();
        setCircles(circles);
    }

    public void setCircles(List<CircleInfo> circles) {
        items.clear();
        items.add(new NavMenuHeader(context.getResources().getString(R.string.nav_menu_header_circle)));
        items.addAll(circles);

        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            return new HeaderViewHolder(LayoutInflater.from(context).inflate(R.layout.nav_menu_header, parent, false));
        }
        else {
            return new CircleViewHolder(LayoutInflater.from(context).inflate(R.layout.nav_menu_item, parent, false));
        }
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).getType();
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (items.get(position) instanceof NavMenuHeader) {
            NavMenuHeader header = (NavMenuHeader) items.get(position);
            HeaderViewHolder viewHolder = (HeaderViewHolder) holder;
            int numCircle = getItemCount() - 1;
            viewHolder.header.setText(String.format(header.getTitle(), numCircle));
        }
        else if (items.get(position) instanceof CircleInfo){
            CircleInfo current = (CircleInfo) items.get(position);
            CircleViewHolder viewHolder = (CircleViewHolder) holder;
            Glide.with(context)
                    .load(current.avatar)
                    .transform(new CircleTransform(context))
                    .override((int) context.getResources().getDimension(R.dimen.nav_menu_circle_avatar),
                            (int) context.getResources().getDimension(R.dimen.nav_menu_circle_avatar))
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .crossFade(300)
                    .into(viewHolder.avatar);
            viewHolder.title.setText(current.name);
        }
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    public NavMenuItem getItemAtPosition(int position) {
        return items.get(position);
    }

    private static class CircleViewHolder extends RecyclerView.ViewHolder {
        ImageView avatar;
        TextView title;

        CircleViewHolder(View itemView) {
            super(itemView);
            avatar = (ImageView) itemView.findViewById(R.id.circle_avatar);
            title = (TextView) itemView.findViewById(R.id.circle_title);
        }
    }

    private static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView header;

        HeaderViewHolder(View itemView) {
            super(itemView);

            header = (TextView) itemView.findViewById(R.id.title);
        }
    }
}
