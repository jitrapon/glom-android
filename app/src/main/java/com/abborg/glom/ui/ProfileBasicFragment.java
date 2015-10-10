package com.abborg.glom.ui;

/**
 * Created by Boat on 23/8/58.
 */

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.abborg.glom.R;
import com.abborg.glom.model.User;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

/**
 * A placeholder fragment containing a simple view.
 */
public class ProfileBasicFragment extends Fragment implements View.OnClickListener, OnMapReadyCallback, GoogleMap.OnMapClickListener {

    Button mapButton;
    static ProfileBasicFragment instance;

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static ProfileBasicFragment getInstance() {
        if (instance == null) instance = new ProfileBasicFragment();
//            Bundle args = new Bundle();
//            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
//            fragment.setArguments(args);
        return instance;
    }

    /**
     * Ctor
     */
    public ProfileBasicFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_profile_basic, container, false);

        // disable map fragment gestures
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map_fragment);
        mapFragment.getMapAsync(this);

        mapButton = (Button) rootView.findViewById(R.id.buttonMap);
        mapButton.setOnClickListener(this);

        TextView nameText = (TextView) rootView.findViewById(R.id.name_textView);
        ProfileActivity profileActivity = (ProfileActivity)getActivity();
        nameText.setText(profileActivity.getUser().getName());

        TextView idText = (TextView) rootView.findViewById(R.id.id_textView);
        idText.setText(profileActivity.getUser().getId());

        return rootView;
    }

    @Override
    public void onMapReady(GoogleMap map) {
        map.getUiSettings().setAllGesturesEnabled(false);
        map.setOnMapClickListener(this);
    }

    @Override
    public void onMapClick(LatLng location) {
        User user = ((ProfileActivity) this.getActivity()).getUser();

        Toast.makeText(getActivity().getApplicationContext(), "Showing map...", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this.getActivity().getApplicationContext(), LocationActivity.class);
        intent.putExtra(getString(R.string.EXTRA_CURRENT_USER), user);
        startActivity(intent);
    }

    @Override
    public void onClick(View v) {
        //do what you want to do when button is clicked
        switch (v.getId()) {
            case R.id.buttonMap:
                User user = ((ProfileActivity) this.getActivity()).getUser();

                Toast.makeText(getActivity().getApplicationContext(), "Showing map...", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this.getActivity().getApplicationContext(), LocationActivity.class);
                intent.putExtra(getString(R.string.EXTRA_CURRENT_USER), user);
                startActivity(intent);
                break;
        }
    }
}
