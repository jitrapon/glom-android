package com.abborg.glom.adapters;

import android.content.Context;
import android.graphics.Paint;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.abborg.glom.R;
import com.abborg.glom.model.CheckedItem;
import com.abborg.glom.model.ListItem;

import java.util.ArrayList;
import java.util.List;

import static com.abborg.glom.model.ListItem.STATE_CHECKED;

/**
 * Created by jitrapon
 */
public class SimpleListItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;

    private List<CheckedItem> items;

    private static final int MAX_DISPLAYED_ITEMS = 10;

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
        this.items.clear();

        int totalDisplayedItems = Math.min(items.size(), MAX_DISPLAYED_ITEMS);
        List<CheckedItem> uncheckedItems = new ArrayList<>();
        List<CheckedItem> checkedItems = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            CheckedItem item = items.get(i);
            if (item.getState() == ListItem.STATE_DEFAULT) {
                uncheckedItems.add(item);
            }
            else if (item.getState() == STATE_CHECKED) {
                checkedItems.add(item);
            }
        }

        for (int i = 0; i < totalDisplayedItems; i++) {
            if (uncheckedItems.size() > 0) {
                this.items.add(uncheckedItems.remove(0));
            }
            else if (checkedItems.size() > 0) {
                this.items.add(checkedItems.remove(0));
            }
        }
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
        String text = checkedItem.getText();
        final SimpleListItemHolder viewHolder = (SimpleListItemHolder) holder;


        viewHolder.checkbox.setChecked(state == STATE_CHECKED);
        viewHolder.text.setTextColor((state == STATE_CHECKED) ?
                ContextCompat.getColor(context, R.color.textColorLight)
                : ContextCompat.getColor(context, R.color.textColorDark));
        viewHolder.text.setPaintFlags((state == STATE_CHECKED) ?
                viewHolder.text.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                : viewHolder.text.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
        viewHolder.text.setText(text);
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }
}
