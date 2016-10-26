package com.abborg.glom.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.abborg.glom.ApplicationState;
import com.abborg.glom.Const;
import com.abborg.glom.R;
import com.abborg.glom.adapters.ChatMessageAdapter;
import com.abborg.glom.data.DataProvider;
import com.abborg.glom.di.ComponentInjector;
import com.abborg.glom.interfaces.OnMessageClickListener;
import com.abborg.glom.model.BaseChatMessage;
import com.abborg.glom.model.ChatMessage;
import com.abborg.glom.model.Circle;
import com.abborg.glom.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import static com.abborg.glom.model.BaseChatMessage.OutgoingStatus;

public class ChatActivity extends AppCompatActivity implements
        View.OnClickListener,
        Handler.Callback,
        OnMessageClickListener {

    @Inject
    ApplicationState appState;

    @Inject
    DataProvider dataProvider;

    private Circle circle;

    private User user;

    private List<BaseChatMessage> messages;

    /** UI elements **/
    private static final String TAG = "ChatActivity";
    private RecyclerView chatView;
    private Button sendMessageBtn;
    private EditText editMessageText;
    private ImageView addMediaBtn;

    private ChatMessageAdapter adapter;

    private Handler handler;

    private BroadcastReceiver broadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ComponentInjector.INSTANCE.getApplicationComponent().inject(this);

        handler = new Handler(this);

        circle = appState.getActiveCircle();
        user = appState.getActiveUser();
        messages = new ArrayList<>();

        setContentView(R.layout.activity_chat);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(circle.getTitle());
        }

        chatView = (RecyclerView) findViewById(R.id.chat_recyclerview);
        sendMessageBtn = (Button) findViewById(R.id.send_btn);
        editMessageText = (EditText) findViewById(R.id.edit_text_msg);
        addMediaBtn = (ImageView) findViewById(R.id.add_media_btn);
        chatView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        chatView.setLayoutManager(layoutManager);
        RecyclerView.ItemAnimator itemAnimator = chatView.getItemAnimator();
        if (itemAnimator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) itemAnimator).setSupportsChangeAnimations(false);
        }

        adapter = new ChatMessageAdapter(this, messages, this, circle, user);
        sendMessageBtn.setOnClickListener(this);
        addMediaBtn.setOnClickListener(this);
        chatView.setAdapter(adapter);

        broadcastReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {

                String circleId = intent.getStringExtra(getResources().getString(R.string.EXTRA_MESSAGE_CIRCLE_ID));
                if (circleId != null && circleId.equals(circle.getId())) {

                    // NEW MESSAGE INCOMING
                    if (intent.getAction().equals(getResources().getString(R.string.ACTION_NEW_MESSAGE))) {
                        BaseChatMessage chatMessage = null;
                        User sender = null;
                        String content = intent.getStringExtra(getResources().getString(R.string.EXTRA_MESSAGE_CONTENT));
                        String senderId = intent.getStringExtra(getResources().getString(R.string.EXTRA_MESSAGE_SENDER));
                        String messageId = intent.getStringExtra(getResources().getString(R.string.EXTRA_MESSAGE_ID));
                        String type = intent.getStringExtra(getResources().getString(R.string.EXTRA_MESSAGE_TYPE));

                        List<User> users = circle.getUsers();
                        if (users != null && !users.isEmpty()) {
                            for (User user : users) {
                                if (user.getId().equals(senderId)) {
                                    sender = user;
                                    break;
                                }
                            }
                        }
                        if (sender != null && TextUtils.equals(circleId, circle.getId())) {
                            if (TextUtils.equals(type, Const.JSON_VALUE_MESSAGE_TYPE_TEXT)) {
                                chatMessage = new ChatMessage(messageId, content, sender, false);
                            }

                            // add the message to the adapter
                            addMessage(chatMessage);
                        }
                    }

                    // ACK MESSAGE
                    else if (intent.getAction().equals(getResources().getString(R.string.ACTION_SERVER_ACK_MESSAGE))) {
                        String messageId = intent.getStringExtra(getResources().getString(R.string.EXTRA_MESSAGE_ID));
                        setOutgoingStatus(messageId, OutgoingStatus.SERVER_RECEIVED);
                    }
                }
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();

        registerBroadcastReceiver(broadcastReceiver);
    }

    private void registerBroadcastReceiver(BroadcastReceiver receiver) {
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(getResources().getString(R.string.ACTION_NEW_MESSAGE));
        intentFilter.addAction(getResources().getString(R.string.ACTION_SERVER_ACK_MESSAGE));
        broadcastManager.registerReceiver(receiver, intentFilter);
    }

    @Override
    protected void onStop() {
        // unregister the local broadcast receiver
        if (broadcastReceiver != null) {
            LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
            broadcastManager.unregisterReceiver(broadcastReceiver);
        }

        super.onStop();
    }

    /***************************************
     * On Button clicked
     ***************************************/

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.send_btn: {
                Log.d(TAG, "Send message clicked");
                String message = editMessageText.getText().toString();
                if (!TextUtils.isEmpty(message)) {
                    sendTextMessage(message);
                    editMessageText.setText("");
                }
                break;
            }
            case R.id.add_media_btn:
                Log.d(TAG, "Add media clicked");
                break;
            default: break;
        }
    }

    /***********************************************************************
     * HELPER METHODS ON MESSAGES
     ***********************************************************************/
    private String generateMessageId() {
        return UUID.randomUUID().toString();
    }

    private void sendTextMessage(String content) {
        String id = generateMessageId();
        ChatMessage message = new ChatMessage(id, content, user, true);
        addMessage(message);
        dataProvider.sendUpstreamMessage(message);
    }

    private synchronized void addMessage(BaseChatMessage message) {
        messages.add(message);
        int lastPos = messages.size() - 1;
        adapter.notifyItemInserted(lastPos);
        chatView.scrollToPosition(lastPos);
    }

    private synchronized int getMessagePosition(String id) {
        if (messages != null && !messages.isEmpty()) {
            for (int i = 0; i < messages.size(); i++) {
                if (messages.get(i).getId().equals(id)) return i;
            }
        }
        return -1;
    }

    private synchronized void setOutgoingStatus(String id, OutgoingStatus status) {
        int position = getMessagePosition(id);
        if (position != -1) {
            BaseChatMessage message = messages.get(position);
            if (message != null) {
                message.setOutgoingStatus(status);
                adapter.notifyItemChanged(position);
            }
        }
    }

    /***********************************************************************
     * HANDLERS
     ***********************************************************************/

    @Override
    public boolean handleMessage(Message msg) {
        return false;
    }

    @Override
    public void onMessageClicked(BaseChatMessage message) {
        Log.d(TAG, "Clicked message is " + message.getContent());
    }

    @Override
    public void onMessageLongClicked(BaseChatMessage message) {
        Log.d(TAG, "Long-clicked message is " + message.getContent());
    }
}
