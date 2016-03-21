package com.abborg.glom.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.abborg.glom.AppState;
import com.abborg.glom.Const;
import com.abborg.glom.R;
import com.abborg.glom.activities.MainActivity;
import com.abborg.glom.data.DataUpdater;
import com.google.android.gms.gcm.GcmListenerService;

/**
 * Created by Boat on 13/9/58.
 *
 * http://stackoverflow.com/questions/32137660/android-gcm-duplicate-push-after-notification-dismiss
 * If a notification is not consumed, and the app is closed (in the recent app), the notification will arrive again
 */
public class MessageListenerService extends GcmListenerService {

    private static final String TAG = "GcmListenerService";

    private static final int LOCATION_UPDATE = 0;

    @Override
    public void onMessageSent(String messageId) {
        Log.d(TAG, "Message is sent with id " + messageId);
    }

    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(String from, Bundle data) {
        Log.d(TAG, "Data received: " + data);
        String message = data.getString(Const.JSON_SERVER_MESSAGE);
        Log.d(TAG, "Message: " + message);

        /**
         * Production applications would usually process the message here.
         * Eg: - Syncing with server.
         *     - Store message in local database.
         *     - Update UI.
         */
        String opCodeString = data.getString(Const.JSON_SERVER_OP);
        if (opCodeString != null) {
            try {
                int opCode = Integer.parseInt(opCodeString);
                Log.d(TAG, "Message of type " + opCode + " received");

                //TODO keep track of received message of user and circleId
                //TODO store it in USERS table under column notification
                switch(opCode) {

                    // MESSAGE TYPE 1: Location updates in circle
                    case LOCATION_UPDATE:
                        Intent locUpdateIntent = new Intent(getResources().getString(R.string.ACTION_RECEIVE_LOCATION));

                        // save updated location in DB
                        AppState appState = AppState.getInstance();
                        DataUpdater dataUpdater;
                        String currentUserId = null;
                        if (appState != null) {
                            currentUserId = appState.getActiveUser().getId();
                            dataUpdater = appState.getDataUpdater();
                        }
                        else dataUpdater = DataUpdater.init(this);
                        dataUpdater.open();
                        dataUpdater.onLocationUpdateReceived(data, currentUserId);

                        locUpdateIntent.putExtra(getResources().getString(R.string.EXTRA_RECEIVE_LOCATION_USERS) , data.getString(Const.JSON_SERVER_USERIDS));
                        locUpdateIntent.putExtra(getResources().getString(R.string.EXTRA_RECEIVE_LOCATION_CIRCLE_ID), data.getString(Const.JSON_SERVER_CIRCLEID));
                        LocalBroadcastManager.getInstance(this).sendBroadcast(locUpdateIntent);

                        sendNotification(message);
                        break;

                    default:
                        // do nothing for now
                        Log.e(TAG, "Unsupported opcode received, do nothing for now!");
                }
            }
            catch (NumberFormatException ex) {
                Log.e(TAG, ex.getMessage());
            }
        }

        if (from.startsWith("/topics/")) {
            // message received from some topic.
        } else {
            // normal downstream message.
        }

        // [END_EXCLUDE]
    }
    // [END receive_message]

    /**
     * Create and show a simple notification containing the received GCM message.
     * TODO Send notification with different intents
     *
     * @param message GCM message received.
     */
    private void sendNotification(String message) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, Const.NOTIFY_LOCATION_UPDATE, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_action_alarm)
                .setContentTitle(getResources().getString(R.string.notification_title_location_update))
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(Const.NOTIFY_LOCATION_UPDATE, notificationBuilder.build());
    }
}
