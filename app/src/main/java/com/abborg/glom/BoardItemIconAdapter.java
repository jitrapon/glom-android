package com.abborg.glom;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.abborg.glom.adapters.BoardItemAction;
import com.abborg.glom.adapters.BoardItemActionClickListener;

import java.util.List;

public class BoardItemIconAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "BoardItemIconAdapter";

    private Context context;
    private BoardItemActionClickListener listener;

    private List<BoardItemAction> items;

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

        public void bind(final BoardItemAction item, final BoardItemActionClickListener listener) {
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    listener.onItemClicked(item);
                }
            });
        }
    }

    public BoardItemIconAdapter(Context context, List<BoardItemAction> items, BoardItemActionClickListener listener) {
        this.listener = listener;
        this.items = items;
        this.context = context;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.icon_board_item, parent, false);
        return new BoardItemActionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        BoardItemAction item = items.get(position);
        BoardItemActionViewHolder viewHolder = (BoardItemActionViewHolder) holder;

        viewHolder.icon.setImageDrawable(ContextCompat.getDrawable(context, item.getIcon()));
        viewHolder.label.setText(item.getLabel());
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }
}
