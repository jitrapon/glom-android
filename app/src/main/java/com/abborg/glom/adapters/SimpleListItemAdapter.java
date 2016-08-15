package com.abborg.glom.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.abborg.glom.R;
import com.abborg.glom.model.BoardItem;
import com.abborg.glom.model.CheckedItem;
import com.abborg.glom.model.ListItem;
import com.abborg.glom.model.TextItem;

import java.util.List;

/**
 * Created by jitrapon
 */
public class SimpleListItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;

    private List<CheckedItem> items;

    public class SimpleListItemHolder extends RecyclerView.ViewHolder {

        CheckBox checkbox;
        TextView text;

        public SimpleListItemHolder(View itemView) {
            super(itemView);

            checkbox = (CheckBox) itemView.findViewById(R.id.checkbox);
            text = (TextView) itemView.findViewById(R.id.item_text);
        }
    }

    public SimpleListItemAdapter(Context context, List<CheckedItem> items) {
        this.context = context;
        this.items = items;
    }

    public void setItems(List<CheckedItem> items) {
        this.items = items;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list_simple, parent, false);
        return new SimpleListItemHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        CheckedItem checkedItem = items.get(position);
        int state = checkedItem.getState();
        BoardItem item = checkedItem.getItem();
        final SimpleListItemHolder viewHolder = (SimpleListItemHolder) holder;

        viewHolder.checkbox.setChecked(state == ListItem.STATE_CHECKED);
        if (item instanceof TextItem) {
            viewHolder.text.setText(((TextItem)item).getText());
        }
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }
}
