package com.abborg.glom.adapters;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.abborg.glom.ApplicationState;
import com.abborg.glom.R;
import com.abborg.glom.di.ComponentInjector;
import com.abborg.glom.interfaces.BoardItemClickListener;
import com.abborg.glom.interfaces.MultiSelectionListener;
import com.abborg.glom.model.BoardItem;
import com.abborg.glom.model.CheckedItem;
import com.abborg.glom.model.DrawItem;
import com.abborg.glom.model.EventItem;
import com.abborg.glom.model.FeedAction;
import com.abborg.glom.model.FileItem;
import com.abborg.glom.model.LinkItem;
import com.abborg.glom.model.ListItem;
import com.abborg.glom.model.NoteItem;
import com.abborg.glom.utils.CircleTransform;
import com.abborg.glom.utils.DateUtils;
import com.abborg.glom.utils.TaskUtils;
import com.abborg.glom.views.InterceptTouchCardView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.StringSignature;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * Handles the view logic to display items in a RecyclerView. The adapter can support
 * showing items in two layouts: the traditional linear layout and in a staggered grid.
 */
public class BoardItemAdapter
        extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements MultiSelectionListener {

    @Inject
    ApplicationState appState;

    private static String TAG = "BoardItemAdapter";

    private Context context;

    private List<BoardItem> items;
    private List<Integer> staleItems;
    private SparseBooleanArray selectedItems;

    private BoardItemClickListener listener;

    /**************************************************
     * VIEW HOLDERS
     **************************************************/

    private static class EventHolder extends RecyclerView.ViewHolder {

        Button mapButton;
        TextView nameText;
        TextView timeText;
        TextView locationText;
        TextView noteText;
        ImageView googleAttrImage;
        CardView cardView;

        private EventHolder(View itemView) {
            super(itemView);

            mapButton = (Button) itemView.findViewById(R.id.action_get_directions);
            nameText = (TextView) itemView.findViewById(R.id.event_name);
            timeText = (TextView) itemView.findViewById(R.id.event_time);
            locationText = (TextView) itemView.findViewById(R.id.event_location);
            noteText = (TextView) itemView.findViewById(R.id.event_info);
            googleAttrImage = (ImageView) itemView.findViewById(R.id.card_powered_by_google);
            cardView = (CardView) itemView.findViewById(R.id.card_view);
        }

        private void setClickListener(final EventItem item, final BoardItemClickListener listener) {
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    listener.onItemClicked(item, getAdapterPosition());
                }
            });

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    return listener.onItemLongClicked(item, getAdapterPosition());
                }
            });
        }
    }

    private static class FileHolder extends RecyclerView.ViewHolder {

        Button viewButton;
        TextView fileName;
        TextView fileNote;
        ImageView fileThumbnail;
        CardView cardView;

        private FileHolder(View itemView) {
            super(itemView);

            viewButton = (Button) itemView.findViewById(R.id.action_view_file);
            fileName = (TextView) itemView.findViewById(R.id.file_name);
            fileNote = (TextView) itemView.findViewById(R.id.file_note);
            fileThumbnail = (ImageView) itemView.findViewById(R.id.file_thumbnail);
            cardView = (CardView) itemView.findViewById(R.id.card_view);
        }

        private void setClickListener(final FileItem item, final BoardItemClickListener listener) {
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    listener.onItemClicked(item, getAdapterPosition());
                }
            });

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    return listener.onItemLongClicked(item, getAdapterPosition());
                }
            });
        }
    }

    private static class DrawingHolder extends RecyclerView.ViewHolder {

        ImageView thumbnail;
        CardView cardView;

        private DrawingHolder(View itemView) {
            super(itemView);

            thumbnail = (ImageView) itemView.findViewById(R.id.drawing_thumbnail);
            cardView = (CardView) itemView.findViewById(R.id.card_view);
        }

        private void setOnClickListener(final DrawItem item, final BoardItemClickListener listener) {
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    listener.onItemClicked(item, getAdapterPosition());
                }
            });

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    return listener.onItemLongClicked(item, getAdapterPosition());
                }
            });
        }
    }

    private static class LinkHolder extends RecyclerView.ViewHolder {

        Button editButton;
        Button copyButton;
        TextView url;
        ImageView thumbnail;
        TextView title;
        TextView description;
        CardView cardView;
        RelativeLayout thumbnailLayout;

        private LinkHolder(View itemView) {
            super(itemView);

            url = (TextView) itemView.findViewById(R.id.link_url);
            thumbnail = (ImageView) itemView.findViewById(R.id.link_thumbnail);
            title = (TextView) itemView.findViewById(R.id.link_title);
            description = (TextView) itemView.findViewById(R.id.link_description);
            cardView = (CardView) itemView.findViewById(R.id.card_view);
            thumbnailLayout = (RelativeLayout) itemView.findViewById(R.id.link_thumbnail_layout);
            editButton = (Button) itemView.findViewById(R.id.action_edit_link);
            copyButton = (Button) itemView.findViewById(R.id.action_copy_link);
        }

        private void setClickListener(final LinkItem item, final BoardItemClickListener listener) {
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    listener.onItemClicked(item, getAdapterPosition());
                }
            });

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    return listener.onItemLongClicked(item, getAdapterPosition());
                }
            });
        }
    }

    private static class ListHolder extends RecyclerView.ViewHolder {

        InterceptTouchCardView cardView;
        TextView titleView;
        RecyclerView listView;

        private ListHolder(View itemView) {
            super(itemView);

            cardView = (InterceptTouchCardView) itemView.findViewById(R.id.card_view);
            titleView = (TextView) itemView.findViewById(R.id.list_title);
            listView = (RecyclerView) itemView.findViewById(R.id.list_items);
        }

        private void setClickListener(final ListItem item, final BoardItemClickListener listener) {
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    listener.onItemClicked(item, getAdapterPosition());
                }
            });

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    return listener.onItemLongClicked(item, getAdapterPosition());
                }
            });
        }
    }

    private static class NoteHolder extends RecyclerView.ViewHolder {

        CardView cardView;
        TextView titleText;
        TextView contentText;

        private NoteHolder(View itemView) {
            super(itemView);

            cardView = (CardView) itemView.findViewById(R.id.card_view);
            titleText = (TextView) itemView.findViewById(R.id.note_title);
            contentText = (TextView) itemView.findViewById(R.id.note_text);
        }

        private void setClickListener(final NoteItem item, final BoardItemClickListener listener) {
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    listener.onItemClicked(item, getAdapterPosition());
                }
            });

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    return listener.onItemLongClicked(item, getAdapterPosition());
                }
            });
        }
    }

    /**************************************************
     * VIEW CALLBACKS
     **************************************************/

    public BoardItemAdapter(Context ctx, List<BoardItem> models, BoardItemClickListener clickListener) {
        ComponentInjector.INSTANCE.getApplicationComponent().inject(this);

        context = ctx;
        items = models;
        listener = clickListener;
        staleItems = new ArrayList<>();
        selectedItems = new SparseBooleanArray();
        setHasStableIds(true);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == BoardItem.TYPE_EVENT) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.board_item_event, parent, false);
            return new EventHolder(view);
        }
        else if (viewType == BoardItem.TYPE_FILE) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.board_item_file, parent, false);
            return new FileHolder(view);
        }
        else if (viewType == BoardItem.TYPE_DRAWING) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.board_item_drawing, parent, false);
            return new DrawingHolder(view);
        }
        else if (viewType == BoardItem.TYPE_LINK) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.board_item_link, parent, false);
            return new LinkHolder(view);
        }
        else if (viewType == BoardItem.TYPE_LIST) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.board_item_list, parent, false);
            ListHolder holder = new ListHolder(view);

            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
            layoutManager.setAutoMeasureEnabled(true);
            holder.listView.setLayoutManager(layoutManager);

            SimpleListItemAdapter adapter = new SimpleListItemAdapter(context, new ArrayList<CheckedItem>());
            holder.listView.setAdapter(adapter);

            return holder;
        }
        else if (viewType == BoardItem.TYPE_NOTE) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.board_item_note, parent, false);
            return new NoteHolder(view);
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
        else if (recyclerViewHolder instanceof LinkHolder)
            setLinkViewHolder(position, recyclerViewHolder);
        else if (recyclerViewHolder instanceof ListHolder)
            setListViewHolder(position, recyclerViewHolder);
        else if (recyclerViewHolder instanceof NoteHolder)
            setNoteViewHolder(position, recyclerViewHolder);
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
            else if (item.getType() == BoardItem.TYPE_LINK) {
                LinkItem link = (LinkItem) item;
                String url = TextUtils.isEmpty(link.getUrl()) ? "" : link.getUrl();
                String title = TextUtils.isEmpty(link.getTitle()) ? "" : link.getTitle();
                String thumbnail = TextUtils.isEmpty(link.getThumbnail()) ? "" : link.getThumbnail();
                String description = TextUtils.isEmpty(link.getDescription()) ? "" : link.getDescription();
                id = (link.getId() + url + title + thumbnail + description).hashCode();
            }
            else if (item.getType() == BoardItem.TYPE_LIST) {
                ListItem list = (ListItem) item;
                String title = TextUtils.isEmpty(list.getTitle()) ? "" : list.getTitle();
                int size = list.getItems() == null ? 0 : list.getItems().size();
                long modified = list.getUpdatedTime().getMillis();
                id = (title + size + modified).hashCode();
            }
            else if (item.getType() == BoardItem.TYPE_NOTE) {
                NoteItem note = (NoteItem) item;
                String title = TextUtils.isEmpty(note.getTitle()) ? "" : note.getTitle();
                String text = TextUtils.isEmpty(note.getText()) ? "" : note.getText();
                id = (title + text).hashCode();
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

    /**************************************************
     * MODEL UPDATES
     **************************************************/

    public void addItem(String id) {
        notifyDataSetChanged();
    }

    public void updateItem(String id) {
        notifyDataSetChanged();
    }

    public void deleteItem(String id) {
        notifyDataSetChanged();
    }

    public void update(List<BoardItem> items) {
        // update from specific listView of items
        if (items != null) {
            this.items = items;
            notifyDataSetChanged();
        }
        else {
            for (int position : staleItems) {
                notifyItemChanged(position);
            }
            staleItems.clear();
        }
    }

    /**************************************************
     * UI HELPERS
     **************************************************/

    private void attachPostInfo(FeedAction feedAction, TextView posterName, ImageView posterAvatar, TextView postTime) {
        if (feedAction != null) {
            if (feedAction.user != null) {

                // set update text
                switch(feedAction.type) {
                    case FeedAction.CREATE:
                        posterName.setText(Html.fromHtml("<b>" + feedAction.user.getName() + "</b> " +
                                context.getResources().getString(R.string.card_post_info)));
                        break;
                    case FeedAction.CANCELED:
                        posterName.setText(Html.fromHtml("<b>" + feedAction.user.getName() + "</b> " +
                                context.getResources().getString(R.string.card_cancel_info)));
                        break;
                    case FeedAction.EDITED:
                        posterName.setText(Html.fromHtml("<b>" + feedAction.user.getName() + "</b> " +
                                context.getResources().getString(R.string.card_edit_info)));
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

    private void attachSelectionOverlay(int position, CardView card) {
        card.setCardBackgroundColor(selectedItems.get(position) ? ContextCompat.getColor(context, R.color.selectItemOverlay) :
                Color.WHITE);
    }

    private void attachSyncStatus(BoardItem item, ImageView syncIcon) {
        if (item.getSyncStatus() == BoardItem.SYNC_COMPLETE)
            syncIcon.setVisibility(View.INVISIBLE);
        else {
            syncIcon.setVisibility(View.VISIBLE);

            if (item.getSyncStatus() == BoardItem.SYNC_ERROR)
                syncIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_sync_failed));
            else if (item.getSyncStatus() == BoardItem.SYNC_IN_PROGRESS)
                syncIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_sync));
            else if (item.getSyncStatus() == BoardItem.NO_SYNC)
                syncIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_sync_off));
        }
    }

    private void attachSyncStatusWithProgress(BoardItem item, ImageView syncIcon, ProgressBar progressBar) {
        if (item.getSyncStatus() == BoardItem.SYNC_COMPLETE) {
            syncIcon.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.GONE);
        }
        else {
            syncIcon.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.VISIBLE);

            if (item.getSyncStatus() == BoardItem.SYNC_ERROR)
                syncIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_sync_failed));
            else if (item.getSyncStatus() == BoardItem.SYNC_IN_PROGRESS) {
                syncIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_sync));
                if (item.getProgress() > 0) {
                    progressBar.setIndeterminate(false);
                    progressBar.setProgress(item.getProgress());
                }
                else {
                    progressBar.setIndeterminate(true);
                }
            }
            else if (item.getSyncStatus() == BoardItem.NO_SYNC) {
                syncIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_sync_off));
            }
        }
    }

    private void setFileViewHolder(int position, RecyclerView.ViewHolder recyclerViewHolder) {
        FileItem file = (FileItem) items.get(position);
        final FileHolder holder = (FileHolder) recyclerViewHolder;

        holder.setClickListener(file, listener);

        attachSelectionOverlay(position, holder.cardView);

        if (file.getLocalCache() == null || !file.getLocalCache().exists()) {
            holder.viewButton.setText(context.getResources().getString(R.string.card_action_download));
            holder.viewButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    holder.itemView.performClick();
                }
            });
        }
        else {
            holder.viewButton.setText(context.getResources().getString(R.string.card_action_view));
            holder.viewButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    holder.itemView.performClick();
                }
            });
        }

        String name = !TextUtils.isEmpty(file.getName()) ? file.getName()
                : context.getResources().getString(R.string.file_name_placeholder);
        holder.fileName.setText(name);
        String note = !TextUtils.isEmpty(file.getName()) ? file.getNote()
                : "";
        holder.fileNote.setText(note);

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
            icon = file.isImage() ? R.drawable.ic_placeholder_image : R.drawable.ic_placeholder_file;
            Glide.with(context)
                    .load(icon).centerCrop()
                    .crossFade(1000)
                    .into(holder.fileThumbnail);
        }
    }

    private void setEventViewHolder(int position, RecyclerView.ViewHolder recyclerViewHolder) {
        final EventItem item = (EventItem) items.get(position);
        final EventHolder holder = (EventHolder) recyclerViewHolder;

        holder.setClickListener(item, listener);

        attachSelectionOverlay(position, holder.cardView);

        if (item.getPlace() != null || item.getLocation() != null) {
            holder.mapButton.setVisibility(View.VISIBLE);
            holder.mapButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) listener.onActionButtonClicked(item, R.id.action_get_directions);
                }
            });
        }
        else {
            holder.mapButton.setVisibility(View.GONE);
        }

        holder.nameText.setText(item.getName());

        if (item.getStartTime() != null) {
            String formattedDateTime = DateUtils.getFormattedDate(context, item.getStartTime(), item.getEndTime());
            String formattedDuration = DateUtils.getFormattedTimeFromNow(context, item.getStartTime());
            holder.timeText.setText(String.format(context.getResources().getString(R.string.card_event_time),
                    formattedDateTime, formattedDuration));
        }
        else {
            holder.timeText.setText(context.getResources().getString(R.string.date_to_be_determined));
        }

        if (!TextUtils.isEmpty(item.getPlace())) {
            String placeName = item.getLocation() != null ?
                    item.getLocation().getLatitude() + ", " + item.getLocation().getLongitude() :
                    context.getResources().getString(R.string.notify_retrieving_place_info);
            TaskUtils.getLocationFromPlaceId(appState.getGoogleApiClient(), item.getPlace(), new TaskUtils.OnLocationReceivedListener() {
                @Override
                public void onLocationReceived(List<CharSequence> locations) {
                    if (!locations.isEmpty()) {
                        holder.locationText.setText(locations.get(0));
                        holder.googleAttrImage.setVisibility(View.VISIBLE);

                        int updatedEventIndex = staleItems.indexOf(holder.getAdapterPosition());
                        if (updatedEventIndex >= 0 && updatedEventIndex < staleItems.size())
                            staleItems.remove(updatedEventIndex);
                    }
                    else {
                        holder.locationText.setText(context.getResources().getString(R.string.date_to_be_determined));
                        holder.googleAttrImage.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onLocationFailed() {
                    holder.locationText.setText(context.getResources().getString(R.string.date_to_be_determined));
                    holder.googleAttrImage.setVisibility(View.GONE);
                }
            });
            holder.locationText.setText(placeName);
            holder.googleAttrImage.setVisibility(View.GONE);

            // add to listView of items that contain places info to be refreshed
            staleItems.add(holder.getAdapterPosition());
        }
        else {
            if (item.getLocation() != null) {
                holder.locationText.setText(item.getLocation().getLatitude() + ", " +
                        item.getLocation().getLongitude());
            }
            else {
                holder.locationText.setText(context.getResources().getString(R.string.date_to_be_determined));
            }
            holder.googleAttrImage.setVisibility(View.GONE);
        }

        holder.noteText.setText(item.getNote());
        if (TextUtils.isEmpty(item.getNote())) holder.noteText.setVisibility(View.GONE);
        else holder.noteText.setVisibility(View.VISIBLE);
    }

    private void setDrawingViewHolder(int position, RecyclerView.ViewHolder recyclerViewHolder) {
        DrawItem drawing = (DrawItem) items.get(position);
        final DrawingHolder holder = (DrawingHolder) recyclerViewHolder;

        holder.setOnClickListener(drawing, listener);

        attachSelectionOverlay(position, holder.cardView);

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

    private void setLinkViewHolder(int position, RecyclerView.ViewHolder recyclerViewHolder) {
        final LinkItem link = (LinkItem) items.get(position);
        final LinkHolder holder = (LinkHolder) recyclerViewHolder;

        holder.setClickListener(link, listener);

        attachSelectionOverlay(position, holder.cardView);

        holder.url.setText(trimUrl(link.getUrl()));
        if (TextUtils.isEmpty(link.getTitle()) || link.getTitle().equals("null")) {
            holder.title.setVisibility(View.GONE);
            holder.title.setText(null);
        }
        else {
            holder.title.setVisibility(View.VISIBLE);
            holder.title.setText(link.getTitle());
        }
        if (TextUtils.isEmpty(link.getDescription()) || link.getDescription().equals("null")) {
            holder.description.setVisibility(View.GONE);
            holder.description.setText(null);
        }
        else {
            holder.description.setVisibility(View.VISIBLE);
            holder.description.setText(link.getDescription());
        }
        if (TextUtils.isEmpty(link.getThumbnail()) || link.getThumbnail().equals("null")) {
            holder.thumbnailLayout.setVisibility(View.GONE);
        }
        else {
            holder.thumbnailLayout.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(link.getThumbnail()).centerCrop()
                    .crossFade(1000)
                    .placeholder(R.drawable.ic_placeholder_image)
                    .error(R.drawable.ic_placeholder_image)
                    .into(holder.thumbnail);
        }

        holder.editButton.setText(context.getString(R.string.card_action_edit_link));
        holder.editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onActionButtonClicked(link, R.id.action_edit_link);
            }
        });
        holder.copyButton.setText(context.getString(R.string.card_action_copy_link));
        holder.copyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onActionButtonClicked(link, R.id.action_copy_link);
            }
        });
    }

    private void setListViewHolder(int position, RecyclerView.ViewHolder recyclerViewHolder) {
        final ListItem list = (ListItem) items.get(position);
        final ListHolder holder = (ListHolder) recyclerViewHolder;

        holder.setClickListener(list, listener);

        attachSelectionOverlay(position, holder.cardView);

        holder.titleView.setVisibility(TextUtils.isEmpty(list.getTitle()) ? View.GONE : View.VISIBLE);
        holder.titleView.setText(list.getTitle());
        SimpleListItemAdapter adapter = (SimpleListItemAdapter) holder.listView.getAdapter();
        adapter.setItems(list.getItems());
        adapter.notifyDataSetChanged();
    }

    private void setNoteViewHolder(int position, RecyclerView.ViewHolder recyclerViewHolder) {
        final NoteItem note = (NoteItem) items.get(position);
        final NoteHolder holder = (NoteHolder) recyclerViewHolder;

        holder.setClickListener(note, listener);

        attachSelectionOverlay(position, holder.cardView);

        holder.titleText.setVisibility(TextUtils.isEmpty(note.getTitle()) ? View.GONE : View.VISIBLE);
        holder.titleText.setText(note.getTitle());
        holder.contentText.setVisibility(TextUtils.isEmpty(note.getText()) ? View.GONE : View.VISIBLE);
        holder.contentText.setText(note.getText());
    }

    private String trimUrl(String url) {
        if (TextUtils.isEmpty(url)) return null;
        return Uri.parse(url).getHost();
    }

    /**************************************************************
     * MULTIPLE ITEM SELECTION LISTENER
     **************************************************************/

    @Override
    public void toggleSelection(int pos) {
        if (selectedItems.get(pos, false)) {
            Log.d(TAG, "Unselected item at " + pos);
            selectedItems.delete(pos);
        }
        else {
            Log.d(TAG, "Selected item at " + pos);
            selectedItems.put(pos, true);
        }
        Log.d(TAG, "Total selections is " + getSelectedItemCount());
        notifyItemChanged(pos);
    }

    @Override
    public void clearSelections() {
        Log.d(TAG, "Clear all item selections");
        selectedItems.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getSelectedItemCount() {
        return selectedItems.size();
    }

    @Override
    public List<Integer> getSelectedItems() {
        List<Integer> items = new ArrayList<>(selectedItems.size());
        for (int i = 0; i < selectedItems.size(); i++) {
            items.add(selectedItems.keyAt(i));
        }
        return items;
    }
}