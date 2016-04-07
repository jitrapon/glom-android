package com.abborg.glom.adapters;

import android.content.Context;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.abborg.glom.R;
import com.abborg.glom.model.DiscoverItem;
import com.abborg.glom.model.EmptyItem;
import com.abborg.glom.model.Movie;
import com.afollestad.sectionedrecyclerview.SectionedRecyclerViewAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DiscoverRecyclerViewAdapter
        extends SectionedRecyclerViewAdapter<RecyclerView.ViewHolder> {

    private static final String TAG = "DiscoverAdapter";

    private Context context;

    private Handler handler;

    /** the main model in this adapter, maps the item type to list of the items so we can easily
     * retrieve the item list based on their types
     */
    private Map<Integer, List<DiscoverItem>> items;

    /** Maps section to view types **/
    private static final int[] sections = {
            DiscoverItem.TYPE_MOVIE,
            DiscoverItem.TYPE_EVENT,
            DiscoverItem.TYPE_FOOD
    };

    public DiscoverRecyclerViewAdapter(Context context, Handler handler) {
        this.context = context;
        this.handler = handler;

        // initialize with empty item object in each list
        // the section will not be visible if there is no object in the list
        items = new HashMap<>(sections.length);
        for (Integer type : sections) {
            List<DiscoverItem> list = new ArrayList<>();
            list.add(new EmptyItem());
            items.put(type, list);
        }
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
        List<DiscoverItem> items = getItems(section);
        if (items != null) Log.e(TAG, "[" + section + "] Item count for " + sections[section] + " is " + items.size());
        return items == null ? 0 : items.size();
    }

    @Override
    public void onBindHeaderViewHolder(RecyclerView.ViewHolder viewHolder, int section) {
        SectionHeaderViewHolder holder = (SectionHeaderViewHolder) viewHolder;
        if (sections[section] == DiscoverItem.TYPE_MOVIE)
            holder.sectionTitle.setText(context.getResources().getString(R.string.discover_section_header_movie));
        else if (sections[section] == DiscoverItem.TYPE_EVENT)
            holder.sectionTitle.setText(context.getResources().getString(R.string.discover_section_header_event));
        else if (sections[section] == DiscoverItem.TYPE_FOOD)
            holder.sectionTitle.setText(context.getResources().getString(R.string.discover_section_header_food));
    }

    private List<DiscoverItem> getItems(int section) {
        return items.get(sections[section]);
    }

    private DiscoverItem getItem(int section, int sectionPosition) {
        List<DiscoverItem> items = getItems(section);
        if (items != null && !items.isEmpty()) return items.get(sectionPosition);
        else return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder,
                                 int section, int sectionPosition, int absolutePosition) {
        String emptyText = context.getResources().getString(R.string.discover_item_empty);
        DiscoverItem item = getItem(section, sectionPosition);

        // movies!
        if (viewHolder instanceof MovieItemViewHolder) {
            MovieItemViewHolder holder = (MovieItemViewHolder) viewHolder;
            if (item != null) {

                // when there is an actual movie to display
                if (item instanceof Movie) {
                    Movie movie = (Movie) item;
                    holder.movieTitle.setText(movie.getTitle());
                }

                // when there is nothing to display
                else {
                    holder.movieTitle.setText(emptyText);
                }
            }
        }

        // nothing to show!
        else if (viewHolder instanceof EmptyItemViewHolder) {
            EmptyItemViewHolder holder = (EmptyItemViewHolder) viewHolder;
            holder.title.setText(emptyText);
        }
    }

    @Override
    public int getItemViewType(int section, int sectionPosition, int absolutePosition) {
        return sections[section];
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEW_TYPE_HEADER: {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.discover_section_header, parent, false);
                return new SectionHeaderViewHolder(view);
            }
            case DiscoverItem.TYPE_MOVIE: {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.discover_item_movie, parent, false);
                return new MovieItemViewHolder(view);
            }
            case DiscoverItem.TYPE_EVENT:
            case DiscoverItem.TYPE_FOOD:
            default:
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.discover_item_empty, parent, false);
                return new EmptyItemViewHolder(view);
        }
    }

    /********************
     * VIEW HOLDERS
     *******************/
    public static class SectionHeaderViewHolder extends RecyclerView.ViewHolder {

        TextView sectionTitle;

        public SectionHeaderViewHolder(View itemView) {
            super(itemView);

            sectionTitle = (TextView) itemView.findViewById(R.id.section_title_text);
        }
    }

    public static class MovieItemViewHolder extends RecyclerView.ViewHolder {

        TextView movieTitle;

        public MovieItemViewHolder(View itemView) {
            super(itemView);

            movieTitle = (TextView) itemView.findViewById(R.id.movie_title);
        }
    }

    public static class EmptyItemViewHolder extends RecyclerView.ViewHolder {

        TextView title;

        public EmptyItemViewHolder(View itemView) {
            super(itemView);

            title = (TextView) itemView.findViewById(R.id.item_title);
        }
    }

    /********************
     * MODEL CRUD OPERATIONS
     *******************/

    public void update(int type, List<DiscoverItem> newList) {
        if (newList == null || newList.isEmpty()) {
            newList = new ArrayList<>();
            newList.add(new EmptyItem());
        }

        List<DiscoverItem> prevList = items.get(type);
        if (prevList != null) {
            prevList.clear();
            prevList.addAll(newList);
        }
        else prevList = newList;
        Log.e(TAG, "Size for item " + type + " is " + prevList.size());
        items.put(type, prevList);

        notifyDataSetChanged();
    }
}
