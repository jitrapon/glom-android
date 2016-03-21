package com.abborg.glom.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.abborg.glom.AppState;
import com.abborg.glom.R;
import com.abborg.glom.adapters.ChatMessageAdapter;
import com.abborg.glom.data.DataUpdater;
import com.abborg.glom.interfaces.OnMessageClickListener;
import com.abborg.glom.model.ChatMessage;
import com.abborg.glom.model.Circle;
import com.abborg.glom.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ChatActivity extends AppCompatActivity implements View.OnClickListener, Handler.Callback,
        OnMessageClickListener {

    /** Circle state information **/
    AppState appState;
    Circle circle;
    User user;
    DataUpdater dataUpdater;

    List<ChatMessage> messages;

    /** UI elements **/
    private static final String TAG = "ChatActivity";
    private RecyclerView chatView;
    private Button sendMessageBtn;
    private EditText editMessageText;
    private ImageView addMediaBtn;

    private ChatMessageAdapter adapter;

    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        handler = new Handler(this);

        //TODO load circle info if null
        appState = AppState.getInstance();
        if (appState == null || appState.getDataUpdater() == null) {
            finish();
        }
        circle = appState.getActiveCircle();
        user = appState.getActiveUser();
        dataUpdater = appState.getDataUpdater();
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

        adapter = new ChatMessageAdapter(this, messages, this, circle, user);
        sendMessageBtn.setOnClickListener(this);
        addMediaBtn.setOnClickListener(this);
        chatView.setAdapter(adapter);
    }

    /***************************************
     * On Button clicked
     ***************************************/

    @Override
    public void onClick(View v) {
        Log.d(TAG, "Clicked event");
        switch (v.getId()) {
            case R.id.send_btn: {
                Log.d(TAG, "Send message clicked");
                String message = editMessageText.getText().toString();
                if (!TextUtils.isEmpty(message)) {
                    sendMessage(message);
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
    private void sendMessage(String content) {
        ChatMessage message = new ChatMessage(content, true);
        addMessage(message);
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                botReply();
//            }
//        }, 500);

        dataUpdater.sendUpstreamMessage(content);
    }

    private synchronized void addMessage(ChatMessage message) {
        messages.add(message);
        int lastPos = messages.size() - 1;
        adapter.notifyItemInserted(lastPos);
        chatView.scrollToPosition(lastPos);
    }

    private void botReply() {
        String[] replies = {"Hello!","Hi there","What's up?","Weather is good today, isn't it?",
                "I'm doing good as well, we should catch up some time","wtf you want bro","Nothing much","Really bored right now"};
        int random = new Random().nextInt(replies.length);
        ChatMessage chatMessage = new ChatMessage(replies[random], false);
        addMessage(chatMessage);
    }

    /***********************************************************************
     * HANDLERS
     ***********************************************************************/

    @Override
    public boolean handleMessage(Message msg) {
        return false;
    }

    @Override
    public void onMessageClicked(ChatMessage message) {
        Log.d(TAG, "Clicked message is " + message.getContent());
    }

    @Override
    public void onMessageLongClicked(ChatMessage message) {
        Log.d(TAG, "Long-clicked message is " + message.getContent());
    }
}
