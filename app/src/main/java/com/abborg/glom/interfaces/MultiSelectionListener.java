package com.abborg.glom.interfaces;

import java.util.List;

/**
 * Created by jitrapon on 21/6/16.
 */
public interface MultiSelectionListener {

    void toggleSelection(int pos);

    void clearSelections();

    int getSelectedItemCount();

    List<Integer> getSelectedItems();
}
