package com.abborg.glom.activities;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.customtabs.CustomTabsCallback;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.customtabs.CustomTabsSession;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.abborg.glom.ApplicationState;
import com.abborg.glom.R;
import com.abborg.glom.data.DataProvider;
import com.abborg.glom.di.ComponentInjector;

import java.util.List;

import javax.inject.Inject;

/**
 * Base Activity that some activities can extend for sharing functions doing permission checking,
 * common task utilities functions, and handler functions
 *
 * Created by Jitrapon on 13/11/2559.
 */
public class BaseActivity extends AppCompatActivity {

    @Inject
    ApplicationState appState;

    @Inject
    DataProvider dataProvider;

    /* Chrome custom tab client */
    protected CustomTabsClient browserServiceClient;
    protected CustomTabsServiceConnection browserServiceConnection;
    protected boolean browserServiceIsBound;
    protected boolean willLaunchBrowser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ComponentInjector.INSTANCE.getApplicationComponent().inject(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // connect to the Google Play API
        appState.connectGoogleApiClient();

        dataProvider.openDB();

        willLaunchBrowser = false;
    }

    @Override
    protected void onStop() {
        // disconnect google api client
        if (appState != null)
            if (!appState.shouldKeepGoogleApiAlive()) appState.disconnectGoogleApiClient();

        // unbind browser services
        if (!willLaunchBrowser && browserServiceIsBound && browserServiceConnection != null) {
            unbindService(browserServiceConnection);
            browserServiceIsBound = false;
        }

        super.onStop();
    }

    /**
     *  Launch the default application that the user has chosen for links (e.g. Youtube app for Youtube links)
     * otherwise, for other url, launch the built-in browser
     * figure out which activity (in the list of installed third-party applications) can handle
     * this intent. If the activity is not that of a browser's, use the default app to launch.
     * Otherwise, use the Chrome custom tabs service for in-app browsing experience. */
    protected void launchThirdPartyUrlApp(String url) {
        try {
            final Uri uri = Uri.parse(url);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(uri);
            PackageManager packageManager = getPackageManager();
            List<ResolveInfo> matchedActivities = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            if (matchedActivities.size() > 0) {
                boolean isBrowserIntent = false;
                for (ResolveInfo activity : matchedActivities) {
                    if (activity.activityInfo.packageName.contains("sbrowser") ||
                            activity.activityInfo.packageName.contains("chrome")) {
                        isBrowserIntent = true;
                        break;
                    }
                }
                if (isBrowserIntent) {
                    if (browserServiceClient == null) {
                        browserServiceConnection = new CustomTabsServiceConnection() {

                            @Override
                            public void onServiceDisconnected(ComponentName name) {
                                browserServiceClient = null;
                            }

                            @Override
                            public void onCustomTabsServiceConnected(ComponentName componentName,
                                                                     CustomTabsClient customTabsClient) {
                                browserServiceClient = customTabsClient;
                                launchBrowserSession(uri);
                            }
                        };
                        boolean serviceAvailable = CustomTabsClient.bindCustomTabsService(this, getString(R.string.chrome_package_name),
                                browserServiceConnection);
                        if (!serviceAvailable) {
                            browserServiceIsBound = false;
                            startActivity(intent);
                        }
                        else {
                            browserServiceIsBound = true;
                        }
                    }
                    else {
                        launchBrowserSession(uri);
                    }
                }
                else {
                    startActivity(intent);
                }
            }
        }
        catch (Exception ex) {
            Log.e("ERROR", ex.getMessage());
        }
    }

    private void launchBrowserSession(Uri uri) {
        willLaunchBrowser = true;
        CustomTabsSession session = browserServiceClient.newSession(new CustomTabsCallback() {

            @Override
            public void onNavigationEvent(int navigationEvent, Bundle extras) {
                super.onNavigationEvent(navigationEvent, extras);
            }
        });
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder(session)
                .setToolbarColor(ContextCompat.getColor(this, R.color.colorPrimary))
                .setSecondaryToolbarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark))
                .setCloseButtonIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_action_close))
                .enableUrlBarHiding()
                .addDefaultShareMenuItem()
                .setShowTitle(true);
        CustomTabsIntent launchIntent = builder.build();
        launchIntent.launchUrl(this, uri);
    }
}
