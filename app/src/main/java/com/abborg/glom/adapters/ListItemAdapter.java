package com.abborg.glom.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;

import com.abborg.glom.R;
import com.abborg.glom.interfaces.ItemStateChangedListener;
import com.abborg.glom.model.BoardItem;
import com.abborg.glom.model.CheckedItem;
import com.abborg.glom.model.ListItem;
import com.abborg.glom.model.TextItem;

import java.util.List;

/**
 * Created by jitrapon
 */
public class ListItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements TextWatcher {

    private static final String TAG = "ListItemAdapter";

    private Context context;

    private ItemStateChangedListener listener;
    private List<CheckedItem> items;

    private int focusedItemIndex = 0;
    private CheckedItem focusedItem;
    private EditText focusedEditText;

    /**************************************************
     * VIEW HOLDERS
     **************************************************/

    public class ListItemHolder extends RecyclerView.ViewHolder {

        ImageView dragButton;
        CheckBox checkbox;
        EditText content;
        ImageView deleteButton;

        CheckedItem item;

        public ListItemHolder(View itemView) {
            super(itemView);

            dragButton = (ImageView) itemView.findViewById(R.id.drag_item_button);
            checkbox = (CheckBox) itemView.findViewById(R.id.checkbox);
            content = (EditText) itemView.findViewById(R.id.content_text);
            deleteButton = (ImageView) itemView.findViewById(R.id.delete_item_button);

            content.setOnFocusChangeListener(new View.OnFocusChangeListener() {

                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        Log.d(TAG, "onFocusChange called at " + getAdapterPosition());
                        focusedItemIndex = getAdapterPosition();
                        focusedItem = item;
                        focusedEditText = content;
                        deleteButton.setVisibility(View.VISIBLE);
                    }
                    else {
                        deleteButton.setVisibility(View.INVISIBLE);
                    }
                }
            });

            checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) listener.onItemSelected(getAdapterPosition(), item);
                    else listener.onItemUnselected(getAdapterPosition(), item);
                }
            });

            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onItemWillRemove(getAdapterPosition(), item);
                }
            });
        }

        public void setItem(CheckedItem item) {
            this.item = item;
        }
    }

    public ListItemAdapter(Context context, List<CheckedItem> items, ItemStateChangedListener listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list, parent, false);
        return new ListItemHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        CheckedItem checkedItem = items.get(position);
        int state = checkedItem.getState();
        BoardItem item = checkedItem.getItem();
        ListItemHolder viewHolder = (ListItemHolder) holder;
        viewHolder.setItem(checkedItem);
        if (position == focusedItemIndex) {
            if (!viewHolder.content.hasFocus()) viewHolder.content.requestFocus();
        }

        viewHolder.checkbox.setChecked(state == ListItem.STATE_CHECKED);
        if (item instanceof TextItem) {
            viewHolder.content.removeTextChangedListener(this);
            viewHolder.content.setText(((TextItem)item).getText());
            viewHolder.content.addTextChangedListener(this);
        }
        viewHolder.deleteButton.setVisibility(View.INVISIBLE);
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    public void setFocusOnItem(int index) {
        focusedItemIndex = index;
    }

    /**************************************************
     * TEXT WATCHER
     **************************************************/

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {}

    @Override
    public void afterTextChanged(Editable s) {
        Log.d(TAG, s.toString());
        Log.d(TAG, "EditText: " + focusedEditText + ", item: " + focusedItem + ", index: " + focusedItemIndex);
        if (focusedEditText != null & focusedItem != null && focusedItemIndex != -1) {
            String text = s.toString();
            String nextLine = null;
            if (text.contains("\n")) {
                Log.d(TAG, "Contains newline");
                String tokens[] = text.split("\n");
                if (tokens.length == 2) {
                    text = tokens[0];
                    nextLine = tokens[1].replace("\n", "");
                }

                focusedEditText.removeTextChangedListener(this);
                focusedEditText.setText(text.replace("\n", ""));
                focusedEditText.addTextChangedListener(this);

                listener.onItemWillAdd(focusedItemIndex, focusedItem, nextLine);
            }
            listener.onItemContentChanged(focusedItemIndex, focusedItem, text.replace("\n", ""));
        }
    }
}
