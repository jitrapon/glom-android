package com.abborg.glom.interfaces;

import com.abborg.glom.model.ExploreItem;

import java.util.List;

public interface ExploreItemChangeListener {

    void onItemsReceived(int type, List<ExploreItem> items);
}
