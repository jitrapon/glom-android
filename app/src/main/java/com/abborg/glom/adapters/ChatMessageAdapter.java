package com.abborg.glom.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.abborg.glom.R;
import com.abborg.glom.interfaces.OnMessageClickListener;
import com.abborg.glom.model.ChatMessage;
import com.abborg.glom.model.Circle;
import com.abborg.glom.model.User;
import com.abborg.glom.utils.CircleTransform;
import com.bumptech.glide.Glide;

import java.util.List;

public class ChatMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "ChatMessageAdapter";
    private static final int TYPE_MY_MESSAGE = 1;
    private static final int TYPE_OTHER_MESSAGE = 2;

    private List<ChatMessage> messages;
    private Context context;
    private OnMessageClickListener onClickListener;
    private Circle circle;
    private User user;

    public static class MyMessageViewHolder extends RecyclerView.ViewHolder {

        TextView message;


        public MyMessageViewHolder(View itemView) {
            super(itemView);

            message = (TextView) itemView.findViewById(R.id.message);
        }
    }

    public static class OtherMessageViewHolder extends RecyclerView.ViewHolder {

        TextView message;
        ImageView avatar;

        public OtherMessageViewHolder(View itemView) {
            super(itemView);

            message = (TextView) itemView.findViewById(R.id.message);
            avatar = (ImageView) itemView.findViewById(R.id.sender_avatar);
        }
    }

    public ChatMessageAdapter(Context context, List<ChatMessage> messages, OnMessageClickListener onClickListener,
                              Circle circle, User user) {
        this.user = user;
        this.circle = circle;
        this.context = context;
        this.messages = messages;
        this.onClickListener = onClickListener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_MY_MESSAGE) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_my_message, parent, false);
            final MyMessageViewHolder holder = new MyMessageViewHolder(view);
            view.findViewById(R.id.message_view).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = holder.getAdapterPosition();
                    if (onClickListener != null && messages != null && !messages.isEmpty()
                            && position != RecyclerView.NO_POSITION) {
                        onClickListener.onMessageClicked(messages.get(position));
                    }
                }
            });
            return holder;
        }
        else if (viewType == TYPE_OTHER_MESSAGE) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_other_message, parent, false);
            final OtherMessageViewHolder holder = new OtherMessageViewHolder(view);
            view.findViewById(R.id.message_view).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = holder.getAdapterPosition();
                    if (onClickListener != null && messages != null && !messages.isEmpty()
                            && position != RecyclerView.NO_POSITION) {
                        onClickListener.onMessageClicked(messages.get(position));
                    }
                }
            });
            return holder;
        }
        else return null;
    }

    @Override
    public int getItemViewType(int position) {
        if (messages != null) {
            ChatMessage message = messages.get(position);
            if (message.isMine()) {
                return TYPE_MY_MESSAGE;
            }
            else {
                return TYPE_OTHER_MESSAGE;
            }
        }
        return 0;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        if (holder instanceof MyMessageViewHolder) {
            MyMessageViewHolder viewHolder = (MyMessageViewHolder) holder;
            viewHolder.message.setText(message.getContent());
        }
        else if (holder instanceof OtherMessageViewHolder) {
            OtherMessageViewHolder viewHolder = (OtherMessageViewHolder) holder;
            viewHolder.message.setText(message.getContent());
            Glide.with(context)
                    .load(user.getAvatar()).fitCenter()
                    .transform(new CircleTransform(context))
                    .override(38, 38)
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .crossFade(1000)
                    .into(viewHolder.avatar);
        }
    }

    @Override
    public int getItemCount() {
        return messages == null ? 0 : messages.size();
    }
}
