package com.abborg.glom.interfaces;

import com.abborg.glom.model.DiscoverItem;

import java.util.List;

public interface DiscoverItemChangeListener {

    void onItemsReceived(int type, List<DiscoverItem> items);
}
