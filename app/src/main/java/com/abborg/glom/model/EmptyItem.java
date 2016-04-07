package com.abborg.glom.model;

public class EmptyItem extends DiscoverItem {

    public EmptyItem() { super(null, null, null); }

    @Override
    public int getType() {
        return DiscoverItem.TYPE_EMPTY;
    }
}
