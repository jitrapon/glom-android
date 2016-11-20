package com.abborg.glom.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.util.LruCache;
import android.util.Log;

import com.abborg.glom.ApplicationState;
import com.abborg.glom.Const;
import com.abborg.glom.R;
import com.abborg.glom.di.ComponentInjector;
import com.abborg.glom.interfaces.ResponseListener;
import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

/**
 * Created by Boat on 8/9/58.
 *
 * Singleton to encapsulate RequestQueue and other Volley functionality.
 * This class is responsible for handling all network requests.
 * Use this class with the Application context.
 */
public class HttpClient {

    @Inject
    ApplicationState appState;

    /* Google RequestQueue instance */
    private RequestQueue requestQueue;

    /* The context of this RequestQueue */
    private Context context;

    /* Volley's image loader */
    private ImageLoader imageLoader;

    private static final String TAG = "Network";

    public HttpClient(Context context) {
        ComponentInjector.INSTANCE.getApplicationComponent().inject(this);
        this.context = context;
        requestQueue = getRequestQueue();

        imageLoader = new ImageLoader(requestQueue,
        new ImageLoader.ImageCache() {
            private final LruCache<String, Bitmap>
                    cache = new LruCache<String, Bitmap>(20);

            @Override
            public Bitmap getBitmap(String url) {
                return cache.get(url);
            }

            @Override
            public void putBitmap(String url, Bitmap bitmap) {
                cache.put(url, bitmap);
            }
        });

        VolleyLog.setTag(TAG);
    }

    /**
     * Call this generic error-handling method to handle various connection errors from server
     * Returns true if it is a connectivity issue
     */
    public boolean handleError(VolleyError error) {
        if (error instanceof TimeoutError) {
            Log.e(TAG, context.getString(R.string.error_network_timeout));
            return true;
        }

        else if (error instanceof NoConnectionError) {
            Log.e(TAG, context.getString(R.string.error_no_connection));
            return true;
        }
        else if (error instanceof AuthFailureError) {
            Log.e(TAG, context.getString(R.string.error_auth_failure));
        }
        else if (error instanceof ServerError) {
            Log.e(TAG, context.getString(R.string.error_server_generic));
        }
        else if (error instanceof NetworkError) {
            Log.e(TAG, context.getString(R.string.error_network_generic));
            return true;
        }
        else if (error instanceof ParseError) {
            Log.e(TAG, context.getString(R.string.error_parse_generic));
        }

        return false;
    }

    /**
     * Get the underlying RequestQueue object
     * @return The RequestQueue object
     */
    public RequestQueue getRequestQueue() {
        if (requestQueue == null) {
            // getApplicationContext() is key, it keeps you from leaking the
            // Activity or BroadcastReceiver if someone passes one in.
            // also, there is no need to call start() since this will call start() automatically
            requestQueue = Volley.newRequestQueue(context.getApplicationContext());
        }
        return requestQueue;
    }

    /**
     * Add a request to the list of request queue
     * Once added, the request queue will start sending the request
     *
     * @param req The request to add
     * @param <T> Type of request
     */
    public <T> void addToRequestQueue(Request<T> req) {
        VolleyLog.d("--> " + getMethodName(req) + " " + req.getUrl() + " (" + req.getTag() + ")");
        getRequestQueue().add(req);
    }

    private String getMethodName(Request req) {
        switch (req.getMethod()) {
            case Request.Method.GET: return "GET";
            case Request.Method.DELETE: return "DELETE";
            case Request.Method.HEAD: return "HEAD";
            case Request.Method.OPTIONS: return "OPTIONS";
            case Request.Method.PATCH: return "PATCH";
            case Request.Method.POST: return "POST";
            case Request.Method.PUT: return "PUT";
            case Request.Method.TRACE: return "TRACE";
            default: return "UNKNOWN METHOD";
        }
    }

    /**
     * Get the image loader
     * @return The image loader
     */
    public ImageLoader getImageLoader() {
        return imageLoader;
    }

    private String getStatusText(int code) {
        switch (code) {
            case HttpURLConnection.HTTP_ACCEPTED: return "ACCEPTED";
            case HttpURLConnection.HTTP_BAD_GATEWAY: return "BAD GATEWAY";
            case HttpURLConnection.HTTP_BAD_METHOD: return "BAD METHOD";
            case HttpURLConnection.HTTP_BAD_REQUEST: return "BAD REQUEST";
            case HttpURLConnection.HTTP_CLIENT_TIMEOUT: return "CLIENT TIMEOUT";
            case HttpURLConnection.HTTP_CONFLICT: return "CONFLICT";
            case HttpURLConnection.HTTP_CREATED: return "CREATED";
            case HttpURLConnection.HTTP_ENTITY_TOO_LARGE: return "ENTITY TOO LARGE";
            case HttpURLConnection.HTTP_FORBIDDEN: return "FORBIDDEN";
            case HttpURLConnection.HTTP_GATEWAY_TIMEOUT: return "GATEWAY TIMEOUT";
            case HttpURLConnection.HTTP_GONE: return "GONE";
            case HttpURLConnection.HTTP_INTERNAL_ERROR: return "SERVER INTERNAL ERROR";
            case HttpURLConnection.HTTP_LENGTH_REQUIRED: return "LENGTH REQUIRED";
            case HttpURLConnection.HTTP_MOVED_PERM: return "MOVED PERM";
            case HttpURLConnection.HTTP_MOVED_TEMP: return "MOVED TEMP";
            case HttpURLConnection.HTTP_MULT_CHOICE: return "MULT CHOICE";
            case HttpURLConnection.HTTP_NOT_ACCEPTABLE: return "NOT ACCEPTABLE";
            case HttpURLConnection.HTTP_NOT_AUTHORITATIVE: return "NOT AUTHORITATIVE";
            case HttpURLConnection.HTTP_NOT_FOUND: return "NOT FOUND";
            case HttpURLConnection.HTTP_NOT_IMPLEMENTED: return "NOT IMPLEMENTED";
            case HttpURLConnection.HTTP_NOT_MODIFIED: return "NOT MODIFIED";
            case HttpURLConnection.HTTP_NO_CONTENT: return "NO CONTENT";
            case HttpURLConnection.HTTP_OK: return "OK";
            case HttpURLConnection.HTTP_PARTIAL: return "PARTIAL";
            case HttpURLConnection.HTTP_PAYMENT_REQUIRED: return "PAYMENT REQUIRED";
            case HttpURLConnection.HTTP_PRECON_FAILED: return "PRECON FAILED";
            case HttpURLConnection.HTTP_PROXY_AUTH: return "PROXY AUTH";
            case HttpURLConnection.HTTP_REQ_TOO_LONG: return "REQUEST TOO LONG";
            case HttpURLConnection.HTTP_RESET: return "RESET";
            case HttpURLConnection.HTTP_SEE_OTHER: return "SEE OTHER";
            case HttpURLConnection.HTTP_UNAUTHORIZED: return "UNAUTHORIZED";
            case HttpURLConnection.HTTP_UNAVAILABLE: return "UNAVAILABLE";
            case HttpURLConnection.HTTP_UNSUPPORTED_TYPE: return "UNSUPPORTED TYPE";
            case HttpURLConnection.HTTP_USE_PROXY: return "USE PROXY";
            case HttpURLConnection.HTTP_VERSION: return "VERSION NOT SUPPORTED";
            default: return "UNKNOWN";
        }
    }

    public void get(String tag, String endpoint, final ResponseListener listener) {
        String baseUrl = Const.HOST_ADDRESS;
        String url;
        if (baseUrl.endsWith("/") && endpoint.startsWith("/"))
            url = baseUrl.substring(0, baseUrl.length()-1).concat(endpoint);
        else if (!endpoint.endsWith("/") && !endpoint.startsWith("/"))
            url = baseUrl.concat("/").concat(endpoint);
        else url = baseUrl.concat(endpoint);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        if (listener != null) listener.onSuccess(response);
                    }
                },
                new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (listener != null) listener.onError(error);
                    }
                })

        {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put(Const.API_AUTHORIZATION_HEADER, Const.TEST_API_AUTHORIZATION_HEADER);
                headers.put(Const.API_USERID_HEADER, appState.getActiveUser().getId());
                return headers;
            }

            @Override
            protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
                int status = response.statusCode;
                VolleyLog.d("<-- " + status + " " + getStatusText(status) + " (" + response.networkTimeMs + "ms, " +
                        response.data.length + "-byte body)");
                return super.parseNetworkResponse(response);
            }
        };

        request.setTag(tag);
        addToRequestQueue(request);
    }

    /**
     * Convenience method for making a POST request using the HOST_ADDRESS and specifying body and endpoint
     */
    public void post(String tag, String endpoint, JSONObject body, final ResponseListener listener) {
        String baseUrl = Const.HOST_ADDRESS;
        String url;
        if (baseUrl.endsWith("/") && endpoint.startsWith("/"))
            url = baseUrl.substring(0, baseUrl.length()-1).concat(endpoint);
        else if (!endpoint.endsWith("/") && !endpoint.startsWith("/"))
            url = baseUrl.concat("/").concat(endpoint);
        else url = baseUrl.concat(endpoint);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, body,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        if (listener != null) listener.onSuccess(response);
                    }
                },
                new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (listener != null) listener.onError(error);
                    }
                })

        {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put(Const.API_AUTHORIZATION_HEADER, Const.TEST_API_AUTHORIZATION_HEADER);
                headers.put(Const.API_USERID_HEADER, appState.getActiveUser().getId());
                return headers;
            }

            @Override
            protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
                int status = response.statusCode;
                VolleyLog.d("<-- " + status + " " + getStatusText(status) + " (" + response.networkTimeMs + "ms, " +
                        response.data.length + "-byte body)");
                return super.parseNetworkResponse(response);
            }
        };

        request.setTag(tag);
        addToRequestQueue(request);
    }

    /**
     * Convenience method for making a DELETE request
     */
    public void delete(String tag, String endpoint, JSONObject body, final ResponseListener listener) {
        String baseUrl = Const.HOST_ADDRESS;
        String url;
        if (baseUrl.endsWith("/") && endpoint.startsWith("/"))
            url = baseUrl.substring(0, baseUrl.length() - 1).concat(endpoint);
        else if (!endpoint.endsWith("/") && !endpoint.startsWith("/"))
            url = baseUrl.concat("/").concat(endpoint);
        else url = baseUrl.concat(endpoint);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.DELETE, url, body,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        if (listener != null) listener.onSuccess(response);
                    }
                },
                new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (listener != null) listener.onError(error);
                    }
                })

        {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put(Const.API_AUTHORIZATION_HEADER, Const.TEST_API_AUTHORIZATION_HEADER);
                headers.put(Const.API_USERID_HEADER, appState.getActiveUser().getId());
                return headers;
            }

            @Override
            protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
                int status = response.statusCode;
                VolleyLog.d("<-- " + status + " " + getStatusText(status) + " (" + response.networkTimeMs + "ms, " +
                        response.data.length + "-byte body)");
                return super.parseNetworkResponse(response);
            }
        };

        request.setTag(tag);
        addToRequestQueue(request);
    }
}
