package com.abborg.glom.model;

public class EmptyItem extends ExploreItem {

    public EmptyItem() { super(null, null, null); }

    @Override
    public int getType() {
        return ExploreItem.TYPE_EMPTY;
    }
}
