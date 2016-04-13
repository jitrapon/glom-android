package com.abborg.glom.model;

import com.abborg.glom.Const;

/**
 * A simple text message
 */
public class ChatMessage extends BaseChatMessage {

    public ChatMessage(String messageId, String message, User user, boolean mine) {
        super(messageId, message, user, mine);
    }

    @Override
    public String getType() {
        return Const.JSON_VALUE_MESSAGE_TYPE_TEXT;
    }
}