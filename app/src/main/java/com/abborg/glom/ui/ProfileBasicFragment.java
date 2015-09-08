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
import android.widget.Toast;

import com.abborg.glom.R;
import com.abborg.glom.model.User;

/**
 * A placeholder fragment containing a simple view.
 */
public class ProfileBasicFragment extends Fragment implements View.OnClickListener {

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

        mapButton = (Button) rootView.findViewById(R.id.buttonMap);
        mapButton.setOnClickListener(this);

        return rootView;
    }

    @Override
    public void onClick(View v) {
        //do what you want to do when button is clicked
        switch (v.getId()) {
            case R.id.buttonMap:
                User user = ((ProfileActivity) this.getActivity()).getUser();

                Toast.makeText(getActivity().getApplicationContext(), "Showing map...", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this.getActivity().getApplicationContext(), LocationActivity.class);
                intent.putExtra(getString(R.string.main_user_intent_key), user);
                startActivity(intent);
                break;
        }
    }
}
