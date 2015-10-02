package com.abborg.glom.ui;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;

import com.oguzdev.circularfloatingactionmenu.library.FloatingActionButton;

/**
 * Created by Boat on 1/10/58.
 */
public class AvatarActionButton extends FloatingActionButton {

    public AvatarActionButton(Activity activity, FloatingActionButton.LayoutParams layoutParams, int theme,
                              Drawable backgroundDrawable, int position, View contentView,
                              LayoutParams contentParams) {
        super(activity, layoutParams, theme, backgroundDrawable, position, contentView, contentParams);
    }


    @Override
    public void setPosition(int position, android.widget.FrameLayout.LayoutParams layoutParams) {
        super.setPosition(position, layoutParams);
        LayoutParams lp = (LayoutParams) this.getLayoutParams();
        lp.gravity = Gravity.CENTER;
        this.setLayoutParams(lp);
    }

    public static class Builder {
        private Activity activity;
        private FloatingActionButton.LayoutParams layoutParams;
        private int theme;
        private Drawable backgroundDrawable;
        private int position;
        private View contentView;
        private FloatingActionButton.LayoutParams contentParams;

        public Builder(Activity activity) {
            this.activity = activity;
            int size = activity.getResources().getDimensionPixelSize(com.oguzdev.circularfloatingactionmenu.library.R.dimen.action_button_size);
            int margin = activity.getResources().getDimensionPixelSize(com.oguzdev.circularfloatingactionmenu.library.R.dimen.action_button_margin);
            FloatingActionButton.LayoutParams layoutParams = new FloatingActionButton.LayoutParams(size, size, 85);
            layoutParams.setMargins(margin, margin, margin, margin);
            this.setLayoutParams(layoutParams);
            this.setTheme(0);
            this.setPosition(4);
        }

        public AvatarActionButton.Builder setLayoutParams(FloatingActionButton.LayoutParams params) {
            this.layoutParams = params;
            return this;
        }

        public AvatarActionButton.Builder setTheme(int theme) {
            this.theme = theme;
            return this;
        }

        public AvatarActionButton.Builder setBackgroundDrawable(Drawable backgroundDrawable) {
            this.backgroundDrawable = backgroundDrawable;
            return this;
        }

        public AvatarActionButton.Builder setBackgroundDrawable(int drawableId) {
            return this.setBackgroundDrawable(this.activity.getResources().getDrawable(drawableId));
        }

        public AvatarActionButton.Builder setPosition(int position) {
            this.position = position;
            return this;
        }

        public AvatarActionButton.Builder setContentView(View contentView) {
            return this.setContentView(contentView, (FloatingActionButton.LayoutParams)null);
        }

        public AvatarActionButton.Builder setContentView(View contentView, FloatingActionButton.LayoutParams contentParams) {
            this.contentView = contentView;
            this.contentParams = contentParams;
            return this;
        }

        public AvatarActionButton build() {
            return new AvatarActionButton(this.activity, this.layoutParams, this.theme, this.backgroundDrawable, this.position, this.contentView, this.contentParams);
        }
    }
}
