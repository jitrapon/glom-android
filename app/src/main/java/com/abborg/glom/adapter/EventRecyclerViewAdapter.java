package com.abborg.glom.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.abborg.glom.AppState;
import com.abborg.glom.R;
import com.abborg.glom.model.Event;
import com.abborg.glom.model.FeedAction;
import com.abborg.glom.ui.RecyclerHeaderViewHolder;
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
        extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static String TAG = "EventRecyclerViewAdapter";

    private static final int TYPE_ITEM = 1;

    private static final int TYPE_HEADER = 2;

    private List<Event> events;

    private Context context;

    private List<Integer> staleEvents;

    private View.OnClickListener onClickListener;

    public static class EventHolder extends RecyclerView.ViewHolder {
        ImageView menuButton;
        Button actionButton1;
        Button actionButton2;

        ImageView posterAvatar;
        TextView posterName;
        TextView postTime;

        TextView eventName;
        TextView eventVenue;
        TextView eventNote;

        ImageView googleLogo;

        public EventHolder(View itemView) {
            super(itemView);

            menuButton = (ImageView) itemView.findViewById(R.id.cardActionButtonMenu);
            actionButton1 = (Button) itemView.findViewById(R.id.cardActionButton1);
            actionButton2 = (Button) itemView.findViewById(R.id.cardActionButton2);

            posterAvatar = (ImageView) itemView.findViewById(R.id.cardUserAvatar);
            posterName = (TextView) itemView.findViewById(R.id.cardUserName);
            postTime = (TextView) itemView.findViewById(R.id.cardUserPostTime);

            eventName = (TextView) itemView.findViewById(R.id.cardEventName);
            eventVenue = (TextView) itemView.findViewById(R.id.cardEventVenue);
            eventNote = (TextView) itemView.findViewById(R.id.cardEventNote);

            googleLogo = (ImageView) itemView.findViewById(R.id.cardPoweredByGoogle);
        }
    }

    public EventRecyclerViewAdapter(Context context, List<Event> events, View.OnClickListener onClickListener) {
        this.context = context;
        this.events = events;
        this.onClickListener = onClickListener;
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
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_ITEM) {
            Log.d(TAG, "Creating item");
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.event_card, parent, false);
            view.setOnClickListener(onClickListener);
            return new EventHolder(view);
        }
        else if (viewType == TYPE_HEADER) {
            Log.d(TAG, "Creating header");
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_header, parent, false);
            return new RecyclerHeaderViewHolder(view);
        }
        else
            return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder recyclerViewHolder, int position) {
        if (!isPositionHeader(position) && recyclerViewHolder instanceof EventHolder) {
            Event event = events.get(position - 1);
            EventHolder holder = (EventHolder) recyclerViewHolder;

            // set clicklistener for menu buttons
            holder.menuButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "Menu button clicked");
                }
            });

            // if the hosts contains the user, set action and text accordingly (Edit, Share)
            // if the hosts doesn't contain the user, set action to (Attend, Miss)
            //TODO

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
    }

    @Override
    public int getItemViewType(int position) {
        if (isPositionHeader(position)) {
            return TYPE_HEADER;
        }
        return TYPE_ITEM;
    }

    private boolean isPositionHeader(int position) {
        return position == 0;
    }

    public int getEventCount() { return events == null ? 0 : events.size(); }

    @Override
    public int getItemCount() {
        return events.size() + 1;
    }
}
