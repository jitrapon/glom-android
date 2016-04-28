package com.abborg.glom.adapters;

import android.animation.Animator;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
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
        ImageView outgoingStatusIcon;

        public MyTextMessageViewHolder(View itemView) {
            super(itemView);

            message = (TextView) itemView.findViewById(R.id.message);
            outgoingStatusIcon = (ImageView) itemView.findViewById(R.id.outgoing_status);
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
        BaseChatMessage.OutgoingStatus outgoingStatus = message.getOutgoingStatus();
        String avatar = sender==null ? "" : sender.getAvatar();
        if (holder instanceof MyTextMessageViewHolder) {
            MyTextMessageViewHolder viewHolder = (MyTextMessageViewHolder) holder;
            viewHolder.message.setText(message.getContent());
            switch(outgoingStatus) {
                case SERVER_RECEIVED:
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
//                        animateCircularReveal(viewHolder.outgoingStatusIcon);
                        viewHolder.outgoingStatusIcon.setVisibility(View.VISIBLE);
                        viewHolder.outgoingStatusIcon.setImageResource(R.drawable.ic_message_server_received);
                    }
                    else {
                        viewHolder.outgoingStatusIcon.setVisibility(View.VISIBLE);
                        viewHolder.outgoingStatusIcon.setImageResource(R.drawable.ic_message_server_received);
                    }
                    break;
                case SENDING:
                default:
                    viewHolder.outgoingStatusIcon.setVisibility(View.INVISIBLE);
            }
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

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void animateCircularReveal(final View view) {
        view.post(new Runnable() {
              @Override
              public void run() {
                  int cx = (view.getWidth()) / 2;
                  int cy = (view.getHeight()) / 2;
                  int radius = Math.max(view.getWidth(),
                          view.getHeight());
                  Animator anim = ViewAnimationUtils.createCircularReveal(view,
                          cx, cy, 0, radius);
                  anim.setStartDelay(1000);
                  anim.setDuration(3000);

                  view.setVisibility(View.VISIBLE);
                  anim.start();
              }
            }
        );
    }

    @Override
    public int getItemCount() {
        return messages == null ? 0 : messages.size();
    }
}
