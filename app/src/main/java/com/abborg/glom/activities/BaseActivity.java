package com.abborg.glom.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.customtabs.CustomTabsCallback;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.customtabs.CustomTabsSession;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.abborg.glom.ApplicationState;
import com.abborg.glom.BoardItemIconAdapter;
import com.abborg.glom.Const;
import com.abborg.glom.R;
import com.abborg.glom.adapters.MenuActionItemClickListener;
import com.abborg.glom.data.DataProvider;
import com.abborg.glom.di.ComponentInjector;
import com.abborg.glom.interfaces.BroadcastLocationListener;
import com.abborg.glom.model.LinkItem;
import com.abborg.glom.model.MenuActionItem;
import com.abborg.glom.service.CirclePushService;
import com.abborg.glom.utils.BottomSheetItemDecoration;
import com.abborg.glom.utils.TaskUtils;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import pub.devrel.easypermissions.EasyPermissions;

/**
 * Base Activity that some activities can extend for sharing functions doing permission checking,
 * common task utilities functions, and handler functions
 *
 * Created by Jitrapon on 13/11/2559.
 */
public class BaseActivity extends AppCompatActivity implements
        MenuActionItemClickListener,
        EasyPermissions.PermissionCallbacks {

    @Inject
    ApplicationState appState;

    @Inject
    DataProvider dataProvider;

    /* Chrome custom tab client */
    protected CustomTabsClient browserServiceClient;
    protected CustomTabsServiceConnection browserServiceConnection;
    protected boolean browserServiceIsBound;
    protected boolean willLaunchBrowser;

    protected BottomSheetDialog boardItemBottomSheet;
    protected BottomSheetDialog broadcastLocationBottomSheet;
    protected View boardItemActionSheetLayout;
    protected View broadcastLocationSheetLayout;
    protected SwitchCompat broadcastLocationToggle;

    protected List<BroadcastLocationListener> broadcastLocationListeners;

    // permission
    protected static final int PERMISSION_LOCATION = 1;
    protected static final int PERMISSION_READ_STORAGE = 2;
    protected static final int PERMISSION_WRITE_STORAGE = 3;

    private static final String TAG = "BaseAactivity";

    /**********************************************************
     * PERMISSION CALLBACK
     **********************************************************/

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        String rationale;
        if (requestCode == PERMISSION_LOCATION) {
            rationale = getString(R.string.permission_location_rationale);
        }
        else if (requestCode == PERMISSION_READ_STORAGE) {
            rationale = getString(R.string.permission_read_external_storage_rationale);
        }
        else if (requestCode == PERMISSION_WRITE_STORAGE) {
            rationale = getString(R.string.permission_write_external_storage_rationale);
        }
        else {
            rationale = getString(R.string.permission_generic_rationale);
        }

        EasyPermissions.checkDeniedPermissionsNeverAskAgain(
                this,
                rationale,
                R.string.dialog_permission_request_settings,
                R.string.dialog_permission_request_cancel,
                null,
                perms);
    }

    @SuppressLint("InflateParams")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ComponentInjector.INSTANCE.getApplicationComponent().inject(this);

        boardItemActionSheetLayout = getLayoutInflater().inflate(R.layout.bottom_sheet_board_items, null);
        broadcastLocationSheetLayout = getLayoutInflater().inflate(R.layout.bottom_sheet_broadcast_location, null);
    }

    protected void addBroadcastLocationListener(BroadcastLocationListener listener) {
        if (broadcastLocationListeners == null) {
            broadcastLocationListeners = new ArrayList<>();
        }
        broadcastLocationListeners.add(listener);
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

    protected void setupBroadcastLocationSheet() {
        // set up the bottom sheets
        List<MenuActionItem> boardMenuItems = Arrays.asList(
                MenuActionItem.IMAGE,
                MenuActionItem.AUDIO,
                MenuActionItem.VIDEO,
                MenuActionItem.ALARM,
                MenuActionItem.DRAW,
                MenuActionItem.NOTE,
                MenuActionItem.EVENT,
                MenuActionItem.LINK,
                MenuActionItem.LOCATION,
                MenuActionItem.LIST
        );

        BoardItemIconAdapter iconAdapter = new BoardItemIconAdapter(this, boardMenuItems, this);
        RecyclerView recyclerView = (RecyclerView) boardItemActionSheetLayout.findViewById(R.id.board_item_actions_recyclerview);
        if (recyclerView != null) {
            recyclerView.setHasFixedSize(true);
            LinearLayoutManager layoutManager = new GridLayoutManager(this, 3);
            recyclerView.setLayoutManager(layoutManager);
            recyclerView.setAdapter(iconAdapter);
            recyclerView.addItemDecoration(new BottomSheetItemDecoration(3, 16, false));
        }

        broadcastLocationToggle = (SwitchCompat) broadcastLocationSheetLayout.findViewById(R.id.toggleBroadcastLocationSwitch);
        broadcastLocationToggle.setChecked(appState.getActiveCircle().isUserBroadcastingLocation());

        // set up broadcast location sheet
        final ImageButton endTimePickerHourIncr = (ImageButton) broadcastLocationSheetLayout.findViewById(R.id.endTimePickerHourIncr);
        final TextView endTimePickerHour = (TextView) broadcastLocationSheetLayout.findViewById(R.id.endTimePickerHour);
        endTimePickerHourIncr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int hour = Integer.parseInt(endTimePickerHour.getText().toString());
                int incrHour = hour+1 > 12 ? 1 : hour+1;
                endTimePickerHour.setText(incrHour + "");
            }
        });
        final ImageButton endTimePickerMinuteIncr = (ImageButton) broadcastLocationSheetLayout.findViewById(R.id.endTimePickerMinuteIncr);
        final TextView endTimePickerMinute = (TextView) broadcastLocationSheetLayout.findViewById(R.id.endTimePickerMinute);
        endTimePickerMinuteIncr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int minute = Integer.parseInt(endTimePickerMinute.getText().toString());
                int incrMinute = minute+1 > 59 ? 0 : minute+1;
                endTimePickerMinute.setText(String.format("%02d", incrMinute));
            }
        });
        final ImageButton endTimePickerHourDecr = (ImageButton) broadcastLocationSheetLayout.findViewById(R.id.endTimePickerHourDecr);
        endTimePickerHourDecr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int hour = Integer.parseInt(endTimePickerHour.getText().toString());
                int decrHour = hour-1 < 1 ? 12 : hour-1;
                endTimePickerHour.setText(decrHour + "");
            }
        });
        final ImageButton endTimePickerMinuteDecr = (ImageButton) broadcastLocationSheetLayout.findViewById(R.id.endTimePickerMinuteDecr);
        endTimePickerMinuteDecr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int minute = Integer.parseInt(endTimePickerMinute.getText().toString());
                int decrMinute = minute-1 < 0 ? 59 : minute-1;
                endTimePickerMinute.setText(String.format("%02d", decrMinute));
            }
        });
        final TextView endTimeAMPMPicker = (TextView) broadcastLocationSheetLayout.findViewById(R.id.endTimePickerAMPM);
        endTimeAMPMPicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String amPm = endTimeAMPMPicker.getText().toString();
                if (amPm.equals(getResources().getString(R.string.time_unit_before_noon))) {
                    endTimeAMPMPicker.setText(getResources().getString(R.string.time_unit_after_noon));
                }
                else {
                    endTimeAMPMPicker.setText(getResources().getString(R.string.time_unit_before_noon));
                }
            }
        });

        // set up broadcast location toggle
        final Context context = this;
        broadcastLocationToggle.setOnClickListener(new CompoundButton.OnClickListener() {

            @Override
            public void onClick(View buttonView) {

                Intent intent = new Intent(context, CirclePushService.class);
                intent.putExtra(getResources().getString(R.string.EXTRA_BROADCAST_LOCATION_USER_ID), appState.getActiveUser().getId());
                intent.putExtra(getResources().getString(R.string.EXTRA_BROADCAST_LOCATION_CIRCLE_ID), appState.getActiveCircle().getId());

                // enabling broadcast location
                if (broadcastLocationToggle.isChecked()) {
                    DateTime now = new DateTime();

                    // if end hour - start hour is negative, add 24 to get the duration from current hour
                    // convert to 24-hour time
                    String amPm = endTimeAMPMPicker.getText().toString();
                    int endHour = Integer.parseInt(endTimePickerHour.getText().toString());
                    if (amPm.equals(getResources().getString(R.string.time_unit_after_noon)) && endHour != 12) {
                        endHour += 12;
                    }
                    else if (amPm.equals(getResources().getString(R.string.time_unit_before_noon)) && endHour == 12) {
                        endHour = 0;
                    }

                    int hourDiff = endHour - now.getHourOfDay();
                    if (hourDiff < 0) {
                        hourDiff += 24;
                    }
                    DateTime endTime = now.plusHours(hourDiff);

                    Duration durationFromNow = new Duration(now, endTime);
                    Long duration = durationFromNow.getMillis();

                    // tell all listeners to update their UI accordingly
                    if (broadcastLocationListeners != null) {
                        for (BroadcastLocationListener listener : broadcastLocationListeners) {
                            listener.onBroadcastLocationEnabled(duration);
                        }
                    }

                    // update DB telling it that this circle is broadcasting
                    Toast.makeText(context, "Broadcasting location updates to "
                            + appState.getActiveCircle().getTitle(), Toast.LENGTH_LONG).show();
                    appState.getActiveCircle().setBroadcastingLocation(true);

                    // update DB about broadcast location change to this circle
                    dataProvider.updateCircleLocationBroadcast(appState.getActiveCircle().getId(), true);

                    // start the push service, telling it to add the user's current circle to start broadcasting location to it
                    intent.putExtra(getResources().getString(R.string.EXTRA_BROADCAST_LOCATION_DURATION), duration);
                    intent.setAction(getResources().getString(R.string.ACTION_CIRCLE_ENABLE_LOCATION_BROADCAST));
                    startService(intent);
                }

                // disabling broadcast location
                else {
                    if (broadcastLocationListeners != null) {
                        for (BroadcastLocationListener listener : broadcastLocationListeners) {
                            listener.onBroadcastLocationDisabled();
                        }
                    }

                    // update DB telling it that this circle is no longer broadcasting
                    Toast.makeText(context, "Stopped broadcasting location updates to "
                            + appState.getActiveCircle().getTitle(), Toast.LENGTH_LONG).show();
                    appState.getActiveCircle().setBroadcastingLocation(false);

                    // update DB about broadcast location change to this cirlce
                    dataProvider.updateCircleLocationBroadcast(appState.getActiveCircle().getId(), false);

                    // informs the push service to remove the user's current circle to stop broadcasting location to it
                    intent.setAction(getResources().getString(R.string.ACTION_CIRCLE_DISABLE_LOCATION_BROADCAST));
                    startService(intent);
                }
            }
        });
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

    /**************************************************
     * Helpers
     **************************************************/

    @SuppressLint("InflateParams")
    protected void showLinkDialog(final LinkItem link) {
        View contentView = getLayoutInflater().inflate(R.layout.dialog_save_link, null);
        final EditText urlField = ((EditText) contentView.findViewById(R.id.input_link_url));
        final boolean shouldCreateNewLink = link == null;
        if (!shouldCreateNewLink) {
            urlField.setText(link.getUrl());
        }

        final AlertDialog dialog = new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setTitle(shouldCreateNewLink ? R.string.dialog_new_link_title : R.string.dialog_edit_link_title)
                .setView(contentView)
                .setPositiveButton(R.string.dialog_new_link_ok, null)
                .setNeutralButton(R.string.dialog_new_link_open_link, null)
                .setNegativeButton(R.string.dialog_new_link_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setCancelable(true)
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                try {
                    String url = TaskUtils.validateUrl(urlField.getText().toString());
                    if (url == null) {
                        urlField.setError(getString(R.string.warning_no_url));
                    }
                    else {
                        if (shouldCreateNewLink) {
                            dataProvider.createLinkAsync(appState.getActiveCircle(), DateTime.now(), url, true);
                        }
                        else {
                            dataProvider.updateLinkAsync(appState.getActiveCircle(), DateTime.now(), link.getId(), url, true);
                        }
                        dialog.dismiss();
                    }
                }
                catch (Exception ex) {
                    Log.e(TAG, ex.getMessage());
                }
            }
        });
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                try {
                    String url = TaskUtils.validateUrl(urlField.getText().toString());
                    if (url == null) {
                        urlField.setError(getString(R.string.warning_no_url));
                    }
                    else {
                        launchThirdPartyUrlApp(url);
                    }
                }
                catch (Exception ex) {
                    Log.e(TAG, ex.getMessage());
                }
            }
        });
    }

    protected void startDrawActivity() {
        String perm = Manifest.permission.WRITE_EXTERNAL_STORAGE;

        if (EasyPermissions.hasPermissions(this, perm)) {
            Intent intent = new Intent(this, DrawActivity.class);
            intent.setAction(getResources().getString(R.string.ACTION_CREATE_DRAWING));
            startActivityForResult(intent, Const.DRAW_RESULT_CODE);
        }
        else {
            EasyPermissions.requestPermissions(this, getString(R.string.permission_write_external_storage_rationale),
                    PERMISSION_WRITE_STORAGE, perm);
        }
    }

    protected void openImageBrowser() {
        String perm = Manifest.permission.READ_EXTERNAL_STORAGE;

        if (EasyPermissions.hasPermissions(this, perm)) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT)
                    .setType("image/*");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            }
            startActivityForResult(Intent.createChooser(intent,
                    getString(R.string.intent_select_images)), Const.IMAGE_SELECTED_RESULT_CODE);
        }
        else {
            EasyPermissions.requestPermissions(this, getString(R.string.permission_read_external_storage_rationale),
                    PERMISSION_READ_STORAGE, perm);
        }
    }

    protected void showBroadcastLocationMenuOptions() {
        String[] perms = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

        if (EasyPermissions.hasPermissions(this, perms)) {
            if (broadcastLocationSheetLayout.getParent() != null) {
                ((ViewGroup)broadcastLocationSheetLayout.getParent()).removeView(broadcastLocationSheetLayout);
            }

            broadcastLocationBottomSheet = new BottomSheetDialog(this);
            broadcastLocationBottomSheet.setContentView(broadcastLocationSheetLayout);
            BottomSheetBehavior behavior = BottomSheetBehavior.from((View) broadcastLocationSheetLayout.getParent());
            behavior.setPeekHeight(350);

            broadcastLocationBottomSheet.show();

            broadcastLocationBottomSheet.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    broadcastLocationBottomSheet = null;
                }
            });
        }
        else {
            EasyPermissions.requestPermissions(this, getString(R.string.permission_location_rationale),
                    PERMISSION_LOCATION, perms);
        }
    }

    protected void showBoardItemBottomSheet() {
        if (broadcastLocationBottomSheet != null) {
            broadcastLocationBottomSheet.dismiss();
            broadcastLocationBottomSheet = null;

            if (broadcastLocationSheetLayout.getParent() != null) {
                ((ViewGroup)broadcastLocationSheetLayout.getParent()).removeView(broadcastLocationSheetLayout);
            }
        }

        if (boardItemActionSheetLayout.getParent() != null) {
            ((ViewGroup)boardItemActionSheetLayout.getParent()).removeView(boardItemActionSheetLayout);
        }

        if (boardItemBottomSheet == null) {
            boardItemBottomSheet = new BottomSheetDialog(this);
            boardItemBottomSheet.setContentView(boardItemActionSheetLayout);

            BottomSheetBehavior behavior = BottomSheetBehavior.from((View) boardItemActionSheetLayout.getParent());
            behavior.setPeekHeight(550);

            boardItemBottomSheet.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    boardItemBottomSheet = null;
                }
            });
        }
        boardItemBottomSheet.show();
    }

    protected void handleMenuActionItem(MenuActionItem action) {
        switch(action) {
            case IMAGE: {
                openImageBrowser();
                break;
            }
            case DRAW: {
                startDrawActivity();
                break;
            }
            case LOCATION: {
                showBroadcastLocationMenuOptions();
                break;
            }
            case EVENT: {
                Intent intent = new Intent(this, EventActivity.class);
                intent.setAction(getResources().getString(R.string.ACTION_CREATE_EVENT));

                appState.setKeepGoogleApiClientAlive(true);
                startActivityForResult(intent, Const.CREATE_EVENT_RESULT_CODE);
                break;
            }
            case LINK: {
                showLinkDialog(null);
                break;
            }
            case NOTE: {
                Intent intent = new Intent(this, NoteActivity.class);
                intent.setAction(getString(R.string.ACTION_CREATE_NOTE));
                startActivityForResult(intent, Const.CREATE_NOTE_RESULT_CODE);
                break;
            }
            case LIST: {
                Intent intent = new Intent(this, ListItemActivity.class);
                intent.setAction(getString(R.string.ACTION_CREATE_LIST));
                startActivityForResult(intent, Const.CREATE_LIST_RESULT_CODE);
                break;
            }
            case ALARM:
            case VIDEO:
            default:  Toast.makeText(getApplicationContext(), "Operation is not yet supported, coming soon!", Toast.LENGTH_SHORT).show();
        }
    }

    /**************************************************
     * Board Item Actions
     **************************************************/

    @Override
    public void onItemClicked(MenuActionItem item) {
        if (boardItemBottomSheet != null) {
            boardItemBottomSheet.dismiss();

            handleMenuActionItem(item);
        }
    }

    protected void updateBroadcastLocationSheet() {
        broadcastLocationToggle.setChecked(appState.getActiveCircle().isUserBroadcastingLocation());
    }
}
