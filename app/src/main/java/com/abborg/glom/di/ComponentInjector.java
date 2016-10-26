package com.abborg.glom.di;

import android.app.Application;

public class ComponentInjector {

    /** Reference this instance always when injecting into a class **/
    public static ComponentInjector INSTANCE;

    private ApplicationComponent component;

    public static void init(Application app) {
        if (INSTANCE == null) {
            INSTANCE = new ComponentInjector();
            INSTANCE.component = DaggerApplicationComponent
                    .builder()
                    .applicationModule(new ApplicationModule(app))
                    .build();
        }
    }

    public ApplicationComponent getApplicationComponent() {
        return component;
    }
}
