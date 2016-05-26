package com.abborg.glom.activities;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.database.SQLException;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;

import com.abborg.glom.AppState;
import com.abborg.glom.Const;
import com.abborg.glom.R;
import com.abborg.glom.data.DataUpdater;
import com.abborg.glom.model.BoardItem;
import com.abborg.glom.model.EventItem;
import com.abborg.glom.model.User;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.maps.android.SphericalUtil;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.MutableDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.List;

public class EventActivity extends AppCompatActivity {

    private AppState appState;

    public static final String TAG = "EventActivity";
    public static final String START_DATE_TAG = "CREATE_EVENT_START_DATE";
    public static final String END_DATE_TAG = "CREATE_EVENT_END_DATE";
    public static final String START_TIME_TAG = "CREATE_EVENT_START_TIME";
    public static final String END_TIME_TAG = "CREATE_EVENT_END_TIME";

    private DataUpdater dataUpdater;
    private TextInputLayout nameTextLayout;
    private EditText nameText;
    private EditText startDateText;
    private EditText startTimeText;
    private EditText endDateText;
    private EditText endTimeText;
    private DateTime startDateTime;
    private DateTime endDateTime;
    private String place;
    private Location location;
    private TextInputEditText locationText;
    private TextInputEditText noteText;

    /* The received intent, this should never be null because this activity is only launched from an intent */
    private Intent intent;

    /* Place API search radius */
    private static final double PLACE_SEARCH_RADIUS = 50 * 1000;

    private Mode mode;
    private EventItem editEvent;

    private enum Mode {
        CREATE,
        UPDATE,
        VIEW
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set up all variables
        intent = getIntent();
        if (intent == null) {
            finishWithResult(null); // end abruptly if we don't know what MODE we're in
        }
        appState = AppState.getInstance();
        dataUpdater = appState.getDataUpdater();
        startDateTime = null;
        endDateTime = null;

        // initialize the views
        setContentView(R.layout.activity_event);
        Toolbar toolbar = (Toolbar) findViewById(R.id.create_event_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_action_close);
        }

        final Button clearNameBtn = (Button) findViewById(R.id.clear_name_button);
        final Button clearStartDateBtn = (Button) findViewById(R.id.clear_start_date_button);
        final Button clearEndDateBtn = (Button) findViewById(R.id.clear_end_date_button);
        final Button clearLocationBtn = (Button) findViewById(R.id.clear_location_button);
        final Button pickPlaceBtn = (Button) findViewById(R.id.pick_place_button);
        nameText = (TextInputEditText) findViewById(R.id.input_event_name);
        nameTextLayout = (TextInputLayout) findViewById(R.id.input_layout_event_name);
        startDateText = (TextInputEditText) findViewById(R.id.input_event_start_date);
        startTimeText = (TextInputEditText) findViewById(R.id.input_event_start_time);
        endDateText = (TextInputEditText) findViewById(R.id.input_event_end_date);
        endTimeText = (TextInputEditText) findViewById(R.id.input_event_end_time);
        locationText = (TextInputEditText) findViewById(R.id.input_event_location);
        noteText = (TextInputEditText) findViewById(R.id.input_event_note);

        if (clearNameBtn != null) {
            clearNameBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (nameText != null) {
                        nameText.setText("");
                    }
                }
            });
        }
        if (nameText != null) {
            nameText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (clearNameBtn != null) {
                        if (s.length() > 0) {
                            clearNameBtn.setVisibility(View.VISIBLE);
                        }
                        else clearNameBtn.setVisibility(View.INVISIBLE);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        if (clearStartDateBtn != null) {
            clearStartDateBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (startDateText != null && startTimeText != null) {
                        startDateText.setText("");
                        startTimeText.setText("");
                    }
                    startDateTime = null;
                }
            });
        }
        if (startDateText != null && startTimeText != null) {
            startDateText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (clearStartDateBtn != null) {
                        if (s.length() > 0) {
                            clearStartDateBtn.setVisibility(View.VISIBLE);
                        }
                        else clearStartDateBtn.setVisibility(View.INVISIBLE);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            startTimeText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (clearStartDateBtn != null) {
                        if (s.length() > 0) {
                            clearStartDateBtn.setVisibility(View.VISIBLE);
                        }
                        else clearStartDateBtn.setVisibility(View.INVISIBLE);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        if (clearEndDateBtn != null) {
            clearEndDateBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (endDateText != null && endTimeText != null) {
                        endDateText.setText("");
                        endTimeText.setText("");
                    }
                    endDateTime = null;
                }
            });
        }
        if (endDateText != null && endTimeText != null) {
            endDateText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (clearEndDateBtn != null) {
                        if (s.length() > 0) {
                            clearEndDateBtn.setVisibility(View.VISIBLE);
                        }
                        else clearEndDateBtn.setVisibility(View.INVISIBLE);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            endTimeText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (clearEndDateBtn != null) {
                        if (s.length() > 0) {
                            clearEndDateBtn.setVisibility(View.VISIBLE);
                        }
                        else clearEndDateBtn.setVisibility(View.INVISIBLE);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        if (clearLocationBtn != null) {
            clearLocationBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (locationText != null) {
                        locationText.setText("");
                    }
                    place = null;
                    location = null;
                }
            });
        }
        if (locationText != null) {
            locationText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (clearLocationBtn != null) {
                        if (s.length() > 0) {
                            clearLocationBtn.setVisibility(View.VISIBLE);
                        }
                        else clearLocationBtn.setVisibility(View.INVISIBLE);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        if (pickPlaceBtn != null)
            pickPlaceBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showPlacePicker(v);
                }
            });

        // show datetime picker
        startDateText.setTag(START_DATE_TAG);
        startDateText.setFocusable(false);
        startDateText.setClickable(true);
        startDateText.setLongClickable(false);
        startDateText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDatePickerDialog(view);
            }
        });

        startTimeText.setTag(START_TIME_TAG);
        startTimeText.setFocusable(false);
        startTimeText.setClickable(true);
        startTimeText.setLongClickable(false);
        startTimeText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showTimePickerDialog(view);
            }
        });

        endDateText.setTag(END_DATE_TAG);
        endDateText.setFocusable(false);
        endDateText.setClickable(true);
        endDateText.setLongClickable(false);
        endDateText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDatePickerDialog(view);
            }
        });

        endTimeText.setTag(END_TIME_TAG);
        endTimeText.setFocusable(false);
        endTimeText.setClickable(true);
        endTimeText.setLongClickable(false);
        endTimeText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showTimePickerDialog(view);
            }
        });

        // set up Google Api auto-suggest places
//        locationText.setThreshold(3);                       // user has to type at least 3 characters for place suggestions to display
//        locationText.setOnItemClickListener(autocompleteClickListener);
//        placeArrayAdapter = new PlaceArrayAdapter(this, R.layout.simple_list_item, autocompleteBounds, autocompleteFilter);
//        locationText.setAdapter(placeArrayAdapter);
        locationText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPlaceAutoComplete(v);
            }
        });

        // based on the mode we receive in the intent, we further retrieve data and fill out the fields
        if (intent.getAction().equals(getResources().getString(R.string.ACTION_CREATE_EVENT)))
            mode = Mode.CREATE;
        else if (intent.getAction().equals(getResources().getString(R.string.ACTION_UPDATE_EVENT)))
            mode = Mode.UPDATE;
        else
            mode = Mode.VIEW;
        init(mode);
    }

    private void init(Mode mode) {
        switch (mode) {
            case CREATE:
                if (getSupportActionBar() != null) getSupportActionBar().setTitle(getResources().getString(R.string.title_activity_event));
                break;
            case UPDATE:
                List<BoardItem> items = appState.getActiveCircle().getItems();
                String id = intent.getStringExtra(getResources().getString(R.string.EXTRA_EVENT_ID));
                if (id != null && !items.isEmpty()) {
                    for (BoardItem item : items) {
                        if (item.getId().equals(id) && item.getType() == BoardItem.TYPE_EVENT) {
                            editEvent = (EventItem) item;
                            break;
                        }
                    }
                    if (editEvent != null) {
                        if (getSupportActionBar() != null)
                            getSupportActionBar().setTitle(getResources().getString(R.string.title_activity_event));
                        DateTimeFormatter printDateFormat = DateTimeFormat.forPattern(getResources().getString(R.string.display_date_format));
                        DateTimeFormatter printTimeFormat = DateTimeFormat.forPattern(getResources().getString(R.string.display_time_format));
                        DateTimeFormatter dayFormat = DateTimeFormat.forPattern("EEEE");
                        DateTime now = new DateTime();
                        String name = editEvent.getName();
                        startDateTime = editEvent.getStartTime();
                        endDateTime = editEvent.getEndTime();
                        place = editEvent.getPlace();
                        location = editEvent.getLocation();

                        nameText.setText(name);
                        if (startDateTime != null) {
                            int days = Days.daysBetween(now.withTimeAtStartOfDay(), startDateTime.withTimeAtStartOfDay()).getDays();
                            if (days == -1) {
                                startDateText.setText(getResources().getString(R.string.time_yesterday));
                            }
                            else if (days == 0) {
                                startDateText.setText(getResources().getString(R.string.time_today));
                            }
                            else if (days == 1) {
                                startDateText.setText(getResources().getString(R.string.time_tomorrow));
                            }
                            else if (days < 7 && days > 0) {
                                startDateText.setText(dayFormat.print(startDateTime));
                            }
                            else {
                                startDateText.setText(printDateFormat.print(startDateTime));
                            }

                            startTimeText.setText(printTimeFormat.print(startDateTime));
                        }
                        if (endDateTime != null) {
                            int days = Days.daysBetween(now.withTimeAtStartOfDay(), endDateTime.withTimeAtStartOfDay()).getDays();
                            if (days == -1) {
                                endDateText.setText(getResources().getString(R.string.time_yesterday));
                            }
                            else if (days == 0) {
                                endDateText.setText(getResources().getString(R.string.time_today));
                            }
                            else if (days == 1) {
                                endDateText.setText(getResources().getString(R.string.time_tomorrow));
                            }
                            else if (days < 7 && days > 0) {
                                endDateText.setText(dayFormat.print(endDateTime));
                            }
                            else {
                                endDateText.setText(printDateFormat.print(endDateTime));
                            }

                            endTimeText.setText(printTimeFormat.print(endDateTime));
                        }

                        if (place != null || location != null) {
                            if (place != null) {
                                if (location != null) {
                                    String displayText = location.getLatitude() + ", " + location.getLongitude();
                                    locationText.setText(displayText);
                                }
                                else {
                                    locationText.setText(getResources().getString(R.string.notify_retrieving_place_info));
                                }

                                PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                                        .getPlaceById(appState.getGoogleApiClient(), place);
                                placeResult.setResultCallback(new ResultCallback<PlaceBuffer>() {
                                    @Override
                                    public void onResult(@NonNull PlaceBuffer places) {
                                        if (!places.getStatus().isSuccess()) {
                                            Log.e(TAG, "Place query did not complete. Error: " +
                                                    places.getStatus().toString());
                                            places.release();
                                            return;
                                        }

                                        Place googlePlace = places.get(0);
                                        if (googlePlace != null) {
                                            locationText.setText(googlePlace.getName());
                                        }

                                        places.release();
                                    }
                                });
                            }
                            else {
                                String displayText = location.getLatitude() + ", " + location.getLongitude();
                                locationText.setText(displayText);
                            }
                        }

                        // set note
                        if (!TextUtils.isEmpty(editEvent.getNote())) {
                            noteText.setText(editEvent.getNote());
                        }
                    }
                }
                break;
            case VIEW:
                break;
            default: break;
        }
    }

    private void showPlaceAutoComplete(View view) {
        try {
            User user = appState.getActiveUser();
            Location userLocation = user.getLocation();
            LatLngBounds autocompleteBounds = null;
            if (userLocation != null) {
                LatLng latlng = new LatLng(userLocation.getLatitude(), userLocation.getLongitude());
                autocompleteBounds = new LatLngBounds.Builder().
                        include(SphericalUtil.computeOffset(latlng, PLACE_SEARCH_RADIUS, 0)).
                        include(SphericalUtil.computeOffset(latlng, PLACE_SEARCH_RADIUS, 90)).
                        include(SphericalUtil.computeOffset(latlng, PLACE_SEARCH_RADIUS, 180)).
                        include(SphericalUtil.computeOffset(latlng, PLACE_SEARCH_RADIUS, 270)).build();
            }

            Intent intent =
                    new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_OVERLAY)
                            .setFilter(null)
                            .setBoundsBias(autocompleteBounds)
                            .build(this);
            startActivityForResult(intent, Const.PLACE_AUTOCOMPLETE_REQUEST_CODE);
        }
        catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
            Log.e(TAG, e.getMessage());
            Toast.makeText(this, getString(R.string.error_google_play_services_error), Toast.LENGTH_LONG).show();
        }
    }

    private void showPlacePicker(View view) {
        try {
            PlacePicker.IntentBuilder intentBuilder =
                    new PlacePicker.IntentBuilder();
            Intent intent = intentBuilder.build(this);
            startActivityForResult(intent, Const.PLACE_PICKER_REQUEST_CODE);

        } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
            Log.e(TAG, e.getMessage());
            Toast.makeText(this, getString(R.string.error_google_play_services_error), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Const.PLACE_AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place result = PlaceAutocomplete.getPlace(this, data);
                Log.d(TAG, "Place: " + result.getName());
                locationText.setText(result.getName());
                place = result.getId();
                location = new Location("");
                location.setLatitude(result.getLatLng().latitude);
                location.setLongitude(result.getLatLng().longitude);
            }
            else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(this, data);
                Log.e(TAG, status.getStatusMessage());
            }
        }
        else if (requestCode == Const.PLACE_PICKER_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                final Place result = PlacePicker.getPlace(this, data);
                Log.d(TAG, "Place: " + result.getName());
                locationText.setText(result.getName());
                place = result.getId();
                location = new Location("");
                location.setLatitude(result.getLatLng().latitude);
                location.setLongitude(result.getLatLng().longitude);
            }
            else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(this, data);
                Log.e(TAG, status.getStatusMessage());
            }
        }
    }

    public void showTimePickerDialog(View view) {
        DialogFragment dialog = TimePickerFragment.newInstance(view);
        dialog.show(getSupportFragmentManager(), (String) view.getTag());
    }

    public void showDatePickerDialog(View view) {
        DialogFragment dialog = DatePickerFragment.newInstance(view);
        dialog.show(getSupportFragmentManager(), (String) view.getTag());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_event, menu);
        return true;
    }

    public void onStartDateSet(DateTime dateTime) {
        if (startDateTime != null) {
            MutableDateTime newDateTime = startDateTime.toMutableDateTime();
            newDateTime.setYear(dateTime.getYear());
            newDateTime.setMonthOfYear(dateTime.getMonthOfYear());
            newDateTime.setDayOfMonth(dateTime.getDayOfMonth());
            startDateTime = newDateTime.toDateTime();
        }
        else {
            startDateTime = dateTime;
        }

        Log.d("START DATETIME", appState.getDateTimeFormatter().print(startDateTime));
    }

    public void onStartTimeSet(DateTime dateTime) {
        if (startDateTime != null) {
            MutableDateTime newDateTime = startDateTime.toMutableDateTime();
            newDateTime.setHourOfDay(dateTime.getHourOfDay());
            newDateTime.setMinuteOfHour(dateTime.getMinuteOfHour());
            startDateTime = newDateTime.toDateTime();
        }
        else {
            startDateTime = dateTime;
        }

        Log.d("START DATETIME", appState.getDateTimeFormatter().print(startDateTime));
    }

    public void onEndDateSet(DateTime dateTime) {
        if (endDateTime != null) {
            MutableDateTime newDateTime = endDateTime.toMutableDateTime();
            newDateTime.setYear(dateTime.getYear());
            newDateTime.setMonthOfYear(dateTime.getMonthOfYear());
            newDateTime.setDayOfMonth(dateTime.getDayOfMonth());
            endDateTime = newDateTime.toDateTime();
        }
        else {
            endDateTime = dateTime;
        }

        Log.d("END DATETIME", appState.getDateTimeFormatter().print(endDateTime));
    }

    public void onEndTimeSet(DateTime dateTime) {
        if (endDateTime != null) {
            MutableDateTime newDateTime = endDateTime.toMutableDateTime();
            newDateTime.setHourOfDay(dateTime.getHourOfDay());
            newDateTime.setMinuteOfHour(dateTime.getMinuteOfHour());
            endDateTime = newDateTime.toDateTime();
        }
        else {
            endDateTime = dateTime;
        }

        Log.d("END DATETIME", appState.getDateTimeFormatter().print(endDateTime));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_event_done) {

            // verify that the event name is provided
            // verify that datetime is input correctly
            if (validateName() && validateDateTime()) {
                String note = noteText.getText() == null ? null : noteText.getText().toString();
                Intent intent = new Intent();
                long start = startDateTime == null ? 0L : startDateTime.getMillis();
                long end = endDateTime == null ? 0L : endDateTime.getMillis();

                //TODO handle the case where LOCATION is custom
                //TODO if the user has not picked a location from a LocationPicker or selected one from auto-suggest
                //TODO assume that the location is custom, alert the user to create a new place,
                //TODO picked a location instead, or continue without map knowing the place location.
                if (mode.equals(Mode.UPDATE)) {
                    if (editEvent != null) {
                        if (TextUtils.isEmpty(locationText.getText())) {
                            place = null;
                            location = null;
                            Log.d(TAG, "Location is empty");
                        }

                        intent.putExtra(getResources().getString(R.string.EXTRA_EVENT_ID), editEvent.getId());
                    }
                }

                intent.putExtra(getResources().getString(R.string.EXTRA_ITEM_CREATE_TIME), DateTime.now().getMillis());
                intent.putExtra(getResources().getString(R.string.EXTRA_EVENT_NAME), nameText.getText().toString());
                intent.putExtra(getResources().getString(R.string.EXTRA_EVENT_START_TIME), start);
                intent.putExtra(getResources().getString(R.string.EXTRA_EVENT_END_TIME), end);
                intent.putExtra(getResources().getString(R.string.EXTRA_EVENT_PLACE_ID), place);
                intent.putExtra(getResources().getString(R.string.EXTRA_EVENT_LOCATION), location);
                intent.putExtra(getResources().getString(R.string.EXTRA_EVENT_NOTE), note);

                // pass data to finishWithResult
                finishWithResult(intent);
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void finishWithResult(Intent intent) {
        if (intent != null) {
            setResult(RESULT_OK, intent);
        }
        else {
            setResult(RESULT_CANCELED);
        }

        finish();
    }

    private boolean validateName() {
        if (nameText.getText().toString().trim().isEmpty()) {
            nameTextLayout.setError(getString(R.string.error_event_name_empty));
            nameText.requestFocus();
            return false;
        }
        else {
            nameTextLayout.setError(null);
        }

        return true;
    }

    private boolean validateDateTime() {
        // must input both start date and time if one of them is provided
       return true;
    }

    public static class TimePickerFragment extends DialogFragment
            implements TimePickerDialog.OnTimeSetListener {

        private EditText editText;

        public static TimePickerFragment newInstance(View view) {
            TimePickerFragment fragment = new TimePickerFragment();
            fragment.editText = (EditText) view;
            return fragment;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current time as the default values for the picker
            DateTime dateTime = new DateTime();
            int hour = dateTime.getHourOfDay() + 1;
            int minute = dateTime.getMinuteOfHour();

            // Create a new instance of TimePickerDialog and return it
            return new TimePickerDialog(getActivity(), this, hour, minute, DateFormat.is24HourFormat(getActivity()));
        }

        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            DateTimeFormatter formatter = DateTimeFormat.forPattern("HH:mm");
            DateTimeFormatter printFormat = DateTimeFormat.forPattern(getResources().getString(R.string.display_time_format));
            DateTime dateTime = formatter.parseDateTime(hourOfDay + ":" + minute);

            if (editText.getTag().toString().equals(START_TIME_TAG)) {
                ((EventActivity)getActivity()).onStartTimeSet(dateTime);
            }
            else {
                ((EventActivity)getActivity()).onEndTimeSet(dateTime);
            }

            editText.setText(printFormat.print(dateTime));
        }
    }

    public static class DatePickerFragment extends DialogFragment
            implements DatePickerDialog.OnDateSetListener {

        private EditText editText;

        public static DatePickerFragment newInstance(View view) {
            DatePickerFragment fragment = new DatePickerFragment();
            fragment.editText = (EditText) view;
            return fragment;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current date as the default date in the picker
            DateTime dateTime = new DateTime();
            int year = dateTime.getYear();
            int month = dateTime.getMonthOfYear() - 1;
            int day = dateTime.getDayOfMonth();

            // Create a new instance of DatePickerDialog and return it
            return new DatePickerDialog(getActivity(), this, year, month, day);
        }

        public void onDateSet(DatePicker view, int year, int month, int day) {
            month += 1;
            DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy MM dd");
            DateTimeFormatter printFormat = DateTimeFormat.forPattern(getResources().getString(R.string.display_date_format));
            DateTimeFormatter dayFormat = DateTimeFormat.forPattern("EEEE");
            DateTime dateTime = formatter.parseDateTime(year + " " + month + " " + day);

            if (editText.getTag().toString().equals(START_DATE_TAG)) {
                ((EventActivity)getActivity()).onStartDateSet(dateTime);
            }
            else {
                ((EventActivity)getActivity()).onEndDateSet(dateTime);
            }

            DateTime now = new DateTime();
            int days = Days.daysBetween(now.withTimeAtStartOfDay(), dateTime.withTimeAtStartOfDay()).getDays();

            if (days == -1) {
                editText.setText(getResources().getString(R.string.time_yesterday));
            }
            else if (days == 0) {
                editText.setText(getResources().getString(R.string.time_today));
            }
            else if (days == 1) {
                editText.setText(getResources().getString(R.string.time_tomorrow));
            }
            else if (days < 7 && days > 0) {
                editText.setText(dayFormat.print(dateTime));
            }
            else {
                editText.setText(printFormat.print(dateTime));
            }
        }
    }

    @Override
    protected void onResume() {
        try {
            dataUpdater.open();
        }
        catch (SQLException ex) {
            Log.e(TAG, ex.getMessage());
        }

        // make sure google api client for place api is connected
        appState.connectGoogleApiClient();

        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

}
