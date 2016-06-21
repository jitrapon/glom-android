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

import com.abborg.glom.ApplicationState;
import com.abborg.glom.Const;
import com.abborg.glom.R;
import com.abborg.glom.interfaces.BoardItemClickListener;
import com.abborg.glom.model.BoardItem;
import com.abborg.glom.model.EventItem;
import com.abborg.glom.model.FeedAction;
import com.abborg.glom.model.FileItem;
import com.abborg.glom.utils.CircleTransform;
import com.afollestad.sectionedrecyclerview.SectionedRecyclerViewAdapter;
import com.bumptech.glide.Glide;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Handles the view logic to display mappedItems in a RecyclerView. The adapter can support
 * showing mappedItems in two layouts: the traditional linear layout and in a staggered grid.
 */
public class BoardRecyclerViewAdapter1 extends SectionedRecyclerViewAdapter<RecyclerView.ViewHolder> {

    private static String TAG = "BoardRecyclerViewAdapter";

    /** the main model in this adapter, maps the item type to list of the mappedItems so we can easily
     * retrieve the item list based on their types
     */
    private Map<Integer, List<BoardItem>> mappedItems;
    private List<BoardItem> items;

    private Context context;

    private List<Integer> staleItems;

    private BoardItemClickListener listener;

    private Handler handler;

    /** How to categorize the map **/
    private enum Category {
        TAG,
        TYPE
    }

    /** Maps section to view types **/
    private static final int[] sections = {
            BoardItem.TYPE_EVENT,
            BoardItem.TYPE_FILE
    };

    public BoardRecyclerViewAdapter1(Context ctx, List<BoardItem> models, BoardItemClickListener clickListener, Handler h) {
        context = ctx;
        items = models;
        mappedItems = new HashMap<>(sections.length);
        updateMap(Category.TYPE, -1);
        listener = clickListener;
        staleItems = new ArrayList<>();
        handler = h;
        setHasStableIds(true);
    }

    private int updateMap(Category category, int originalIndex) {
        if (category == Category.TAG) return -1;
        else if (category == Category.TYPE) {
            if (originalIndex == -1) {
                for (Integer section : sections) {
                    mappedItems.put(section, new ArrayList<BoardItem>());
                    Log.d(TAG, "Initialized new section of type " + section);
                }

                if (items != null && !items.isEmpty()) {
                    for (BoardItem item : items) {
                        mappedItems.get(item.getType()).add(item);
                        Log.d(TAG, "Adding item " + item.getId() + " of type " + item.getType() + " to map");
                    }
                }
                return -1;
            }
            else {
                BoardItem updatedItem = items.get(originalIndex);
                List<BoardItem> listToUpdate = mappedItems.get(updatedItem.getType());
                for (int i = 0; i < listToUpdate.size(); i++) {
                    BoardItem e = listToUpdate.get(i);
                    if (e.getId().equals(updatedItem.getId())) {
                        listToUpdate.set(i, updatedItem);

                        int sectionIndex = 0;
                        int numTotal = 0;
                        int itemIndex = 0;
                        for (Integer section : sections) {
                            if (section == updatedItem.getType()) {
                                itemIndex = i;
                                break;
                            }
                            else {
                                List<BoardItem> items = mappedItems.get(section);
                                numTotal += items == null ? 0 : items.size();
                                if (numTotal > 0) sectionIndex++;
                            }
                        }
                        int adapterIndex = sectionIndex + numTotal + itemIndex + 1;
                        Log.d(TAG, "Updated an item " + updatedItem.getId() + " of type "
                                + updatedItem.getType() + " at " + adapterIndex);
                        return adapterIndex;
                    }
                }
            }
        }
        return -1;
    }

    private int addToMap(int originalIndex) {
        BoardItem addedItem = items.get(originalIndex);
        List<BoardItem> listToAdd = mappedItems.get(addedItem.getType());
        listToAdd.add(0, addedItem);

        int sectionIndex = 0;
        int numTotal = 0;
        int itemIndex = 0;
        for (Integer section : sections) {
            if (section == addedItem.getType()) {
                itemIndex = 0;
                break;
            }
            else {
                List<BoardItem> items = mappedItems.get(section);
                numTotal += items == null ? 0 : items.size();
                if (numTotal > 0) sectionIndex++;
            }
        }
        int adapterIndex = sectionIndex + numTotal + itemIndex + 1;
        Log.d(TAG, "Added an item " + addedItem.getId() + " of type " + addedItem.getType() + " at " + adapterIndex);
        return adapterIndex;
    }

    private int removeFromMap(int originalIndex) {
        BoardItem removedItem = items.get(originalIndex);
        List<BoardItem> listToRemove = mappedItems.get(removedItem.getType());
        int index = -1;
        for (int i = 0; i < listToRemove.size(); i++) {
            if (listToRemove.get(i).getId().equals(removedItem.getId())) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            int sectionIndex = 0;
            int numTotal = 0;
            int itemIndex = 0;
            for (Integer section : sections) {
                if (section == removedItem.getType()) {
                    itemIndex = index;
                    break;
                }
                else {
                    List<BoardItem> items = mappedItems.get(section);
                    numTotal += items == null ? 0 : items.size();
                    if (numTotal > 0) sectionIndex++;
                }
            }
            int adapterIndex = sectionIndex + numTotal + itemIndex + 1;
            Log.d(TAG, "Deleted an item " + removedItem.getId() + " of type " + removedItem.getType() + " at " + adapterIndex);

            listToRemove.remove(index);
            return mappedItems.size() == 0 ? -1 : adapterIndex;
        }
        return -1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     * The number of sections
     */
    @Override
    public int getSectionCount() {
        return sections.length;
    }

    /**
     * The number of items in the specified section
     */
    @Override
    public int getItemCount(int section) {
        List<BoardItem> items = getItems(section);
        return items == null ? 0 : items.size();
    }

    @Override
    public void onBindHeaderViewHolder(RecyclerView.ViewHolder viewHolder, int section) {
        SectionHeaderViewHolder holder = (SectionHeaderViewHolder) viewHolder;
        if (sections[section] == BoardItem.TYPE_EVENT)
            holder.title.setText("Events");
        else if (sections[section] == BoardItem.TYPE_FILE)
            holder.title.setText("Files");
    }

    private List<BoardItem> getItems(int section) {
        return mappedItems.get(sections[section]);
    }

    private BoardItem getItem(int section, int sectionPosition) {
        List<BoardItem> items = getItems(section);
        if (items != null && !items.isEmpty()) return items.get(sectionPosition);
        else return null;
    }

    /**
     * Header of the section
     */
    public static class SectionHeaderViewHolder extends RecyclerView.ViewHolder {

        TextView title;

        public SectionHeaderViewHolder(View itemView) {
            super(itemView);

            title = (TextView) itemView.findViewById(R.id.section_title_text);
        }
    }

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

    public void addItem(String id) {
        try {
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).getId().equals(id)) {
                    int index = addToMap(i);
                    if (index != -1) notifyItemInserted(index);
                    else notifyDataSetChanged();
                    break;
                }
            }
        }
        catch (Exception ex) {
            Log.e(TAG, ex.getMessage());
        }
    }

    public void updateItem(String id) {
        try {
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).getId().equals(id)) {
                    int index = updateMap(Category.TYPE, i);
                    if (index != -1) notifyItemInserted(index);
                    else notifyDataSetChanged();
                    break;
                }
            }
        }
        catch (Exception ex) {
            Log.e(TAG, ex.getMessage());
        }
    }

    public void deleteItem(String id) {
        try {
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).getId().equals(id)) {
                    int index = removeFromMap(i);
                    notifyDataSetChanged();
                    break;
                }
            }
        }
        catch (Exception ex) {
            Log.e(TAG, ex.getMessage());
        }
    }

    public void update(List<BoardItem> models) {
        if (models != null) {
            items = models;
            updateMap(Category.TYPE, -1);
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

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.section_header, parent, false);
            return new SectionHeaderViewHolder(view);
        }
        else if (viewType == BoardItem.TYPE_EVENT) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_event, parent, false);
            return new EventHolder(view);
        }
        else if (viewType == BoardItem.TYPE_FILE) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_file, parent, false);
            return new FileHolder(view);
        }
        else
            return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder recyclerViewHolder,
                                 int section, int sectionPosition, int absolutePosition) {
        if (recyclerViewHolder instanceof EventHolder)
            setEventViewHolder(recyclerViewHolder, section, sectionPosition, absolutePosition);
        else if (recyclerViewHolder instanceof FileHolder)
            setFileViewHolder(recyclerViewHolder, section, sectionPosition, absolutePosition);
    }

    @Override
    public int getItemViewType(int section, int sectionPosition, int absolutePosition) {
        BoardItem item = getItem(section, sectionPosition);
        if (item == null) return 0;
        else return item.getType();
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

    private void setFileViewHolder(RecyclerView.ViewHolder recyclerViewHolder,
                                   int section, int sectionPosition, int absolutePosition) {
        FileItem file = (FileItem) getItem(section, sectionPosition);
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
        if (file.isImage()) {
            icon = R.drawable.ic_placeholder_image;
            if (file.isGif()) {
                Glide.with(context)
                        .load(file.getLocalCache()).asGif().centerCrop()
                        .placeholder(icon)
                        .error(icon)
                        .crossFade(1000)
                        .into(holder.fileThumbnail);
            }
            else {
                if (file.getLocalCache() != null && file.getLocalCache().exists()) {
                    Glide.with(context)
                            .load(file.getLocalCache()).centerCrop()
                            .placeholder(icon)
                            .error(icon)
                            .crossFade(1000)
                            .into(holder.fileThumbnail);
                }
                else {
                    Glide.with(context)
                            .load(icon).centerCrop()
                            .crossFade(1000)
                            .into(holder.fileThumbnail);
                }
            }
        }
        else {
            icon = R.drawable.ic_placeholder_file;
            Glide.with(context)
                    .load(icon).centerCrop()
                    .crossFade(1000)
                    .into(holder.fileThumbnail);
        }
    }

    private void setEventViewHolder(RecyclerView.ViewHolder recyclerViewHolder,
                                    int section, int sectionPosition, int absolutePosition) {
        EventItem event = (EventItem) getItem(section, sectionPosition);
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
                        GoogleApiClient apiClient = ApplicationState.getInstance().getGoogleApiClient();
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
            GoogleApiClient apiClient = ApplicationState.getInstance().getGoogleApiClient();
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

            // add to list of mappedItems that contain places info to be refreshed
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
