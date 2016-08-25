package com.abborg.glom.activities;

import android.content.Intent;
import android.database.SQLException;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.abborg.glom.ApplicationState;
import com.abborg.glom.R;
import com.abborg.glom.adapters.ItemTouchHelperCallback;
import com.abborg.glom.adapters.ListItemAdapter;
import com.abborg.glom.data.DataProvider;
import com.abborg.glom.interfaces.ItemStateChangedListener;
import com.abborg.glom.interfaces.ItemTouchListener;
import com.abborg.glom.model.BoardItem;
import com.abborg.glom.model.CheckedItem;
import com.abborg.glom.model.ListItem;
import com.abborg.glom.utils.VerticalSpaceItemDecoration;

import org.joda.time.DateTime;

import java.util.Collections;
import java.util.List;

public class ListItemActivity extends AppCompatActivity
        implements ItemStateChangedListener, ItemTouchListener {

    public static final String TAG = "ListItemActivity";

    private DataProvider dataProvider;

    private EditText titleText;
    private RecyclerView listItemView;
    private ListItemAdapter adapter;
    private static final int ITEMS_VERTICAL_SPACE = 0;

    private Mode mode;
    private ListItem listItem;

    private enum Mode {
        CREATE,
        UPDATE,
        VIEW
    }

    /**********************************************************
     * View Initializations
     **********************************************************/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_item);

        dataProvider = ApplicationState.getInstance().getDataProvider();

        // set up all variables
        Intent intent = getIntent();
        if (intent == null) {
            finish(); // end abruptly if we don't know what MODE we're in
        }

        setContentView(R.layout.activity_list_item);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_action_close);
        }

        listItemView = (RecyclerView) findViewById(R.id.item_list_view);
        titleText = (EditText) findViewById(R.id.title_text);
        TextView newItemText = (TextView) findViewById(R.id.new_item);
        if (newItemText != null) {
            newItemText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemWillAdd(listItem.getItems().size()-1, null, null);
                }
            });
        }

        loadData(intent);
        adapter = new ListItemAdapter(this, listItem.getItems(), this);

        listItemView.setHasFixedSize(false);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        listItemView.setLayoutManager(layoutManager);
        listItemView.setAdapter(adapter);

        // add spacing decorator to the recyclerview
        listItemView.addItemDecoration(new VerticalSpaceItemDecoration(ITEMS_VERTICAL_SPACE));

        // add swipe and drag capabilities to the recyclerview
        ItemTouchHelper.Callback callback = new ItemTouchHelperCallback(this);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(listItemView);
        adapter.setItemTouchHelper(touchHelper);
    }

    @Override
    protected void onResume() {
        try {
            dataProvider.open();
        }
        catch (SQLException ex) {
            Log.e(TAG, ex.getMessage());
        }

        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        }

        else if (id == R.id.action_list_done) {
            saveAndFinish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void saveAndFinish() {
        listItem.setTitle(String.valueOf(titleText.getText()));

        if (mode == Mode.CREATE) {
            dataProvider.createListAsync(ApplicationState.getInstance().getActiveCircle(),
                    DateTime.now(), listItem, true);
        }
        else if (mode == Mode.UPDATE) {
            dataProvider.updateListAsync(ApplicationState.getInstance().getActiveCircle(), DateTime.now(), listItem, true);
        }

        finish();
    }

    private void loadData(Intent intent) {
        if (intent.getAction().equals(getResources().getString(R.string.ACTION_CREATE_LIST)))
            mode = Mode.CREATE;
        else if (intent.getAction().equals(getResources().getString(R.string.ACTION_UPDATE_LIST)))
            mode = Mode.UPDATE;
        else
            mode = Mode.VIEW;

        switch (mode) {

            case CREATE:
                listItem = ListItem.createList(ApplicationState.getInstance().getActiveCircle());
                listItem.getItems().add(new CheckedItem(ListItem.STATE_DEFAULT, null));
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(getString(R.string.title_activity_new_list_item));
                }
                break;
            case UPDATE: {
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(getString(R.string.title_activity_edit_list_item));
                }
                String id = intent.getStringExtra(getString(R.string.EXTRA_LIST_ID));
                List<BoardItem> boardItems = ApplicationState.getInstance().getActiveCircle().getItems();
                for (BoardItem item : boardItems) {
                    if (item.getId().equals(id) && item.getType() == BoardItem.TYPE_LIST) {
                        listItem = (ListItem) item;
                        break;
                    }
                }

                titleText.setText(listItem.getTitle());
                break;
            }
            case VIEW: {
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(getString(R.string.title_activity_view_list_item));
                }
                String id = intent.getStringExtra(getString(R.string.EXTRA_LIST_ID));
                List<BoardItem> boardItems = ApplicationState.getInstance().getActiveCircle().getItems();
                for (BoardItem item : boardItems) {
                    if (item.getId().equals(id) && item.getType() == BoardItem.TYPE_LIST) {
                        listItem = (ListItem) item;
                        break;
                    }
                }

                titleText.setText(listItem.getTitle());
            }
        }
    }

    @Override
    public void onBackPressed() {
        saveAndFinish();
    }

    /**********************************************************
     * Item Selection Callbacks
     **********************************************************/

    @Override
    public void onItemSelected(int index, CheckedItem item) {
        item.setState(ListItem.STATE_CHECKED);
    }

    @Override
    public void onItemUnselected(int index, CheckedItem item) {
        item.setState(ListItem.STATE_DEFAULT);
    }

    @Override
    public void onItemContentChanged(int index, CheckedItem item, String text) {
        item.setText(text);
    }

    @Override
    public void onItemWillAdd(int index, CheckedItem item, String text) {
        try {
            int addIndex = index + 1;
            listItem.getItems().add(addIndex, new CheckedItem(ListItem.STATE_DEFAULT, text));

            adapter.notifyItemInserted(addIndex);
            adapter.setFocusOnItem(addIndex);
        }
        catch (Exception ex) {
            Log.e(TAG, ex.getMessage());
        }
    }

    @Override
    public void onItemWillRemove(int index, CheckedItem item, boolean shouldAppendToPreviousItem) {
        try {
            int prevIndex = Math.max(0, index - 1);

            if (shouldAppendToPreviousItem) {
                CheckedItem prevItem = listItem.getItem(prevIndex);
                String newText = TextUtils.isEmpty(item.getText()) ? prevItem.getText() : prevItem.getText() + item.getText();
                prevItem.setText(newText);
                adapter.notifyItemChanged(prevIndex);
            }
            listItem.getItems().remove(index);
            adapter.notifyItemRemoved(index);

            View view = listItemView.getChildAt(prevIndex);
            if (view != null) {
                view.requestFocus();
            }
        }
        catch (Exception ex) {
            Log.e(TAG, ex.getMessage());
        }
    }

    @Override
    public void onItemMoved(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(listItem.getItems(), i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(listItem.getItems(), i, i - 1);
            }
        }

        adapter.notifyItemMoved(fromPosition, toPosition);
    }

    @Override
    public void onItemDismissed(int position) {
        listItem.getItems().remove(position);

        adapter.notifyItemRemoved(position);
        View view = listItemView.getChildAt(Math.max(0, position-1));
        if (view != null) {
            view.requestFocus();
        }
    }
}
