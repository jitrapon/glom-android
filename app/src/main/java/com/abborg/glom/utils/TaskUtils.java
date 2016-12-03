package com.abborg.glom.utils;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Any generic task that can be shared between classes are here
 *
 * Created by Jitrapon on 13/11/2559.
 */
public class TaskUtils {

    public static void launchGoogleMapsNavigation(Context context, double lat, double lng) {
        Uri gmmIntentUri = Uri.parse(
                String.format(Locale.ENGLISH, "google.navigation:q=%1f,%2f", lat, lng));
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        context.startActivity(mapIntent);
    }

    public static void copyToClipboard(Context context, String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(text, text);
        clipboard.setPrimaryClip(clip);
    }

    public static String validateUrl(String urlString) throws MalformedURLException {
        if (!TextUtils.isEmpty(urlString)) {
            urlString = urlString.trim();
            if (!urlString.toLowerCase().matches("^\\w+://.*")) {
                urlString = "http://" + urlString;
            }
            return new URL(urlString).toString();
        }
        return null;
    }

    public static void getLocationFromPlaceId(GoogleApiClient apiClient, String placeId, final OnLocationReceivedListener listener) {
        if (apiClient != null && apiClient.isConnected()) {
            PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi.getPlaceById(apiClient, placeId);
            placeResult.setResultCallback(new ResultCallback<PlaceBuffer>() {

                @Override
                public void onResult(@NonNull PlaceBuffer places) {
                    if (!places.getStatus().isSuccess()) {
                        places.release();
                        if (listener != null) {
                            listener.onLocationFailed();
                        }
                        return;
                    }

                    List<CharSequence> locations = new ArrayList<>();
                    for (int i = 0; i < places.getCount(); i++) {
                        locations.add(places.get(i).getName());
                    }
                    places.release();

                    if (listener != null) {
                        listener.onLocationReceived(locations);
                    }
                }
            });
        }
    }

    public interface OnLocationReceivedListener {
        void onLocationReceived(List<CharSequence> locations);
        void onLocationFailed();
    }
}
