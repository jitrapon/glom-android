package com.abborg.glom.utils;

import android.content.Context;

import com.abborg.glom.R;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * Helper class that deals with date time.
 */
public class DateUtils {

    public static String getFormattedTimeFromNow(Context context, DateTime timeToCompare) {
        String duration;
        DateTime now = new DateTime();
        Period period = new Period(now, timeToCompare);

        int years = period.getYears() * -1;
        int months = period.getMonths() * -1;
        int weeks = period.getWeeks() * -1;
        int hours = period.getHours() * -1;
        int days = period.getDays() * -1;
        int minutes = period.getMinutes() * -1;

        // positive periods
        if (period.getYears() >= 1)
            duration = period.getYears() + " " + context.getResources().getString(R.string.time_unit_year) + " " +
                    context.getResources().getString(R.string.time_unit_and_seperator) + " " +
                    period.getMonths() + " " + context.getResources().getString(R.string.time_unit_month);
        else if (period.getMonths() >= 1)
            duration = period.getMonths() + " " + context.getResources().getString(R.string.time_unit_month) + " " +
                    context.getResources().getString(R.string.time_unit_and_seperator) + " " +
                    period.getDays() + " " + context.getResources().getString(R.string.time_unit_day);
        else if (period.getWeeks() >= 1)
            duration = period.getWeeks() + " " + context.getResources().getString(R.string.time_unit_week) + " " +
                    context.getResources().getString(R.string.time_unit_and_seperator) + " " +
                    period.getDays() + " " + context.getResources().getString(R.string.time_unit_day);
        else if (period.getDays() >= 1)
            duration = period.getDays() + " " + context.getResources().getString(R.string.time_unit_day) + " " +
                    context.getResources().getString(R.string.time_unit_and_seperator) + " " +
                    period.getHours() + " " + context.getResources().getString(R.string.time_unit_hour);
        else if (period.getHours() >= 1)
            duration = period.getHours() + " " + context.getResources().getString(R.string.time_unit_hour) + " " +
                    context.getResources().getString(R.string.time_unit_and_seperator) + " " +
                    period.getMinutes() + " " + context.getResources().getString(R.string.time_unit_minute);
        else if (period.getMinutes() >= 0)
            duration = period.getMinutes() + " " + context.getResources().getString(R.string.time_unit_minute);
            
        // negative periods (already passed)
        else if (period.getYears() <= -1) {
            duration = years + " " + context.getResources().getString(R.string.time_unit_year) + " " +
                    context.getResources().getString(R.string.time_unit_and_seperator) + " " +
                    months + " " + context.getResources().getString(R.string.time_unit_month) + " " +
                    context.getResources().getString(R.string.time_suffix_ago);
        }
        else if (period.getMonths() <= -1) {
            duration = months + " " + context.getResources().getString(R.string.time_unit_month) + " " +
                    context.getResources().getString(R.string.time_unit_and_seperator) + " " +
                    days + " " + context.getResources().getString(R.string.time_unit_day) + " " +
                    context.getResources().getString(R.string.time_suffix_ago);
        }
        else if (period.getWeeks() <= -1) {
            duration = weeks + " " + context.getResources().getString(R.string.time_unit_week) + " " +
                    context.getResources().getString(R.string.time_unit_and_seperator) + " " +
                    days + " " + context.getResources().getString(R.string.time_unit_day) + " " +
                    context.getResources().getString(R.string.time_suffix_ago);
        }
        else if (period.getDays() <= -1) {
            duration = days + " " + context.getResources().getString(R.string.time_unit_day) + " " +
                    context.getResources().getString(R.string.time_unit_and_seperator) + " " +
                    hours + " " + context.getResources().getString(R.string.time_unit_hour) + " " +
                    context.getResources().getString(R.string.time_suffix_ago);
        }
        else if (period.getHours() <= -1) {
            duration = hours + " " + context.getResources().getString(R.string.time_unit_hour) + " " +
                    context.getResources().getString(R.string.time_unit_and_seperator) + " " +
                    minutes + " " + context.getResources().getString(R.string.time_unit_minute) + " " +
                    context.getResources().getString(R.string.time_suffix_ago);
        }
        else {
            duration = minutes + " " + context.getResources().getString(R.string.time_unit_minute) + " " +
                    context.getResources().getString(R.string.time_suffix_ago);
        }

        return duration;
    }
    
    public static String getFormattedDate(Context context, DateTime startDate, DateTime endDate) {
        DateTimeFormatter formatter = DateTimeFormat.forPattern(context.getResources().getString(R.string.card_event_datetime_format));
        DateTimeFormatter timeFormatter = DateTimeFormat.forPattern(context.getResources().getString(R.string.card_event_time_format));
        DateTime now = new DateTime();
        String formattedStartDateTime;
        String formattedEndDateTime;

        if (startDate == null) {
            startDate = now;
        }
        int daysBetween = Days.daysBetween(now.withTimeAtStartOfDay(), startDate.withTimeAtStartOfDay()).getDays();
        DateTimeFormatter dayWithTimeFormatter = DateTimeFormat.forPattern("EEEE, " +
                context.getResources().getString(R.string.card_event_time_format));

        if (startDate.getMillis() == now.getMillis()) {
            formattedStartDateTime = context.getResources().getString(R.string.date_now);
        }
        else if (daysBetween == -1) {
            formattedStartDateTime = context.getResources().getString(R.string.time_yesterday)
                    + ", " + timeFormatter.print(startDate);
        }
        else if (daysBetween == 0) {
            formattedStartDateTime = context.getResources().getString(R.string.time_today)
                    + ", " + timeFormatter.print(startDate);
        }
        else if (daysBetween == 1) {
            formattedStartDateTime = context.getResources().getString(R.string.time_tomorrow)
                    + ", " + timeFormatter.print(startDate);
        }
        else if (daysBetween < 7 && daysBetween > 0) {
            formattedStartDateTime = dayWithTimeFormatter.print(startDate);
        }
        else {
            formattedStartDateTime = formatter.print(startDate);
        }

        if (endDate != null) {
            daysBetween = Days.daysBetween(now.withTimeAtStartOfDay(), endDate.withTimeAtStartOfDay()).getDays();
            int daysDuration = Days.daysBetween(startDate.withTimeAtStartOfDay(),
                    endDate.withTimeAtStartOfDay()).getDays();
            if (daysDuration == 0) {
                formattedEndDateTime = " - " + timeFormatter.print(endDate) + " ";
            }
            else {
                if (daysBetween == -1) {
                    formattedEndDateTime = " - " + context.getResources().getString(R.string.time_yesterday)
                            + ", " + timeFormatter.print(endDate) + " ";
                }
                else if (daysBetween == 0) {
                    formattedEndDateTime = " - " + context.getResources().getString(R.string.time_today)
                            + ", " + timeFormatter.print(endDate) + " ";
                }
                else if (daysBetween == 1) {
                    formattedEndDateTime = " - " + context.getResources().getString(R.string.time_tomorrow)
                            + ", " + timeFormatter.print(endDate) + " ";
                }
                else if (daysBetween < 7 && daysBetween > 0) {
                    formattedEndDateTime = " - " + dayWithTimeFormatter.print(endDate) + " ";
                }
                else {
                    formattedEndDateTime = " - " + formatter.print(endDate) + " ";
                }
            }
        }
        else {
            formattedEndDateTime = "";
        }
        
        return formattedStartDateTime + formattedEndDateTime;
    }
}
