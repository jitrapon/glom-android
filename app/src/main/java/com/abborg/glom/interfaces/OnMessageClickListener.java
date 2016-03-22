package com.abborg.glom.interfaces;

import com.abborg.glom.model.BaseChatMessage;

public interface OnMessageClickListener {

    void onMessageClicked(BaseChatMessage message);

    void onMessageLongClicked(BaseChatMessage message);
}
