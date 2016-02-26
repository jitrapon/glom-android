package com.abborg.glom.utils;

import com.android.volley.VolleyError;

import org.json.JSONObject;

/**
 * Created by jitrapon on 25/2/16.
 */
public interface ResponseListener {

    void onSuccess(JSONObject response);

    void onError(VolleyError error);
}
