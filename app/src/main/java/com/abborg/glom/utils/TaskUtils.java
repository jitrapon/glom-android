package com.abborg.glom.utils;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import java.net.MalformedURLException;
import java.net.URL;
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


}
