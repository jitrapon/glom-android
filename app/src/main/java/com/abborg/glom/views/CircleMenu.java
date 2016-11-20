package com.abborg.glom.views;

import android.app.Activity;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.abborg.glom.R;
import com.abborg.glom.interfaces.CircleMenuListener;
import com.abborg.glom.model.MenuActionItem;
import com.abborg.glom.utils.CircleTransform;
import com.bumptech.glide.Glide;
import com.oguzdev.circularfloatingactionmenu.library.FloatingActionMenu;
import com.oguzdev.circularfloatingactionmenu.library.SubActionButton;

import java.util.Collections;
import java.util.List;

import static com.abborg.glom.views.CircleMenu.Size.LARGE;

/**
 * Wrapper around Circular Menu library
 *
 * Created by Jitrapon on 12/11/2559.
 */
public class CircleMenu {

    private List<MenuActionItem> items;
    private CircleMenuListener listener;
    private Size size;
    private ImageView centerView;
    private int startAngle;
    private int endAngle;
    private Activity activity;
    private FloatingActionMenu menu;
    private String centerImageUrl;

    private boolean isAnimating;
    private boolean isOpened;

    private Animation fadeInAnim;
    private Animation fadeOutAnim;

    private RelativeLayout circleMenuLayout;
    private ImageView menuOverlay;

    private Handler handler;

    private static final int MENU_OVERLAY_ANIM_TIME = 150;

    public enum Size {
        SMALL, MEDIUM, LARGE
    }

    private CircleMenu() {
        items = Collections.emptyList();
        listener = null;
        size = LARGE;
        centerView = null;
        startAngle = 0;
        endAngle = 360;
        activity = null;
    }

    public static CircleMenu init() {
        return new CircleMenu();
    }

    public CircleMenu setLayout(RelativeLayout relativeLayout) {
        circleMenuLayout = relativeLayout;
        return this;
    }

    public CircleMenu setActivity(Activity activity) {
        this.activity = activity;
        return this;
    }

    public CircleMenu setMenuItems(List<MenuActionItem> menuItems) {
        items = menuItems;
        return this;
    }

    public CircleMenu setMenuOptionsClickedListener(CircleMenuListener menuListener) {
        listener = menuListener;
        return this;
    }

    public CircleMenu setRadiusSize(Size menuSize) {
        size = menuSize;
        return this;
    }

    public CircleMenu setCenterImageSource(String url) {
        centerImageUrl = url;
        centerView = new ImageView(activity);
        return this;
    }

    public CircleMenu setStartEndAngle(int start, int end) {
        startAngle = start;
        endAngle = end;
        return this;
    }

    public CircleMenu setHandler(Handler h) {
        handler = h;
        return this;
    }

    public CircleMenu create() {
        FloatingActionMenu.Builder menuBuilder =  new FloatingActionMenu.Builder(activity);

        int subActionButtonSize = activity.getResources().getDimensionPixelSize(R.dimen.light_sub_action_button_size);
        int subActionButtonContentMargin = activity.getResources().getDimensionPixelSize(R.dimen.light_sub_action_button_content_margin);

        SubActionButton.Builder lCSubBuilder = new SubActionButton.Builder(activity);
        lCSubBuilder.setBackgroundDrawable(ContextCompat.getDrawable(activity, R.drawable.button_action_light_selector));

        FrameLayout.LayoutParams actionButtonContentParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        actionButtonContentParams.setMargins(subActionButtonContentMargin,
                subActionButtonContentMargin,
                subActionButtonContentMargin,
                subActionButtonContentMargin);
        lCSubBuilder.setLayoutParams(actionButtonContentParams);

        // Set custom layout params
        FrameLayout.LayoutParams subActionButtonParams = new FrameLayout.LayoutParams(subActionButtonSize, subActionButtonSize);
        lCSubBuilder.setLayoutParams(subActionButtonParams);

        // add menu items
        for (final MenuActionItem menuItem : items) {
            ImageView icon = new ImageView(activity);
            icon.setImageDrawable(ContextCompat.getDrawable(activity, menuItem.getIcon()));
            SubActionButton actionButton = lCSubBuilder.setContentView(icon, actionButtonContentParams).build();
            actionButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    close(false);
                    if (menuItem.getLabel() == R.string.action_others) {
                        if (listener != null) {
                            listener.onOtherCircleMenuOptionClicked();
                        }
                    }
                    else {
                        if (listener != null) {
                            listener.onCircleMenuOptionsClicked(menuItem);
                        }
                    }
                }
            });
            menuBuilder.addSubActionView(actionButton);
        }

        menuBuilder.setStateChangeListener(new FloatingActionMenu.MenuStateChangeListener() {
            @Override
            public void onMenuOpened(FloatingActionMenu floatingActionMenu) {
                isAnimating = true;
                Log.d("DEBUG", "onMenuOpening");
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        isAnimating = false;
                        isOpened = true;
                        Log.d("DEBUG", "onMenuOpened");
                    }
                }, 700);
            }

            @Override
            public void onMenuClosed(FloatingActionMenu floatingActionMenu) {
                Log.d("DEBUG", "onMenuClosed");
                isAnimating = false;
                isOpened = false;
            }
        });

        // initialize the overlay imageview
        menuOverlay = new ImageView(activity);
        RelativeLayout.LayoutParams menuOverlayParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);
        menuOverlay.setLayoutParams(menuOverlayParams);
        menuOverlay.setBackgroundColor(ContextCompat.getColor(activity, R.color.menuOverlay));

        // initialize the overlay center icon with radial menu
        RelativeLayout.LayoutParams centerViewParams = new RelativeLayout.LayoutParams(
                activity.getResources().getDimensionPixelSize(R.dimen.avatar_menu_size),
                activity.getResources().getDimensionPixelSize(R.dimen.avatar_menu_size));
        centerViewParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        centerView.setLayoutParams(centerViewParams);

        // add fade-in / fade-out animation when visibilty changes
        fadeInAnim = AnimationUtils.loadAnimation(activity, android.R.anim.fade_in);
        fadeOutAnim = AnimationUtils.loadAnimation(activity, android.R.anim.fade_out);
        fadeInAnim.setDuration(MENU_OVERLAY_ANIM_TIME);
        fadeOutAnim.setDuration(MENU_OVERLAY_ANIM_TIME);

        menuOverlay.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (!isAnimating) {
                    close(true);
                }
            }
        });

        // add the overlay and avatar icon to the layout
        circleMenuLayout.addView(menuOverlay, 0);
        circleMenuLayout.addView(centerView, 1);

        // hide the layout for now until an avatar is clicked
        circleMenuLayout.setVisibility(RelativeLayout.GONE);

        // finally build it
        int radiusResource = size == Size.SMALL ? R.dimen.avatar_menu_radius_small :
                size == Size.MEDIUM ? R.dimen.avatar_menu_radius_medium : R.dimen.avatar_menu_radius_large;
        menu = menuBuilder.setRadius(activity.getResources().getDimensionPixelSize(radiusResource))
                .setStartAngle(startAngle)
                .setEndAngle(endAngle)
                .attachTo(centerView)
                .build();

        return this;
    }

    private void showMenuOverlay() {
        fadeInAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {

            }
        });
        circleMenuLayout.setVisibility(RelativeLayout.VISIBLE);
        menuOverlay.startAnimation(fadeInAnim);
    }

    public boolean isOpened() {
        return isOpened;
    }

    public boolean isAnimating() {
        return isAnimating;
    }

    public void open(final boolean animated) {
        if (!isAnimating) {
            if (listener != null) {
                listener.onCircleMenuOptionsOpening();
            }

            showMenuOverlay();

            Glide.with(activity)
                    .load(centerImageUrl)
                    .fitCenter()
                    .transform(new CircleTransform(activity))
                    .override((int) activity.getResources().getDimension(R.dimen.user_avatar_width),
                            (int) activity.getResources().getDimension(R.dimen.user_avatar_height))
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .crossFade(1000)
                    .into(centerView);

            handler.postDelayed(new Runnable() {

                @Override
                public void run() {
                    if (!menu.isOpen())
                        menu.open(animated);
                }
            }, 50);
        }
    }

    public void close(boolean animated) {
        if (listener != null) {
            listener.onCircleMenuOptionsClosing();
        }

        isAnimating = animated;
        if (isAnimating) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    isAnimating = false;
                }
            }, 700);
        }

        fadeOutAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                circleMenuLayout.setVisibility(RelativeLayout.GONE);
            }
        });
        if (animated) menuOverlay.startAnimation(fadeOutAnim);
        else circleMenuLayout.setVisibility(RelativeLayout.GONE);

        if (menu != null) menu.close(animated);
    }

    public void destroy() {
        menu = null;
        items = null;
        listener = null;
        size = null;
        centerView = null;
        activity = null;
        circleMenuLayout = null;
        menuOverlay = null;
    }
}
