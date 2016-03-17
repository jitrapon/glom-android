package com.abborg.glom.activities;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.database.SQLException;
import android.location.Location;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;

import com.abborg.glom.AppState;
import com.abborg.glom.R;
import com.abborg.glom.adapters.PlaceArrayAdapter;
import com.abborg.glom.data.DataUpdater;
import com.abborg.glom.model.BoardItem;
import com.abborg.glom.model.Event;
import com.abborg.glom.model.User;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
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

    private AutoCompleteTextView locationText;

    private PlaceArrayAdapter placeArrayAdapter;

    /* biasing the results of autocomplete places to a specific area specified by latitude and longitude bounds */
    private LatLngBounds autocompleteBounds;

    /* containing a set of place types, which can be used to restrict the results to one or more types of place. */
    private AutocompleteFilter autocompleteFilter;

    /* The received intent, this should never be null because this activity is only launched from an intent */
    private Intent intent;

    /* Place API search radius */
    private static final double PLACE_SEARCH_RADIUS = 50 * 1000;

    private Mode mode;

    private Event editEvent;

    private enum Mode {
        CREATE_EVENT,   // this mode tells the activity on creation that all fields are to remain blank
        UPDATE_EVENT,   // this mode tells the activity on creation that all fields are retrieved from a created event and can be modified
        VIEW_EVENT      // this mode tells the activity on creation that all fields are retrieved from a created event, but cannot be modified
    }

    private AdapterView.OnItemClickListener autocompleteClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final PlaceArrayAdapter.PlaceAutocomplete item = placeArrayAdapter.getItem(position);
            final String placeId = String.valueOf(item.placeId);
            Log.d(TAG, "Selected " + item.description);
            PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                    .getPlaceById(appState.getGoogleApiClient(), placeId);
            placeResult.setResultCallback(updatePlaceDetailsCallback);
            Log.d(TAG, "Fetching details for ID: " + item.placeId);
        }
    };

    private ResultCallback<PlaceBuffer> updatePlaceDetailsCallback
            = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(PlaceBuffer places) {
            if (!places.getStatus().isSuccess()) {
                Log.e(TAG, "Place query did not complete. Error: " +
                        places.getStatus().toString());
                places.release();
                return;
            }
            // Selecting the first object buffer.
            Place googlePlace = places.get(0);
            if (googlePlace != null) {
                place = googlePlace.getId();
                location = new Location("");
                double lat = googlePlace.getLatLng().latitude;
                double lng = googlePlace.getLatLng().longitude;
                location.setLatitude(lat);
                location.setLongitude(lng);
                Log.d(TAG, "Saving place (" + place + ") with LatLng: " + lat + ", " + lng);
            }

            places.release();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        intent = getIntent();
        if (intent == null) {
            finishWithResult(null); // end abruptly if we don't know what MODE we're in
        }

        appState = AppState.getInstance();
        dataUpdater = appState.getDataUpdater();
        startDateTime = null;
        endDateTime = null;

        setContentView(R.layout.activity_event);
        Toolbar toolbar = (Toolbar) findViewById(R.id.create_event_toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_action_close);

        nameText = (EditText) findViewById(R.id.input_event_name);
        nameTextLayout = (TextInputLayout) findViewById(R.id.input_layout_event_name);

        // show datetime picker
        startDateText = (EditText) findViewById(R.id.input_event_start_date);
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

        startTimeText = (EditText) findViewById(R.id.input_event_start_time);
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

        endDateText = (EditText) findViewById(R.id.input_event_end_date);
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

        endTimeText = (EditText) findViewById(R.id.input_event_end_time);
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
        locationText = (AutoCompleteTextView) findViewById(R.id.input_event_location);
        locationText.setThreshold(3);   // user has to type at least 3 characters for place suggestions to display
        List<User> users = appState.getActiveCircle().getUsers();
        Location userLocation = null;
        for (User user : users) {
            if (user.getId().equals(appState.getActiveUser().getId())) {
                userLocation = user.getLocation();
            }
        }
        if (userLocation != null) {
            LatLng latlng = new LatLng(userLocation.getLatitude(), userLocation.getLongitude());
            autocompleteBounds = new LatLngBounds.Builder().
                    include(SphericalUtil.computeOffset(latlng, PLACE_SEARCH_RADIUS, 0)).
                    include(SphericalUtil.computeOffset(latlng, PLACE_SEARCH_RADIUS, 90)).
                    include(SphericalUtil.computeOffset(latlng, PLACE_SEARCH_RADIUS, 180)).
                    include(SphericalUtil.computeOffset(latlng, PLACE_SEARCH_RADIUS, 270)).build();
        }
        autocompleteFilter = null;
        locationText.setOnItemClickListener(autocompleteClickListener);
        placeArrayAdapter = new PlaceArrayAdapter(this, R.layout.simple_list_item, autocompleteBounds, autocompleteFilter);
        locationText.setAdapter(placeArrayAdapter);

        // based on the mode we receive in the intent, we further retrieve data and fill out the fields
        if (intent.getAction().equals(getResources().getString(R.string.ACTION_CREATE_EVENT)))
            mode = Mode.CREATE_EVENT;
        else if (intent.getAction().equals(getResources().getString(R.string.ACTION_UPDATE_EVENT)))
            mode = Mode.UPDATE_EVENT;
        else
            mode = Mode.VIEW_EVENT;
        init(mode);
    }

    private void init(Mode mode) {
        switch (mode) {
            case CREATE_EVENT:
                if (getSupportActionBar() != null) getSupportActionBar().setTitle(getResources().getString(R.string.create_event_title));
                break;
            case UPDATE_EVENT:
                //TODO contact server to get more information
                // only display information stored in DB, which is name, time, and place
                List<BoardItem> items = appState.getActiveCircle().getItems();
                String id = intent.getStringExtra(getResources().getString(R.string.EXTRA_EVENT_ID));
                if (id != null && !items.isEmpty()) {
                    for (BoardItem item : items) {
                        if (item.getId().equals(id) && item.getType() == BoardItem.TYPE_EVENT) {
                            editEvent = (Event) item;
                            break;
                        }
                    }
                    if (editEvent != null) {
                        if (getSupportActionBar() != null) getSupportActionBar().setTitle(editEvent.getName());
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
                                    locationText.setText(location.getLatitude() + ", " + location.getLongitude());
                                }
                                else {
                                    locationText.setText(getResources().getString(R.string.notify_retrieving_place_info));
                                }

                                PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                                        .getPlaceById(appState.getGoogleApiClient(), place);
                                placeResult.setResultCallback(new ResultCallback<PlaceBuffer>() {
                                    @Override
                                    public void onResult(PlaceBuffer places) {
                                        if (!places.getStatus().isSuccess()) {
                                            Log.e(TAG, "Place query did not complete. Error: " +
                                                    places.getStatus().toString());
                                            places.release();
                                            return;
                                        }
                                        // Selecting the first object buffer.
                                        Place googlePlace = places.get(0);
                                        if (googlePlace != null) {
                                            locationText.setText(googlePlace.getName());
                                        }

                                        places.release();
                                    }
                                });
                            }
                            else {
                                locationText.setText(location.getLatitude() + ", " + location.getLongitude());
                            }
                        }
                    }
                }
                break;
            case VIEW_EVENT:
                break;
            default: break;
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
                dataUpdater.open();

                if (mode.equals(Mode.CREATE_EVENT)) {
                    editEvent = dataUpdater.createEvent(appState.getActiveCircle(), null, null, nameText.getText().toString(), startDateTime,
                            endDateTime, place, location, null, true);
                }
                else if (mode.equals(Mode.UPDATE_EVENT)) {
                    if (editEvent != null) {
                        //TODO handle the case where LOCATION is custom
                        //TODO if the user has not picked a location from a LocationPicker or selected one from auto-suggest
                        //TODO assume that the location is custom, alert the user to create a new place,
                        //TODO picked a location instead, or continue without map knowing the place location.
                        if (TextUtils.isEmpty(locationText.getText())) {
                            place = null;
                            location = null;
                            Log.d(TAG, "Location is empty");
                        }

                        editEvent = dataUpdater.updateEvent(appState.getActiveCircle(), null, editEvent.getId(), nameText.getText().toString(),
                               startDateTime, endDateTime, place, location, null, true);
                    }
                }

                // pass data to finishWithResult
                finishWithResult(editEvent.getId());
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void finishWithResult(String id) {
        if (id != null) {
            Intent intent = new Intent();
            intent.putExtra(getResources().getString(R.string.EXTRA_EVENT_ID), id);
            setResult(RESULT_OK, intent);
            Log.d(TAG, "finishing with result " + RESULT_OK + " and id of " + id);
        }
        else {
            setResult(RESULT_CANCELED);
            Log.d(TAG, "finishing with result " + RESULT_CANCELED);
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
            nameTextLayout.setErrorEnabled(false);
        }

        return true;
    }

    private boolean validateDateTime() {
        // must input both start date and time if one of them is provided
       return true;
    }

    public static class TimePickerFragment extends DialogFragment
            implements TimePickerDialog.OnTimeSetListener {

        private static final String TAG = "TIMEPICKER";

        private EditText editText;

        public static final TimePickerFragment newInstance(View view) {
            TimePickerFragment fragment = new TimePickerFragment();
            fragment.editText = (EditText) view;
            return fragment;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current time as the default values for the picker
            DateTime dateTime = new DateTime();
            int hour = dateTime.getHourOfDay() + 1;
            int minute = dateTime.getMinuteOfHour();

            // Create a new instance of TimePickerDialog and return it
            return new TimePickerDialog(getActivity(), this, hour, minute, DateFormat.is24HourFormat(getActivity()));
        }

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

        private static final String TAG = "DATEPICKER";

        private EditText editText;

        public static final DatePickerFragment newInstance(View view) {
            DatePickerFragment fragment = new DatePickerFragment();
            fragment.editText = (EditText) view;
            return fragment;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current date as the default date in the picker
            DateTime dateTime = new DateTime();
            int year = dateTime.getYear();
            int month = dateTime.getMonthOfYear() - 1;
            int day = dateTime.getDayOfMonth();

            // Create a new instance of DatePickerDialog and return it
            DatePickerDialog datePickerDialog =  new DatePickerDialog(getActivity(), this, year, month, day);
            DatePicker datePicker = datePickerDialog.getDatePicker();
            datePicker.setMinDate(dateTime.getMillis());
            return datePickerDialog;
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
