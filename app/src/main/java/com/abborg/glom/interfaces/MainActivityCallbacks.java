package com.abborg.glom.interfaces;

import android.os.Handler;

/**
 * Created by Jitrapon on 20/11/2559.
 */
public interface MainActivityCallbacks {

    void onToolbarNavIconChanged(String imageUrl);

    void onToolbarTitleChanged(String title);

    void onToolbarSubtitleChanged(String subtitle);

    void onShowNotificationBar(int bgColor, String text, long duration);

    void onOpenCircleMenu();

    void onSetFabVisible(boolean visible);

    Handler getThreadHandler();

    void onShowCategoryBar();
}
