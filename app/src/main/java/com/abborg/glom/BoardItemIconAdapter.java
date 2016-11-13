package com.abborg.glom;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.abborg.glom.adapters.MenuActionItemClickListener;
import com.abborg.glom.model.MenuActionItem;

import java.util.List;

public class BoardItemIconAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "BoardItemIconAdapter";

    private Context context;
    private MenuActionItemClickListener listener;

    private List<MenuActionItem> items;

    /**************************************************
     * VIEW HOLDERS
     **************************************************/

    public static class BoardItemActionViewHolder extends RecyclerView.ViewHolder {

        ImageView icon;
        TextView label;

        public BoardItemActionViewHolder(View itemView) {
            super(itemView);

            icon = (ImageView) itemView.findViewById(R.id.icon);
            label = (TextView) itemView.findViewById(R.id.label);
        }

        public void bind(final MenuActionItem item, final MenuActionItemClickListener listener) {
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    listener.onItemClicked(item);
                }
            });
        }
    }

    public BoardItemIconAdapter(Context context, List<MenuActionItem> items, MenuActionItemClickListener listener) {
        this.listener = listener;
        this.items = items;
        this.context = context;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.icon_board_item, parent, false);
        view.setClickable(true);
        return new BoardItemActionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        MenuActionItem item = items.get(position);
        BoardItemActionViewHolder viewHolder = (BoardItemActionViewHolder) holder;

        viewHolder.bind(item, listener);

        viewHolder.icon.setImageDrawable(ContextCompat.getDrawable(context, item.getIcon()));
        viewHolder.label.setText(item.getLabel());
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }
}
