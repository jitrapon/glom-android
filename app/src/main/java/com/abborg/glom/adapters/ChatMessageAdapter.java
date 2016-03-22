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
import com.abborg.glom.model.BaseChatMessage;
import com.abborg.glom.model.ChatMessage;
import com.abborg.glom.model.Circle;
import com.abborg.glom.model.User;
import com.abborg.glom.utils.CircleTransform;
import com.bumptech.glide.Glide;

import java.util.List;

import me.himanshusoni.chatmessageview.ChatMessageView;

public class ChatMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "ChatMessageAdapter";
    private static final int TYPE_MY_TEXT_MESSAGE = 1;
    private static final int TYPE_OTHER_TEXT_MESSAGE = 2;

    private List<BaseChatMessage> messages;
    private Context context;
    private OnMessageClickListener onClickListener;
    private Circle circle;
    private User user;

    public static class MyTextMessageViewHolder extends RecyclerView.ViewHolder {

        TextView message;


        public MyTextMessageViewHolder(View itemView) {
            super(itemView);

            message = (TextView) itemView.findViewById(R.id.message);
        }
    }

    public static class OtherTextMessageViewHolder extends RecyclerView.ViewHolder {

        TextView message;
        ImageView avatar;

        public OtherTextMessageViewHolder(View itemView) {
            super(itemView);

            message = (TextView) itemView.findViewById(R.id.message);
            avatar = (ImageView) itemView.findViewById(R.id.sender_avatar);
        }
    }

    public ChatMessageAdapter(Context context, List<BaseChatMessage> messages, OnMessageClickListener onClickListener,
                              Circle circle, User user) {
        this.user = user;
        this.circle = circle;
        this.context = context;
        this.messages = messages;
        this.onClickListener = onClickListener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_MY_TEXT_MESSAGE) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_my_message, parent, false);
            final MyTextMessageViewHolder holder = new MyTextMessageViewHolder(view);
            ChatMessageView chatView = (ChatMessageView) view.findViewById(R.id.message_view);
            chatView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = holder.getAdapterPosition();
                    if (onClickListener != null && messages != null && !messages.isEmpty()
                            && position != RecyclerView.NO_POSITION) {
                        onClickListener.onMessageClicked(messages.get(position));
                    }
                }
            });
            chatView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    int position = holder.getAdapterPosition();
                    if (onClickListener != null && messages != null && !messages.isEmpty()
                            && position != RecyclerView.NO_POSITION) {
                        onClickListener.onMessageLongClicked(messages.get(position));
                    }
                    return true;
                }
            });
            return holder;
        }
        else if (viewType == TYPE_OTHER_TEXT_MESSAGE) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_other_message, parent, false);
            final OtherTextMessageViewHolder holder = new OtherTextMessageViewHolder(view);
            ChatMessageView chatView = (ChatMessageView) view.findViewById(R.id.message_view);
            chatView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = holder.getAdapterPosition();
                    if (onClickListener != null && messages != null && !messages.isEmpty()
                            && position != RecyclerView.NO_POSITION) {
                        onClickListener.onMessageClicked(messages.get(position));
                    }
                }
            });
            chatView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    int position = holder.getAdapterPosition();
                    if (onClickListener != null && messages != null && !messages.isEmpty()
                            && position != RecyclerView.NO_POSITION) {
                        onClickListener.onMessageLongClicked(messages.get(position));
                    }
                    return true;
                }
            });
            return holder;
        }
        else return null;
    }

    @Override
    public int getItemViewType(int position) {
        if (messages != null) {
            BaseChatMessage message = messages.get(position);
            if (message.isMine()) {
                if (message instanceof ChatMessage) return TYPE_MY_TEXT_MESSAGE;
            }
            else {
                if (message instanceof ChatMessage) return TYPE_OTHER_TEXT_MESSAGE;
            }
        }
        return 0;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        BaseChatMessage message = messages.get(position);
        User sender = message.getSender();
        String avatar = sender==null ? "" : sender.getAvatar();
        if (holder instanceof MyTextMessageViewHolder) {
            MyTextMessageViewHolder viewHolder = (MyTextMessageViewHolder) holder;
            viewHolder.message.setText(message.getContent());
        }
        else if (holder instanceof OtherTextMessageViewHolder) {
            OtherTextMessageViewHolder viewHolder = (OtherTextMessageViewHolder) holder;
            viewHolder.message.setText(message.getContent());
            Glide.with(context)
                    .load(avatar).fitCenter()
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
