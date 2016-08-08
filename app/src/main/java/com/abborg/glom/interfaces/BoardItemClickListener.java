package com.abborg.glom.interfaces;

import android.support.annotation.IdRes;

import com.abborg.glom.model.BoardItem;

/**
 * Created by jitrapon
 */
public interface BoardItemClickListener {

    void onItemClicked(BoardItem item, int position);

    boolean onItemLongClicked(BoardItem item, int position);

    void onActionButtonClicked(BoardItem item, @IdRes int buttonId);
}
