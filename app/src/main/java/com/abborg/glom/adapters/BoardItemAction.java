package com.abborg.glom.adapters;

import com.abborg.glom.R;

public enum BoardItemAction {

    IMAGE(R.drawable.ic_vector_image, R.string.action_image),
    VIDEO(R.drawable.ic_vector_camera, R.string.action_video),
    AUDIO(R.drawable.ic_vector_audio, R.string.action_audio),
    EVENT(R.drawable.ic_vector_event, R.string.action_event),
    ALARM(R.drawable.ic_vector_alarm, R.string.action_alarm),
    NOTE(R.drawable.ic_vector_note, R.string.action_note),
    LIST(R.drawable.ic_vector_list, R.string.action_list),
    DRAW(R.drawable.ic_vector_pencil, R.string.action_draw),
    LOCATION(R.drawable.ic_vector_location, R.string.action_location),
    LINK(R.drawable.ic_vector_link, R.string.action_link);

    private final int icon;
    private final int label;

    BoardItemAction(int icon, int label) {
        this.icon = icon;
        this.label = label;
    }

    public int getIcon() { return icon; }
    public int getLabel() { return label; }
}
