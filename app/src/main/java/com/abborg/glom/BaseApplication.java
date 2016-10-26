package com.abborg.glom;

import android.app.Application;

import com.abborg.glom.di.ComponentInjector;

public class BaseApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        ComponentInjector.init(this);
    }
}
