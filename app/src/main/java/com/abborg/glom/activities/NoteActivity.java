package com.abborg.glom.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.abborg.glom.AppState;
import com.abborg.glom.R;
import com.abborg.glom.data.DataUpdater;
import com.abborg.glom.model.Circle;
import com.abborg.glom.model.User;

/**
 * Created by jitrapon on 17/5/16.
 */
public class NoteActivity extends AppCompatActivity {

    private static final String TAG = "NoteActivity";

    /** Circle state information **/
    AppState appState;
    Circle circle;
    User user;
    DataUpdater dataUpdater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        appState = AppState.getInstance();
        if (appState == null || appState.getDataUpdater() == null) {
            finish();
        }
        circle = appState.getActiveCircle();
        user = appState.getActiveUser();
        dataUpdater = appState.getDataUpdater();

        setContentView(R.layout.activity_note);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.title_activity_note_unsaved));
        }


    }


}
