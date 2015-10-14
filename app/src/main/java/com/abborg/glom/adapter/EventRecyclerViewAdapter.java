package com.abborg.glom.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.abborg.glom.R;
import com.abborg.glom.model.Event;
import com.abborg.glom.model.FeedAction;
import com.abborg.glom.utils.CircleTransform;
import com.bumptech.glide.Glide;

import org.joda.time.DateTime;
import org.joda.time.Duration;

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

    public static class EventHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView posterAvatar;
        TextView posterName;
        TextView postTime;

        TextView eventName;
        TextView eventVenue;
        TextView eventNote;

        public EventHolder(View itemView) {
            super(itemView);

            posterAvatar = (ImageView) itemView.findViewById(R.id.cardUserAvatar);
            posterName = (TextView) itemView.findViewById(R.id.cardUserName);
            postTime = (TextView) itemView.findViewById(R.id.cardUserPostTime);

            eventName = (TextView) itemView.findViewById(R.id.cardEventName);
            eventVenue = (TextView) itemView.findViewById(R.id.cardEventVenue);
            eventNote = (TextView) itemView.findViewById(R.id.cardEventNote);
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
    }

    public void update(List<Event> events) {
        this.events = events;
        notifyDataSetChanged();
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

        // update event info
        holder.eventName.setText(event.getName());
        if (event.getLocation() != null) {
            holder.eventVenue.setText(event.getDateTime() + "\n" + event.getLocation().getLatitude() + ", " +
                    event.getLocation().getLongitude());
        }
        else {
            holder.eventVenue.setText("" + event.getDateTime());
        }
        holder.eventNote.setText(event.getNote());
    }

    public void addItem(Event event, int index) {
        events.add(index, event);
        notifyItemInserted(index);
    }

    public void deleteItem(int index) {
        events.remove(index);
        notifyItemRemoved(index);
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    public interface EventClickListener {
        void onItemClick(int position, View v);
    }
}
