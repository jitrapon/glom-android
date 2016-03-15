package com.abborg.glom.interfaces;

public interface BoardItemChangeListener {

    void onItemAdded(String id);

    void onItemModified(String id);

    void onItemDeleted(String id);
}
