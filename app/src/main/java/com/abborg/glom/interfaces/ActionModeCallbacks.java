package com.abborg.glom.interfaces;

import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

public interface ActionModeCallbacks {

    boolean onCreateActionMode(ActionMode mode, Menu menu);

    boolean onPrepareActionMode(ActionMode mode, Menu menu);

    boolean onActionItemClicked(ActionMode mode, MenuItem item);

    void onDestroyActionMode(ActionMode mode);
}
