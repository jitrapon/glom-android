package com.abborg.glom.interfaces;

/**
 * Created by jitrapon
 */
public interface ItemTouchListener {

    void onItemMoved(int fromPosition, int toPosition);

    void onItemDismissed(int position);
}
