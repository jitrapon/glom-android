package com.abborg.glom.interfaces;

import com.abborg.glom.model.CheckedItem;

/**
 * Created by jitrapon
 */
public interface ItemStateChangedListener {

    void onItemSelected(int index, CheckedItem item);

    void onItemUnselected(int index, CheckedItem item);

    void onItemContentChanged(int index, CheckedItem item, String text);

    void onItemWillAdd(int index, CheckedItem item, String text);

    void onItemWillRemove(int index, CheckedItem item);
}
