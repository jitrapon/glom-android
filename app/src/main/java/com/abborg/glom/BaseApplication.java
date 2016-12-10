package com.abborg.glom;

import android.app.Application;

import com.abborg.glom.di.ComponentInjector;
import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;

public class BaseApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());

        ComponentInjector.init(this);
    }
}
