package com.abborg.glom.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.abborg.glom.ApplicationState;
import com.abborg.glom.R;
import com.abborg.glom.adapters.NavMenuAdapter;
import com.abborg.glom.di.ComponentInjector;
import com.abborg.glom.interfaces.CircleListListener;
import com.abborg.glom.interfaces.ClickListener;
import com.abborg.glom.model.User;
import com.abborg.glom.utils.CircleTransform;
import com.bumptech.glide.Glide;

import javax.inject.Inject;

public class DrawerFragment extends Fragment implements
        CircleListListener {

    @Inject
    ApplicationState appState;

    private RecyclerView recyclerView;
    private ImageView userAvatar;
    private TextView userName;
    private TextView userId;
    private ActionBarDrawerToggle drawerToggleIndicator;
    private DrawerLayout drawerLayout;
    private NavMenuAdapter adapter;
    private View containerView;
    private NavMenuClickListener drawerListener;

    public DrawerFragment() {
    }

    public void setDrawerListener(NavMenuClickListener listener) {
        drawerListener = listener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ComponentInjector.INSTANCE.getApplicationComponent().inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_drawer, container, false);
        recyclerView = (RecyclerView) layout.findViewById(R.id.drawer_recycler_view);
        userAvatar = (ImageView) layout.findViewById(R.id.user_avatar);
        userName = (TextView) layout.findViewById(R.id.user_name);
        userId = (TextView) layout.findViewById(R.id.user_id);

        return layout;
    }

    private void updateUserProfileSection() {
        User user = appState.getActiveUser();
        Context context = getContext();
        if (user != null && context != null) {
            Glide.with(this)
                    .load(user.getAvatar())
                    .transform(new CircleTransform(context))
                    .override((int) context.getResources().getDimension(R.dimen.user_avatar_width),
                            (int) context.getResources().getDimension(R.dimen.user_avatar_height))
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .crossFade(300)
                    .into(userAvatar);
            userName.setVisibility(TextUtils.isEmpty(user.getName()) ? View.GONE : View.VISIBLE);
            userName.setText(user.getName());
            userId.setVisibility(TextUtils.isEmpty(user.getId()) ? View.GONE : View.VISIBLE);
            userId.setText("@" + user.getId());
        }
    }

    public void setupView(int fragmentId, DrawerLayout layout, final Toolbar toolbar, @IdRes int toolbarNavICon) {
        updateUserProfileSection();

        adapter = new NavMenuAdapter(getActivity(), appState.getCircleList());
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getActivity(), recyclerView, new ClickListener() {

            @Override
            public void onClick(View view, int position) {
                drawerListener.onNavMenuItemClicked(view, position);
                drawerLayout.closeDrawer(containerView);
            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));

        containerView = getActivity().findViewById(fragmentId);
        drawerLayout = layout;
        drawerToggleIndicator = new ActionBarDrawerToggle(getActivity(), drawerLayout, toolbar, R.string.ACTION_OPEN_DRAWER, R.string.ACTION_CLOSE_DRAWER) {

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                getActivity().invalidateOptionsMenu();
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                getActivity().invalidateOptionsMenu();
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);
                toolbar.setAlpha(1 - slideOffset / 2);
            }
        };
        drawerToggleIndicator.setDrawerIndicatorEnabled(false);
        View navIcon = toolbar.findViewById(toolbarNavICon);
        navIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DrawerFragment.this.drawerLayout.openDrawer(Gravity.LEFT);
            }
        });

        drawerLayout.addDrawerListener(drawerToggleIndicator);
        drawerLayout.post(new Runnable() {
            @Override
            public void run() {
                drawerToggleIndicator.syncState();
            }
        });
    }

    static class RecyclerTouchListener implements RecyclerView.OnItemTouchListener {

        private GestureDetector gestureDetector;
        private ClickListener clickListener;

         RecyclerTouchListener(Context context, final RecyclerView recyclerView, final ClickListener clickListener) {
            this.clickListener = clickListener;
            gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {

                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    return true;
                }

                @Override
                public void onLongPress(MotionEvent e) {
                    View child = recyclerView.findChildViewUnder(e.getX(), e.getY());
                    if (child != null && clickListener != null) {
                        clickListener.onLongClick(child, recyclerView.getChildAdapterPosition(child));
                    }
                }
            });
        }

        @Override
        public boolean onInterceptTouchEvent(RecyclerView recyclerView, MotionEvent e) {
            View child = recyclerView.findChildViewUnder(e.getX(), e.getY());
            if (child != null && clickListener != null && gestureDetector.onTouchEvent(e)) {
                clickListener.onClick(child, recyclerView.getChildAdapterPosition(child));
            }
            return false;
        }

        @Override
        public void onTouchEvent(RecyclerView rv, MotionEvent e) {
        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

        }
    }

    public interface NavMenuClickListener {
        void onNavMenuItemClicked(View view, int position);
    }

    @Override
    public void onCircleListChanged() {
        adapter.setCircles(appState.getCircleList());
    }
}
