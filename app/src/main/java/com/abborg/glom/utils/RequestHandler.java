package com.abborg.glom.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

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
