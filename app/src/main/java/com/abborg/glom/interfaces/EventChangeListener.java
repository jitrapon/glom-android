package com.abborg.glom.interfaces;

public interface EventChangeListener {

    void onEventAdded(String id);

    void onEventModified(String id);

    void onEventDeleted(String id);
}
