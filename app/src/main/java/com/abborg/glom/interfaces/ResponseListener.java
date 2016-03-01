package com.abborg.glom.interfaces;

import com.android.volley.VolleyError;

import org.json.JSONObject;

public interface ResponseListener {

    void onSuccess(JSONObject response);

    void onError(VolleyError error);
}
