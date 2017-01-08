package com.abborg.glom.adapters;

import android.content.Context;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.abborg.glom.Const;
import com.abborg.glom.R;
import com.abborg.glom.model.ExploreItem;
import com.abborg.glom.model.EmptyItem;
import com.abborg.glom.model.MovieItem;
import com.abborg.glom.model.WatchableImage;
import com.abborg.glom.model.WatchableRating;
import com.abborg.glom.model.WatchableVideo;
import com.afollestad.sectionedrecyclerview.SectionedRecyclerViewAdapter;
import com.bumptech.glide.Glide;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExploreRecyclerViewAdapter
        extends SectionedRecyclerViewAdapter<RecyclerView.ViewHolder> {

    private static final String TAG = "DiscoverAdapter";

    private Context context;

    private Handler handler;

    private DateTimeFormatter dateFormatter;

    /** the main model in this adapter, maps the item type to listView of the items so we can easily
     * retrieve the item listView based on their types
     */
    private Map<Integer, List<ExploreItem>> items;

    private static final String YOUTUBE_VIDEO_ID_REGEX =
            "http(?:s)?://(?:www\\.)?youtu(?:\\.be/|be\\.com/(?:watch\\?v=|v/|embed/|user/(?:[\\w#]+/)+))([^&#?\n]+)";
    private Pattern youtubePattern;

    /** Maps section to view types **/
    private static final int[] sections = {
            ExploreItem.TYPE_MOVIE,
            ExploreItem.TYPE_EVENT,
            ExploreItem.TYPE_FOOD
    };

    public ExploreRecyclerViewAdapter(Context context, Handler handler) {
        this.context = context;
        this.handler = handler;

        // initialize with empty item object in each listView
        // the section will not be visible if there is no object in the listView
        items = new HashMap<>(sections.length);
        for (Integer type : sections) {
            List<ExploreItem> list = new ArrayList<>();
            list.add(new EmptyItem());
            items.put(type, list);
        }

        dateFormatter = DateTimeFormat.forPattern("dd MMM yyyy");

        youtubePattern = Pattern.compile(YOUTUBE_VIDEO_ID_REGEX);
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
        List<ExploreItem> items = getItems(section);
        return items == null ? 0 : items.size();
    }

    @Override
    public void onBindHeaderViewHolder(RecyclerView.ViewHolder viewHolder, int section) {
        SectionHeaderViewHolder holder = (SectionHeaderViewHolder) viewHolder;
        if (sections[section] == ExploreItem.TYPE_MOVIE)
            holder.title.setText(context.getResources().getString(R.string.discover_section_header_movie));
        else if (sections[section] == ExploreItem.TYPE_EVENT)
            holder.title.setText(context.getResources().getString(R.string.discover_section_header_event));
        else if (sections[section] == ExploreItem.TYPE_FOOD)
            holder.title.setText(context.getResources().getString(R.string.discover_section_header_food));
    }

    private List<ExploreItem> getItems(int section) {
        return items.get(sections[section]);
    }

    private ExploreItem getItem(int section, int sectionPosition) {
        List<ExploreItem> items = getItems(section);
        if (items != null && !items.isEmpty()) return items.get(sectionPosition);
        else return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder,
                                 int section, int sectionPosition, int absolutePosition) {
        final String emptyText = context.getResources().getString(R.string.discover_item_empty);
        final String unsupportedActionText = context.getResources().getString(R.string.error_unsupported_action);
        final String noTrailerText = context.getResources().getString(R.string.error_no_trailer);
        ExploreItem item = getItem(section, sectionPosition);

        // movies!
        if (viewHolder instanceof MovieItemViewHolder) {
            MovieItemViewHolder holder = (MovieItemViewHolder) viewHolder;
            if (item != null) {

                // when there is an actual movie to display
                if (item instanceof MovieItem) {
                    final MovieItem movie = (MovieItem) item;
                    String placeholder = context.getResources().getString(R.string.empty_placeholder);
                    holder.title.setText(movie.getTitle());
                    holder.releaseDate.setText(dateFormatter.print(movie.getReleaseDate()));
                    holder.lang.setText(movie.getLang());
                    List<WatchableRating> ratings = movie.getRatings();
                    if (ratings != null && !ratings.isEmpty()) {
                        holder.ratingStars.setRating((float) (ratings.get(0).score / 10 * holder.ratingStars.getNumStars()));
                        holder.ratingInfo.setText(ratings.get(0).score + " " + ratings.get(0).source);
                    }
                    else {
                        holder.ratingStars.setRating(0);
                        holder.ratingInfo.setText(placeholder);
                    }
                    holder.director.setText(TextUtils.isEmpty(movie.getDirector()) ? placeholder : movie.getDirector());
                    holder.cast.setText(TextUtils.isEmpty(movie.getCast()) ? placeholder : movie.getCast());
                    List<WatchableImage> images = movie.getImages();
                    if (images != null && !images.isEmpty()) {
                        for (WatchableImage image : images) {
                            if (image.type == WatchableImage.TYPE_POSTER) {
                                Glide.with(context)
                                        .load(image.thumbnail).fitCenter()
                                        .placeholder(R.drawable.ic_placeholder_movie_poster)
                                        .error(R.drawable.ic_placeholder_movie_poster)
                                        .crossFade(1000)
                                        .into(holder.poster);

                                break;
                            }
                        }
                    }
                    holder.bookBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Toast.makeText(v.getContext(), unsupportedActionText, Toast.LENGTH_LONG).show();
                        }
                    });
                    holder.trailerBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            List<WatchableVideo> videos = movie.getVideos();
                            if (videos != null && !videos.isEmpty()) {
                                for (WatchableVideo video : videos) {
                                    if (video.type == WatchableVideo.TYPE_TRAILER) {
                                        String id = getYoutubeVideoId(video.url);
                                        if (handler != null) {
                                            handler.sendMessage(handler.obtainMessage(Const.MSG_PLAY_YOUTUBE_VIDEO, id));
                                        }
                                        break;
                                    }
                                }
                            }
                            else {
                                Toast.makeText(v.getContext(), noTrailerText, Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                }

                // when there is nothing to display
                else {
                    holder.title.setText(emptyText);
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
        ExploreItem item = getItem(section, sectionPosition);
        if (item == null || item instanceof EmptyItem) return ExploreItem.TYPE_EMPTY;
        else {
            if (item instanceof MovieItem) return ExploreItem.TYPE_MOVIE;
            else return ExploreItem.TYPE_EMPTY;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEW_TYPE_HEADER: {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.section_header, parent, false);
                return new SectionHeaderViewHolder(view);
            }
            case ExploreItem.TYPE_MOVIE: {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.card_movie, parent, false);
                return new MovieItemViewHolder(view);
            }
            case ExploreItem.TYPE_EVENT:
            case ExploreItem.TYPE_FOOD:
            default:
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.discover_item_empty, parent, false);
                return new EmptyItemViewHolder(view);
        }
    }

    /********************
     * VIEW HOLDERS
     *******************/

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

    /**
     * Card holding movie information
     */
    public static class MovieItemViewHolder extends RecyclerView.ViewHolder {

        TextView title;
        TextView releaseDate;
        TextView lang;
        RatingBar ratingStars;
        TextView ratingInfo;
        TextView director;
        TextView cast;
        ImageView poster;
        Button bookBtn;
        Button trailerBtn;

        public MovieItemViewHolder(View itemView) {
            super(itemView);

            title = (TextView) itemView.findViewById(R.id.movie_title);
            releaseDate = (TextView) itemView.findViewById(R.id.movie_release_date);
            lang = (TextView) itemView.findViewById(R.id.movie_lang);
            ratingStars =  (RatingBar) itemView.findViewById(R.id.movie_rating_stars);
            ratingInfo = (TextView) itemView.findViewById(R.id.movie_rating_number);
            director = (TextView) itemView.findViewById(R.id.movie_director);
            cast = (TextView) itemView.findViewById(R.id.movie_cast);
            poster = (ImageView) itemView.findViewById(R.id.movie_poster);
            bookBtn = (Button) itemView.findViewById(R.id.card_action_book);
            trailerBtn = (Button) itemView.findViewById(R.id.card_action_watch_trailer);
        }
    }

    /**
     * Generic empty message when no items to display in a section
     */
    public static class EmptyItemViewHolder extends RecyclerView.ViewHolder {

        TextView title;

        public EmptyItemViewHolder(View itemView) {
            super(itemView);

            title = (TextView) itemView.findViewById(R.id.item_title);
        }
    }

    /********************
     * HELPERS
     *******************/

    public void update(int type, List<ExploreItem> newList) {
        if (newList == null || newList.isEmpty()) {
            newList = new ArrayList<>();
            newList.add(new EmptyItem());
        }

        List<ExploreItem> prevList = items.get(type);
        if (prevList != null) {
            prevList.clear();
            prevList.addAll(newList);
        }
        else prevList = newList;
        items.put(type, prevList);

        notifyDataSetChanged();
    }


    private String getYoutubeVideoId(String url) {
        if (!TextUtils.isEmpty(url)) {
            Matcher regexMatcher = youtubePattern.matcher(url);
            if (regexMatcher.find()) {
                return regexMatcher.group(1);
            }
            return null;
        }
        return null;
    }
}
