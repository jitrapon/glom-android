package com.abborg.glom.activities;

import android.content.Intent;
import android.database.SQLException;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import com.abborg.glom.ApplicationState;
import com.abborg.glom.R;
import com.abborg.glom.adapters.ListItemAdapter;
import com.abborg.glom.data.DataProvider;
import com.abborg.glom.interfaces.ItemStateChangedListener;
import com.abborg.glom.model.BoardItem;
import com.abborg.glom.model.CheckedItem;
import com.abborg.glom.model.ListItem;
import com.abborg.glom.model.TextItem;

import java.util.List;

public class ListItemActivity extends AppCompatActivity
        implements ItemStateChangedListener {

    public static final String TAG = "ListItemActivity";

    private DataProvider dataProvider;

    /* The received intent, this should never be null because this activity is only launched from an intent */
    private Intent intent;

    private EditText titleText;
    private RecyclerView listItemView;
    private ListItemAdapter adapter;

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
        intent = getIntent();
        if (intent == null) {
            finishWithResult(null); // end abruptly if we don't know what MODE we're in
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

        loadData(intent);
        adapter = new ListItemAdapter(this, listItem.getItems(), this);

        listItemView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        listItemView.setLayoutManager(layoutManager);
        listItemView.setAdapter(adapter);
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
            finishWithResult(null);
            return true;
        }

        else if (id == R.id.action_list_done) {

            return true;
        }

        return super.onOptionsItemSelected(item);
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
                listItem.getItems().add(new CheckedItem(
                        ListItem.STATE_DEFAULT, TextItem.createText(ApplicationState.getInstance().getActiveCircle())));
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(getString(R.string.title_activity_new_list_item));
                }
                break;
            case UPDATE:
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(getString(R.string.title_activity_edit_list_item));
                }
                break;
            case VIEW:
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
        }
    }

    private void finishWithResult(Intent intent) {
        if (intent != null) {
            setResult(RESULT_OK, intent);
        }
        else {
            setResult(RESULT_CANCELED);
        }

        finish();
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
        if (item.getItem() instanceof TextItem) {
            ((TextItem)item.getItem()).setText(text);
        }
    }

    @Override
    public void onItemWillAdd(int index, CheckedItem item, String text) {
        int addIndex = index + 1;
        TextItem textItem = TextItem.createText(ApplicationState.getInstance().getActiveCircle());
        textItem.setText(text);
        listItem.getItems().add(addIndex, new CheckedItem(ListItem.STATE_DEFAULT, textItem));

        adapter.notifyItemInserted(addIndex);
        adapter.setFocusOnItem(addIndex);
    }

    @Override
    public void onItemWillRemove(int index, CheckedItem item) {
        Log.d(TAG, "Will remove item at index " + index);
//        listItem.getItems().remove(index);
//        adapter.notifyItemRemoved(index);
    }
}
