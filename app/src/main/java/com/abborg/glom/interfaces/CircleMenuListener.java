package com.abborg.glom.interfaces;

import com.abborg.glom.model.MenuActionItem;

/**
 * Created by Jitrapon on 12/11/2559.
 */
public interface CircleMenuListener {

    void onCircleMenuOptionsClicked(MenuActionItem item);

    void onOtherCircleMenuOptionClicked();

    void onCircleMenuOptionsOpening();

    void onCircleMenuOptionsClosing();
}
