package com.abborg.glom.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.util.LruCache;
import android.widget.Toast;

import com.abborg.glom.R;
import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Boat on 8/9/58.
 *
 * Singleton to encapsulate RequestQueue and other Volley functionality.
 * This class is responsible for handling all network requests.
 * Use this class with the Application context.
 */
public class RequestHandler {

    /* The singleton instance to be used throughout the app cycle */
    private static RequestHandler instance;

    /* Google RequestQueue instance */
    private RequestQueue requestQueue;

    /* The context of this RequestQueue */
    private static Context context;

    /* Volley's image loader */
    private ImageLoader imageLoader;

    /**
     * Ctor
     * @param context
     */
    private RequestHandler(Context context) {
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
    }

    /**
     * Call this generic response-handling method to handle response from server
     *
     * @param response
     */
    public void handleResponse(Context context, JSONObject response) {
        if (response != null) {
            try {
                String message = response.getString("message");
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            }
            catch (JSONException ex) {
                Toast.makeText(context,
                        "Error: " + ex.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Call this generic error-handling method to handle various connection errors from server
     *
     * @param error
     */
    public void handleError(VolleyError error) {
        if (error instanceof TimeoutError) {
            Toast.makeText(context,
                    context.getString(R.string.error_network_timeout),
                    Toast.LENGTH_LONG).show();
        }

        else if (error instanceof NoConnectionError) {
            Toast.makeText(context,
                    context.getString(R.string.error_no_connection),
                    Toast.LENGTH_LONG).show();
        }
        else if (error instanceof AuthFailureError) {
            Toast.makeText(context,
                    context.getString(R.string.error_auth_failure),
                    Toast.LENGTH_LONG).show();
        }
        else if (error instanceof ServerError) {
            Toast.makeText(context,
                    context.getString(R.string.error_server_generic),
                    Toast.LENGTH_LONG).show();
        }
        else if (error instanceof NetworkError) {
            Toast.makeText(context,
                    context.getString(R.string.error_network_generic),
                    Toast.LENGTH_LONG).show();
        }
        else if (error instanceof ParseError) {
            Toast.makeText(context,
                    context.getString(R.string.error_parse_generic),
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Returns the singleton instance of the RequestHandler
     * @param context The context in which the RequestQueue is to associated with. Use the application context.
     * @return The singleton instance
     */
    public static synchronized RequestHandler getInstance(Context context) {
        if (instance == null) {
            instance = new RequestHandler(context);
        }
        return instance;
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
        getRequestQueue().add(req);
    }

    /**
     * Get the image loader
     * @return The image loader
     */
    public ImageLoader getImageLoader() {
        return imageLoader;
    }
}
