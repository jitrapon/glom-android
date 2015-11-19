package com.abborg.glom.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.abborg.glom.AppState;
import com.abborg.glom.R;
import com.abborg.glom.model.Event;
import com.abborg.glom.model.FeedAction;
import com.abborg.glom.utils.CircleTransform;
import com.bumptech.glide.Glide;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Boat on 13/10/58.
 */
public class EventRecyclerViewAdapter
        extends RecyclerView.Adapter<EventRecyclerViewAdapter.EventHolder> {

    private static String TAG = "EventRecyclerViewAdapter";

    private List<Event> events;

    private static EventClickListener eventClickListener;

    private Context context;

    private List<Integer> staleEvents;

    public static class EventHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView posterAvatar;
        TextView posterName;
        TextView postTime;

        TextView eventName;
        TextView eventVenue;
        TextView eventNote;

        ImageView googleLogo;

        public EventHolder(View itemView) {
            super(itemView);

            posterAvatar = (ImageView) itemView.findViewById(R.id.cardUserAvatar);
            posterName = (TextView) itemView.findViewById(R.id.cardUserName);
            postTime = (TextView) itemView.findViewById(R.id.cardUserPostTime);

            eventName = (TextView) itemView.findViewById(R.id.cardEventName);
            eventVenue = (TextView) itemView.findViewById(R.id.cardEventVenue);
            eventNote = (TextView) itemView.findViewById(R.id.cardEventNote);

            googleLogo = (ImageView) itemView.findViewById(R.id.cardPoweredByGoogle);
        }

        @Override
        public void onClick(View v) {
            eventClickListener.onItemClick(getAdapterPosition(), v);
        }
    }

    public void setOnItemClickListener(EventClickListener clickListener) {
        this.eventClickListener = clickListener;
    }

    public EventRecyclerViewAdapter(Context context, List<Event> events) {
        this.context = context;
        this.events = events;
        staleEvents = new ArrayList<>();
    }

    public void update(List<Event> events) {
        // update from specific list of events
        if (events != null) {
            this.events = events;
            notifyDataSetChanged();
        }

        // TODO update everything from request
        else {
            for (int position : staleEvents) {
                notifyItemChanged(position);
            }
            staleEvents.clear();
        }
    }

    @Override
    public EventHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.event_card, parent, false);

        EventHolder eventHolder = new EventHolder(view);
        return eventHolder;
    }

    @Override
    public void onBindViewHolder(EventHolder holder, int position) {
        Event event = events.get(position);

        // update poster info
        FeedAction feedAction = event.getLastAction();
        if (feedAction != null) {
            if (feedAction.user != null) {

                // set update text
                switch(feedAction.type) {
                    case FeedAction.CREATE_EVENT:
                        holder.posterName.setText(Html.fromHtml("<b>" + feedAction.user.getName() + "</b> " +
                                context.getResources().getString(R.string.card_post_create_event)));
                        break;
                    case FeedAction.CANCEL_EVENT:
                        holder.posterName.setText(feedAction.user.getName() + " " + context.getResources().getString(R.string.card_post_cancel_event));
                        break;
                    default:
                        holder.posterName.setText(feedAction.user.getName());
                }

                // load avatar
                Glide.with(context)
                        .load(feedAction.user.getAvatar()).fitCenter()
                        .transform(new CircleTransform(context))
                        .override(context.getResources().getDimensionPixelSize(R.dimen.event_card_avatar_size),
                                context.getResources().getDimensionPixelSize(R.dimen.event_card_avatar_size))
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .crossFade(1000)
                        .into(holder.posterAvatar);
            }
            if (feedAction.dateTime != null) {
                DateTime now = new DateTime();
                Duration duration = new Duration(feedAction.dateTime, now);
                String displayTime = null;
                if (duration.getStandardSeconds() < 60)
                    displayTime = duration.getStandardSeconds() + " " + context.getResources().getString(R.string.time_unit_second);
                else if (duration.getStandardMinutes() < 60)
                    displayTime = duration.getStandardMinutes() + " " + context.getResources().getString(R.string.time_unit_minute);
                else if (duration.getStandardHours() < 24)
                    displayTime = duration.getStandardHours() + " " + context.getResources().getString(R.string.time_unit_hour);
                else
                    displayTime = duration.getStandardDays() + " " + context.getResources().getString(R.string.time_unit_day);

                holder.postTime.setText(displayTime);
            }
        }

        // update event info
        // always show time if available
        // if place is specified, show place, otherwise show coordinates
        holder.eventName.setText(event.getName());
        String time = "";
        String duration = "";
        if (event.getDateTime() != null) {
            DateTimeFormatter formatter = DateTimeFormat.forPattern(context.getResources().getString(R.string.card_event_datetime_format));
            DateTime now = new DateTime();
            Period period = new Period(now, event.getDateTime());

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

            time = formatter.print(event.getDateTime()) + " (" + duration + ")\n";
        }

        if (event.getPlace() != null) {

            // retrieve place from its ID
            final EventHolder viewHolder = holder;
            final String placeTime = time;
            String placeName = event.getPlace();
            GoogleApiClient apiClient = AppState.getInstance(context).getGoogleApiClient();
            if (apiClient != null && apiClient.isConnected()) {
                PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi.getPlaceById(apiClient, event.getPlace());
                placeResult.setResultCallback(new ResultCallback<PlaceBuffer>() {

                    @Override
                    public void onResult(PlaceBuffer places) {
                        if (!places.getStatus().isSuccess()) {
                            Log.e(TAG, "Place query did not complete. Error: " + places.getStatus().toString());
                            places.release();
                            return;
                        }

                        // display the first place in the list
                        final Place place = places.get(0);
                        viewHolder.eventVenue.setText(placeTime + Html.fromHtml(place.getName() + ""));
                        Log.d(TAG, "Place query succeeded for " + place.getName());

                        int updatedEventIndex = staleEvents.indexOf(viewHolder.getAdapterPosition());
                        if (updatedEventIndex >= 0 && updatedEventIndex < staleEvents.size())
                            staleEvents.remove(updatedEventIndex);

                        places.release();
                    }
                });
            }
            else {
                Log.e(TAG, "Google API client is not connected, error retrieving place info");
            }

            holder.eventVenue.setText(time + placeName);
            holder.googleLogo.setVisibility(View.VISIBLE);

            // add to list of events that contain places info to be refreshed
            staleEvents.add(holder.getAdapterPosition());
        }
        else {
            if (event.getLocation() != null) {
                holder.eventVenue.setText(time + event.getLocation().getLatitude() + ", " +
                        event.getLocation().getLongitude());
            }
            else {
                holder.eventVenue.setText(time);
            }

            holder.googleLogo.setVisibility(View.INVISIBLE);
        }
        holder.eventNote.setText(event.getNote());
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    public interface EventClickListener {
        void onItemClick(int position, View v);
    }
}
