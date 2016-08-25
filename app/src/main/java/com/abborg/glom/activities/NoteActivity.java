package com.abborg.glom.activities;

import android.content.Intent;
import android.database.SQLException;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import com.abborg.glom.ApplicationState;
import com.abborg.glom.R;
import com.abborg.glom.data.DataProvider;
import com.abborg.glom.model.BoardItem;
import com.abborg.glom.model.NoteItem;

import org.joda.time.DateTime;

import java.util.List;

public class NoteActivity extends AppCompatActivity {

    public static final String TAG = "NoteItemActivity";

    private DataProvider dataProvider;

    private EditText titleText;
    private EditText contentText;

    private Mode mode;
    private NoteItem noteItem;

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
        setContentView(R.layout.activity_note);

        dataProvider = ApplicationState.getInstance().getDataProvider();

        // set up all variables
        Intent intent = getIntent();
        if (intent == null) {
            finish(); // end abruptly if we don't know what MODE we're in
        }

        setContentView(R.layout.activity_note);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_action_close);
        }

        contentText = (EditText) findViewById(R.id.content_text);
        titleText = (EditText) findViewById(R.id.title_text);
        if (contentText != null) {
            contentText.requestFocus();
        }

        loadData(intent);
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
        getMenuInflater().inflate(R.menu.menu_note, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        }

        else if (id == R.id.action_done) {
            saveAndFinish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void saveAndFinish() {
        noteItem.setTitle(String.valueOf(titleText.getText()));
        noteItem.setText(String.valueOf(contentText.getText()));

        if (mode == Mode.CREATE) {
            dataProvider.createNoteAsync(ApplicationState.getInstance().getActiveCircle(),
                    DateTime.now(), noteItem, true);
        }
        else if (mode == Mode.UPDATE) {
            dataProvider.updateNoteAsync(ApplicationState.getInstance().getActiveCircle(), DateTime.now(), noteItem, true);
        }

        finish();
    }

    private void loadData(Intent intent) {
        if (intent.getAction().equals(getResources().getString(R.string.ACTION_CREATE_NOTE)))
            mode = Mode.CREATE;
        else if (intent.getAction().equals(getResources().getString(R.string.ACTION_UPDATE_NOTE)))
            mode = Mode.UPDATE;
        else
            mode = Mode.VIEW;

        switch (mode) {

            case CREATE:
                noteItem = NoteItem.createNote(ApplicationState.getInstance().getActiveCircle());
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(getString(R.string.title_activity_new_note_item));
                }
                break;
            case UPDATE: {
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(getString(R.string.title_activity_edit_note_item));
                }
                String id = intent.getStringExtra(getString(R.string.EXTRA_NOTE_ID));
                List<BoardItem> boardItems = ApplicationState.getInstance().getActiveCircle().getItems();
                for (BoardItem item : boardItems) {
                    if (item.getId().equals(id) && item.getType() == BoardItem.TYPE_NOTE) {
                        noteItem = (NoteItem) item;
                        break;
                    }
                }

                titleText.setText(noteItem.getTitle());
                contentText.setText(noteItem.getText());
                break;
            }
            case VIEW: {
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(getString(R.string.title_activity_view_note_item));
                }
                String id = intent.getStringExtra(getString(R.string.EXTRA_NOTE_ID));
                List<BoardItem> boardItems = ApplicationState.getInstance().getActiveCircle().getItems();
                for (BoardItem item : boardItems) {
                    if (item.getId().equals(id) && item.getType() == BoardItem.TYPE_NOTE) {
                        noteItem = (NoteItem) item;
                        break;
                    }
                }

                titleText.setText(noteItem.getTitle());
                contentText.setText(noteItem.getText());
                break;
            }
        }
    }

    @Override
    public void onBackPressed() {
        saveAndFinish();
    }
}
