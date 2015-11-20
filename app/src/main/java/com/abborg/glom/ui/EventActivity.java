package com.abborg.glom.ui;

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
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;

import com.abborg.glom.AppState;
import com.abborg.glom.R;
import com.abborg.glom.model.DataUpdater;
import com.abborg.glom.model.Event;
import com.abborg.glom.model.User;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.MutableDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Arrays;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        appState = AppState.getInstance(this);
        dataUpdater = appState.getDataUpdater();
        startDateTime = null;
        endDateTime = null;

        setContentView(R.layout.activity_event);
        Toolbar toolbar = (Toolbar) findViewById(R.id.create_event_toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getResources().getString(R.string.create_event_title));
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
        getMenuInflater().inflate(R.menu.menu_create_event, menu);
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

        if (id == R.id.action_create_event_done) {
            // verify that the event name is provided
            // verify that datetime is input correctly
            if (validateName() && validateDateTime()) {
                User user = appState.getUser();
//                String place = "ChIJB5FY5M2e4jARo48nbVRhgAo";
                String place = null;
                Location location = new Location("");
                location.setLatitude(1.29929);
                location.setLongitude(103.86286);
                dataUpdater.open();
                Event newEvent = dataUpdater.createEvent(nameText.getText().toString(), appState.getCurrentCircle(),
                        new ArrayList<>(Arrays.asList(user)), startDateTime, endDateTime, place, location, Event.IN_CIRCLE,
                        new ArrayList<User>(), true, true, true, null
                );

                // TODO notify server that an event has been added
                // pass data to finishWithResult()
                finishWithResult(newEvent.getId());
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void finishWithResult(String id) {
        Intent intent = new Intent();
        intent.putExtra(getResources().getString(R.string.EXTRA_CREATE_EVENT_ID), id);
        setResult(RESULT_OK, intent);
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

            if (days < 0) {
                Toast.makeText(getActivity(), getResources().getString(R.string.error_past_date_selected), Toast.LENGTH_SHORT).show();
            }
            else if (days == 0) {
                editText.setText(getResources().getString(R.string.time_today));
            }
            else if (days == 1) {
                editText.setText(getResources().getString(R.string.time_tomorrow));
            }
            else if (days < 7) {
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
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

}
