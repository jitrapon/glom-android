package com.abborg.glom.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.abborg.glom.AppState;
import com.abborg.glom.Const;
import com.abborg.glom.R;
import com.abborg.glom.interfaces.BoardItemClickListener;
import com.abborg.glom.model.BoardItem;
import com.abborg.glom.model.DrawItem;
import com.abborg.glom.model.EventItem;
import com.abborg.glom.model.FeedAction;
import com.abborg.glom.model.FileItem;
import com.abborg.glom.utils.CircleTransform;
import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.StringSignature;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Handles the view logic to display items in a RecyclerView. The adapter can support
 * showing items in two layouts: the traditional linear layout and in a staggered grid.
 */
public class BoardRecyclerViewAdapter
        extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static String TAG = "BoardRecyclerViewAdapter";

    private List<BoardItem> items;

    private Context context;

    private List<Integer> staleItems;

    private BoardItemClickListener listener;

    private Handler handler;

    public static class EventHolder extends RecyclerView.ViewHolder {
        ImageView menuButton;
        Button actionButton1;

        ImageView posterAvatar;
        TextView posterName;
        TextView postTime;
        ImageView syncStatus;

        TextView eventName;
        TextView eventVenue;
        TextView eventNote;

        ImageView googleLogo;

        public EventHolder(View itemView) {
            super(itemView);

            menuButton = (ImageView) itemView.findViewById(R.id.cardActionButtonMenu);
            actionButton1 = (Button) itemView.findViewById(R.id.cardActionButton1);

            posterAvatar = (ImageView) itemView.findViewById(R.id.cardUserAvatar);
            posterName = (TextView) itemView.findViewById(R.id.cardUserName);
            postTime = (TextView) itemView.findViewById(R.id.cardUserPostTime);
            syncStatus = (ImageView) itemView.findViewById(R.id.card_sync_status);

            eventName = (TextView) itemView.findViewById(R.id.cardEventName);
            eventVenue = (TextView) itemView.findViewById(R.id.cardEventVenue);
            eventNote = (TextView) itemView.findViewById(R.id.cardEventNote);

            googleLogo = (ImageView) itemView.findViewById(R.id.cardPoweredByGoogle);
        }

        public void bind(final EventItem event, final BoardItemClickListener listener) {
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    listener.onItemClicked(event);
                }
            });
        }
    }

    public static class FileHolder extends RecyclerView.ViewHolder {
        ImageView menuButton;
        Button actionButton1;
        Button actionButton2;

        ImageView posterAvatar;
        TextView posterName;
        TextView postTime;
        ImageView syncStatus;

        TextView fileName;
        TextView fileNote;
        ImageView fileThumbnail;

        ProgressBar progressBar;

        public FileHolder(View itemView) {
            super(itemView);

            menuButton = (ImageView) itemView.findViewById(R.id.card_action_button_menu);
            actionButton1 = (Button) itemView.findViewById(R.id.card_action_1);
            actionButton2 = (Button) itemView.findViewById(R.id.card_action_2);

            posterAvatar = (ImageView) itemView.findViewById(R.id.card_user_avatar);
            posterName = (TextView) itemView.findViewById(R.id.card_user_name);
            postTime = (TextView) itemView.findViewById(R.id.card_user_post_time);
            syncStatus = (ImageView) itemView.findViewById(R.id.card_sync_status);

            fileName = (TextView) itemView.findViewById(R.id.file_name);
            fileNote = (TextView) itemView.findViewById(R.id.file_note);
            fileThumbnail = (ImageView) itemView.findViewById(R.id.file_thumbnail);

            progressBar = (ProgressBar) itemView.findViewById(R.id.file_progress);
        }

        public void bind(final FileItem file, final BoardItemClickListener listener) {
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    listener.onItemClicked(file);
                }
            });
        }
    }

    public static class DrawingHolder extends RecyclerView.ViewHolder {
        ImageView posterAvatar;
        TextView posterName;
        TextView postTime;
        ImageView syncStatus;

        ImageView thumbnail;

        public DrawingHolder(View itemView) {
            super(itemView);

            posterAvatar = (ImageView) itemView.findViewById(R.id.card_user_avatar);
            posterName = (TextView) itemView.findViewById(R.id.card_user_name);
            postTime = (TextView) itemView.findViewById(R.id.card_user_post_time);
            syncStatus = (ImageView) itemView.findViewById(R.id.card_sync_status);

            thumbnail = (ImageView) itemView.findViewById(R.id.note_thumbnail);
        }

        public void bind(final DrawItem file, final BoardItemClickListener listener) {
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    listener.onItemClicked(file);
                }
            });
        }
    }

    public BoardRecyclerViewAdapter(Context context, List<BoardItem> items, BoardItemClickListener clickListener, Handler handler) {
        this.context = context;
        this.items = items;
        listener = clickListener;
        staleItems = new ArrayList<>();
        this.handler = handler;
        setHasStableIds(true);
    }

    public void update(List<BoardItem> events) {
        // update from specific list of items
        if (events != null) {
            items = events;
            notifyDataSetChanged();
        }

        // TODO update everything from request
        else {
            for (int position : staleItems) {
                notifyItemChanged(position);
            }
            staleItems.clear();
        }
    }

    public void updateAt(boolean add, int index) {
        if (add) notifyItemInserted(index);
        else notifyItemRemoved(index);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == BoardItem.TYPE_EVENT) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_event, parent, false);
            return new EventHolder(view);
        }
        else if (viewType == BoardItem.TYPE_FILE) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_file, parent, false);
            return new FileHolder(view);
        }
        else if (viewType == BoardItem.TYPE_DRAWING) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_note, parent, false);
            return new DrawingHolder(view);
        }
        else return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder recyclerViewHolder, int position) {
        if (recyclerViewHolder instanceof EventHolder)
            setEventViewHolder(position, recyclerViewHolder);
        else if (recyclerViewHolder instanceof FileHolder)
            setFileViewHolder(position, recyclerViewHolder);
        else if (recyclerViewHolder instanceof DrawingHolder)
            setDrawingViewHolder(position, recyclerViewHolder);
    }

    @Override
    /**
     * When notifyDataSetChanged() is called, getItemId will depend upon the values of what is
     * combined to be the hashcode
     */
    public long getItemId(int position) {
        if (items != null && !items.isEmpty()) {
            BoardItem item = items.get(position);
            long id = RecyclerView.NO_ID;
            if (item.getType() == BoardItem.TYPE_EVENT) {
                EventItem event = (EventItem) item;
                String name = TextUtils.isEmpty(event.getName()) ? "" : event.getName();
                long startTime = event.getStartTime() == null ? 0L : event.getStartTime().getMillis();
                long endTime = event.getEndTime() == null ? 0l : event.getEndTime().getMillis();
                String place = TextUtils.isEmpty(event.getPlace()) ? "" : event.getPlace();
                double lat = event.getLocation() == null ? 0 : event.getLocation().getLatitude();
                double lng = event.getLocation() == null ? 0 : event.getLocation().getLongitude();
                String note = TextUtils.isEmpty(event.getNote()) ? "" : event.getNote();
                id = (event.getId() + event.getType() + event.getUpdatedTime() + name + startTime + endTime +
                        place + lat + lng + note + event.getSyncStatus()).hashCode();
            }
            else if (item.getType() == BoardItem.TYPE_FILE) {
                FileItem file = (FileItem) item;
                String name = TextUtils.isEmpty(file.getName()) ? "" : file.getName();
                String note = TextUtils.isEmpty(file.getNote()) ? "" : file.getNote();
                String mimetype = TextUtils.isEmpty(file.getMimetype()) ? "" : file.getMimetype();
                long size = file.getSize();
                String path = file.getLocalCache()==null? "" : file.getLocalCache().getPath();
                long created = file.getCreatedTime() == null ? 0L : file.getCreatedTime().getMillis();
                long updated = file.getUpdatedTime() == null ? 0L : file.getUpdatedTime().getMillis();
                id = (name + note + mimetype + size + path + created + updated + + file.getSyncStatus()).hashCode();
            }
            else if (item.getType() == BoardItem.TYPE_DRAWING) {
                DrawItem note = (DrawItem) item;
                String name = TextUtils.isEmpty(note.getName()) ? "" : note.getName();
                long created = note.getCreatedTime() == null ? 0L : note.getCreatedTime().getMillis();
                long updated = note.getUpdatedTime() == null ? 0L : note.getUpdatedTime().getMillis();
                id = (name + created + updated + note.getSyncStatus()).hashCode();
            }
            return id;
        }

        return super.getItemId(position);
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).getType();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void addItem(String id) {
        notifyItemInserted(0);
    }

    public void updateItem(String id) {
        int index = -1;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId().equals(id)) {
                index = i;
                break;
            }
        }
        if (index == -1) return;

        notifyItemChanged(index);
    }

    public void deleteItem(String id) {
        int index = -1;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId().equals(id)) {
                index = i;
                break;
            }
        }
        if (index == -1) return;

        notifyItemRemoved(index);
    }

    /**************************************************************
     * View Holder helpers
     **************************************************************/

    private void attachMenuOptions(ImageView menuBtn, final String id) {
        menuBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final CharSequence[] items = {
                        context.getResources().getString(R.string.menu_item_delete),
                        context.getResources().getString(R.string.menu_item_copy),
                        context.getResources().getString(R.string.menu_item_send),
                        context.getResources().getString(R.string.menu_item_star)
                };
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        switch (item) {
                            case 0:
                                if (handler != null) handler.sendMessage(handler.obtainMessage(Const.MSG_ITEM_TO_DELETE, id));
                                break;
                            case 1:

                                break;
                            case 2:

                                break;
                            case 3:

                                break;
                            default: return;
                        }
                    }
                });
                AlertDialog alert = builder.create();
                alert.setCanceledOnTouchOutside(true);
                alert.show();
            }
        });
    }

    private void attachPostInfo(FeedAction feedAction, TextView posterName, ImageView posterAvatar, TextView postTime) {
        if (feedAction != null) {
            if (feedAction.user != null) {

                // set update text
                switch(feedAction.type) {
                    case FeedAction.CREATE_EVENT:
                        posterName.setText(Html.fromHtml("<b>" + feedAction.user.getName() + "</b> " +
                                context.getResources().getString(R.string.card_post_create_event)));
                        break;
                    case FeedAction.CANCEL_EVENT:
                        posterName.setText(Html.fromHtml("<b>" + feedAction.user.getName() + "</b> " +
                                context.getResources().getString(R.string.card_post_cancel_event)));
                        break;
                    case FeedAction.UPDATE_EVENT:
                        posterName.setText(Html.fromHtml("<b>" + feedAction.user.getName() + "</b> " +
                                context.getResources().getString(R.string.card_post_update_event)));
                        break;
                    default:
                        posterName.setText(feedAction.user.getName());
                }

                // load avatar
                Glide.with(context)
                        .load(feedAction.user.getAvatar()).fitCenter()
                        .transform(new CircleTransform(context))
                        .override(context.getResources().getDimensionPixelSize(R.dimen.card_avatar_size),
                                context.getResources().getDimensionPixelSize(R.dimen.card_avatar_size))
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .crossFade(1000)
                        .into(posterAvatar);
            }
            if (feedAction.dateTime != null) {
                DateTime now = new DateTime();
                Duration duration = new Duration(feedAction.dateTime, now);
                String displayTime;
                if (duration.getStandardMinutes() < 5)
                    displayTime = context.getResources().getString(R.string.time_info_just_now);
                else if (duration.getStandardMinutes() < 60)
                    displayTime = duration.getStandardMinutes() + " " + context.getResources().getString(R.string.time_unit_minute);
                else if (duration.getStandardHours() < 24)
                    displayTime = duration.getStandardHours() + " " + context.getResources().getString(R.string.time_unit_hour);
                else
                    displayTime = duration.getStandardDays() + " " + context.getResources().getString(R.string.time_unit_day);

                postTime.setText(displayTime);
            }
        }
    }

    private void setFileViewHolder(int position, RecyclerView.ViewHolder recyclerViewHolder) {
        FileItem file = (FileItem) items.get(position);
        final String id = file.getId();
        final FileHolder holder = (FileHolder) recyclerViewHolder;

        holder.bind(file, listener);

        // attach context menu button
        attachMenuOptions(holder.menuButton, id);

        // attach the last feed info about this post
        attachPostInfo(file.getLastAction(), holder.posterName, holder.posterAvatar, holder.postTime);

        // set action buttons
        if (file.getLocalCache() == null || !file.getLocalCache().exists()) {
            holder.actionButton1.setText(context.getResources().getString(R.string.card_action_download));
            holder.actionButton2.setText(context.getResources().getString(R.string.card_action_share));
            holder.actionButton1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    holder.itemView.performClick();
                }
            });
        }
        else {
            holder.actionButton1.setText(context.getResources().getString(R.string.card_action_view));
            holder.actionButton2.setText(context.getResources().getString(R.string.card_action_share));
            holder.actionButton1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    holder.itemView.performClick();
                }
            });
        }

        // set sync status and progress bar
        if (file.getSyncStatus() == BoardItem.SYNC_COMPLETE) {
            holder.syncStatus.setVisibility(View.INVISIBLE);
            holder.progressBar.setVisibility(View.GONE);
        }
        else {
            holder.syncStatus.setVisibility(View.VISIBLE);
            holder.progressBar.setVisibility(View.VISIBLE);

            if (file.getSyncStatus() == BoardItem.SYNC_ERROR)
                holder.syncStatus.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_sync_failed));
            else if (file.getSyncStatus() == BoardItem.SYNC_IN_PROGRESS) {
                holder.syncStatus.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_sync));
                if (file.getProgress() > 0) {
                    holder.progressBar.setIndeterminate(false);
                    holder.progressBar.setProgress(file.getProgress());
                }
                else {
                    holder.progressBar.setIndeterminate(true);
                }
            }
        }

        // update file info and thumbnail
        String name = !TextUtils.isEmpty(file.getName()) ? file.getName()
                : context.getResources().getString(R.string.file_name_placeholder);
        holder.fileName.setText(name);
        String note = !TextUtils.isEmpty(file.getName()) ? file.getNote()
                : "";
        holder.fileNote.setText(note);

        // set up image icons
        int icon;
        if (file.isImage() && file.getLocalCache() != null && file.getLocalCache().exists()) {
            icon = R.drawable.ic_placeholder_image;
            if (file.isGif()) {
                Glide.with(context)
                        .load(file.getLocalCache()).asGif().centerCrop()
                        .signature(new StringSignature(String.valueOf(file.getLocalCache().lastModified())))
                        .placeholder(icon)
                        .error(icon)
                        .crossFade(1000)
                        .into(holder.fileThumbnail);
            }
            else {
                Glide.with(context)
                        .load(file.getLocalCache()).centerCrop()
                        .signature(new StringSignature(String.valueOf(file.getLocalCache().lastModified())))
                        .placeholder(icon)
                        .error(icon)
                        .crossFade(1000)
                        .into(holder.fileThumbnail);
            }
        }
        else {
            icon = R.drawable.ic_placeholder_file;
            Glide.with(context)
                    .load(icon).centerCrop()
                    .signature(new StringSignature(String.valueOf(file.getLocalCache().lastModified())))
                    .crossFade(1000)
                    .into(holder.fileThumbnail);
        }
    }

    private void setEventViewHolder(int position, RecyclerView.ViewHolder recyclerViewHolder) {
        EventItem event = (EventItem) items.get(position);
        final String id = event.getId();
        EventHolder holder = (EventHolder) recyclerViewHolder;

        holder.bind(event, listener);

        // attach context menu button
        attachMenuOptions(holder.menuButton, id);

        // attach the last feed info about this post
        attachPostInfo(event.getLastAction(), holder.posterName, holder.posterAvatar, holder.postTime);

        // if the hosts contains the user, set action and text accordingly (Edit, Share)
        // if the hosts doesn't contain the user, set action to (Attend, Miss)
        if (event.getPlace() != null || event.getLocation() != null) {
            holder.actionButton1.setVisibility(View.VISIBLE);
            holder.actionButton1.setText(context.getResources().getString(R.string.card_action_get_directions));

            final String placeId = event.getPlace();
            if (!TextUtils.isEmpty(placeId)) {
                holder.actionButton1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        GoogleApiClient apiClient = AppState.getInstance().getGoogleApiClient();
                        if (apiClient != null && apiClient.isConnected()) {
                            PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi.getPlaceById(apiClient, placeId);
                            placeResult.setResultCallback(new ResultCallback<PlaceBuffer>() {

                                @Override
                                public void onResult(PlaceBuffer places) {
                                    if (!places.getStatus().isSuccess()) {
                                        Log.e(TAG, "Place query did not complete. Error: " + places.getStatus().toString());
                                        places.release();
                                        return;
                                    }

                                    final Place place = places.get(0);
                                    Log.d(TAG, "Place query succeeded for " + place.getName());
                                    double lat = place.getLatLng().latitude;
                                    double lng = place.getLatLng().longitude;
                                    places.release();

                                    launchGoogleMapsNavigation(lat, lng);
                                }
                            });
                        }
                    }
                });
            }
            else if (event.getLocation() != null) {
                final double lat = event.getLocation().getLatitude();
                final double lng = event.getLocation().getLongitude();

                holder.actionButton1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        launchGoogleMapsNavigation(lat, lng);
                    }
                });
            }
        }
        else {
            holder.actionButton1.setVisibility(View.GONE);
        }

        // set sync status
        if (event.getSyncStatus() == BoardItem.SYNC_COMPLETE)
            holder.syncStatus.setVisibility(View.INVISIBLE);
        else {
            holder.syncStatus.setVisibility(View.VISIBLE);

            if (event.getSyncStatus() == BoardItem.SYNC_ERROR)
                holder.syncStatus.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_sync_failed));
            else if (event.getSyncStatus() == BoardItem.SYNC_IN_PROGRESS)
                holder.syncStatus.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_sync));
        }

        // update event info
        // always show time if available
        // if place is specified, show place, otherwise show coordinates
        holder.eventName.setText(event.getName());
        String time = "";
        String duration = "";
        if (event.getStartTime() != null) {
            DateTimeFormatter formatter = DateTimeFormat.forPattern(context.getResources().getString(R.string.card_event_datetime_format));
            DateTimeFormatter timeFormatter = DateTimeFormat.forPattern(context.getResources().getString(R.string.card_event_time_format));
            DateTime now = new DateTime();
            Period period = new Period(now, event.getStartTime());

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

            String formattedStartDateTime = "";
            String formattedEndDateTime = "";
            int daysBetween = Days.daysBetween(now.withTimeAtStartOfDay(), event.getStartTime().withTimeAtStartOfDay()).getDays();
            DateTimeFormatter dayWithTimeFormatter = DateTimeFormat.forPattern("EEEE, " +
                    context.getResources().getString(R.string.card_event_time_format));
            if (daysBetween == -1) {
                formattedStartDateTime = context.getResources().getString(R.string.time_yesterday)
                        + ", " + timeFormatter.print(event.getStartTime());
            }
            else if (daysBetween == 0) {
                formattedStartDateTime = context.getResources().getString(R.string.time_today)
                        + ", " + timeFormatter.print(event.getStartTime());
            }
            else if (daysBetween == 1) {
                formattedStartDateTime = context.getResources().getString(R.string.time_tomorrow)
                        + ", " + timeFormatter.print(event.getStartTime());
            }
            else if (daysBetween < 7 && daysBetween > 0) {
                formattedStartDateTime = dayWithTimeFormatter.print(event.getStartTime());
            }
            else {
                formattedStartDateTime = formatter.print(event.getStartTime());
            }

            if (event.getEndTime() != null) {
                daysBetween = Days.daysBetween(now.withTimeAtStartOfDay(), event.getEndTime().withTimeAtStartOfDay()).getDays();
                int daysDuration = Days.daysBetween(event.getStartTime().withTimeAtStartOfDay(),
                        event.getEndTime().withTimeAtStartOfDay()).getDays();
                if (daysDuration == 0) {
                    formattedEndDateTime = " - " + timeFormatter.print(event.getEndTime()) + " ";
                }
                else {
                    if (daysBetween == -1) {
                        formattedEndDateTime = " - " + context.getResources().getString(R.string.time_yesterday)
                                + ", " + timeFormatter.print(event.getEndTime()) + " ";
                    }
                    else if (daysBetween == 0) {
                        formattedEndDateTime = " - " + context.getResources().getString(R.string.time_today)
                                + ", " + timeFormatter.print(event.getEndTime()) + " ";
                    }
                    else if (daysBetween == 1) {
                        formattedEndDateTime = " - " + context.getResources().getString(R.string.time_tomorrow)
                                + ", " + timeFormatter.print(event.getEndTime()) + " ";
                    }
                    else if (daysBetween < 7 && daysBetween > 0) {
                        formattedEndDateTime = " - " + dayWithTimeFormatter.print(event.getEndTime()) + " ";
                    }
                    else {
                        formattedEndDateTime = " - " + formatter.print(event.getEndTime()) + " ";
                    }
                }
            }

            String durationPrefix = period.getMillis() > 0 ? context.getResources().getString(R.string.duration_incoming_prefix) :
                    context.getResources().getString(R.string.duration_started_prefix);
            time = formattedStartDateTime + formattedEndDateTime + " (" + durationPrefix + " " + duration + ")\n";
        }

        if (!TextUtils.isEmpty(event.getPlace())) {

            // retrieve place from its ID
            final EventHolder viewHolder = holder;
            final String placeTime = time;
            String placeName = event.getLocation()!=null ?
                    event.getLocation().getLatitude() + ", " + event.getLocation().getLongitude() :
                    context.getResources().getString(R.string.notify_retrieving_place_info);
            GoogleApiClient apiClient = AppState.getInstance().getGoogleApiClient();
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

                        int updatedEventIndex = staleItems.indexOf(viewHolder.getAdapterPosition());
                        if (updatedEventIndex >= 0 && updatedEventIndex < staleItems.size())
                            staleItems.remove(updatedEventIndex);

                        places.release();
                    }
                });
            }
            else {
                Log.e(TAG, "Google API client is not connected, error retrieving place info");
            }

            holder.eventVenue.setText(time + placeName);
            holder.googleLogo.setVisibility(View.VISIBLE);

            // add to list of items that contain places info to be refreshed
            staleItems.add(holder.getAdapterPosition());
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

        if (TextUtils.isEmpty(holder.eventVenue.getText())) holder.eventVenue.setVisibility(View.GONE);
        else holder.eventVenue.setVisibility(View.VISIBLE);

        if (TextUtils.isEmpty(event.getNote())) holder.eventNote.setVisibility(View.GONE);
        else holder.eventNote.setVisibility(View.VISIBLE);
    }

    private void setDrawingViewHolder(int position, RecyclerView.ViewHolder recyclerViewHolder) {
        DrawItem drawing = (DrawItem) items.get(position);
        final String id = drawing.getId();
        final DrawingHolder holder = (DrawingHolder) recyclerViewHolder;

        holder.bind(drawing, listener);

//        // attach context menu button
//        attachMenuOptions(holder.menuButton, id);

        // attach the last feed info about this post
        attachPostInfo(drawing.getLastAction(), holder.posterName, holder.posterAvatar, holder.postTime);

        // set sync status and progress bar
        if (drawing.getSyncStatus() == BoardItem.SYNC_COMPLETE) {
            holder.syncStatus.setVisibility(View.INVISIBLE);
        }
        else {
            holder.syncStatus.setVisibility(View.VISIBLE);

            if (drawing.getSyncStatus() == BoardItem.SYNC_ERROR)
                holder.syncStatus.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_sync_failed));
            else if (drawing.getSyncStatus() == BoardItem.SYNC_IN_PROGRESS) {
                holder.syncStatus.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_sync));
            }
        }

        // update drawing thumbnail
        File file = drawing.getLocalCache();
        if (file != null && file.exists()) {
            Glide.with(context)
                    .load(file).centerCrop()
                    .crossFade(1000)
                    .signature(new StringSignature(String.valueOf(file.lastModified())))
                    .placeholder(R.drawable.ic_placeholder_drawing)
                    .error(R.drawable.ic_placeholder_drawing)
                    .into(holder.thumbnail);
        }
    }

    /**************************************************************
     * CARD ACTION OPERATIONS
     **************************************************************/
    private void launchGoogleMapsNavigation(double lat, double lng) {
        Uri gmmIntentUri = Uri.parse(
                String.format(Locale.ENGLISH, "google.navigation:q=%1f,%2f", lat, lng));
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        context.startActivity(mapIntent);
    }
}