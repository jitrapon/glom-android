package com.abborg.glom.ui;

/**
 * Created by Boat on 23/8/58.
 */

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.abborg.glom.R;

/**
 * Profile mood fragment
 */
public class ProfileMoodFragment extends Fragment implements View.OnClickListener {

    public final static String USER = "com.abborg.com.abborg.glom.user";

    static ProfileMoodFragment instance;

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static ProfileMoodFragment getInstance() {
        if (instance == null) instance = new ProfileMoodFragment();
        return instance;
    }

    /**
     * Ctor
     */
    public ProfileMoodFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_profile_mood, container, false);
        return rootView;
    }

    @Override
    public void onClick(View v) {

    }
}