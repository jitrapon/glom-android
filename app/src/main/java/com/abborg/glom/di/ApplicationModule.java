package com.abborg.glom.di;

import android.app.Application;

import com.abborg.glom.ApplicationState;
import com.abborg.glom.data.DataProvider;
import com.abborg.glom.utils.HttpClient;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Main application module that provides instances of all classes used throughout
 * the application using dependency injection
 *
 * @author Jitrapon
 *
 */
@Module
public class ApplicationModule {

    private Application application;

    public ApplicationModule(Application app) {
        application = app;
    }

    @Provides
    @Singleton
    public ApplicationState provideApplicationState() {
        return new ApplicationState(application);
    }

    @Provides
    @Singleton
    public DataProvider provideDataProvider() {
        return new DataProvider(application);
    }

    @Provides
    public GoogleCloudMessaging provideGoogleCloudMessaging() {
        return GoogleCloudMessaging.getInstance(application);
    }

    @Provides
    @Singleton
    public HttpClient provideHttpClient() {
        return new HttpClient(application);
    }
}
