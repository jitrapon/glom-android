package com.abborg.glom.di;

import com.abborg.glom.activities.BaseActivity;
import com.abborg.glom.activities.ChatActivity;
import com.abborg.glom.activities.DrawActivity;
import com.abborg.glom.activities.EventActivity;
import com.abborg.glom.activities.ListItemActivity;
import com.abborg.glom.activities.NoteActivity;
import com.abborg.glom.activities.SplashActivity;
import com.abborg.glom.adapters.BoardItemAdapter;
import com.abborg.glom.adapters.UserAvatarAdapter;
import com.abborg.glom.data.DataProvider;
import com.abborg.glom.fragments.BoardFragment;
import com.abborg.glom.fragments.CircleFragment;
import com.abborg.glom.fragments.DiscoverFragment;
import com.abborg.glom.fragments.DrawerFragment;
import com.abborg.glom.fragments.LocationFragment;
import com.abborg.glom.service.CirclePushService;
import com.abborg.glom.service.MessageService;
import com.abborg.glom.service.RegistrationIntentService;
import com.abborg.glom.utils.HttpClient;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {ApplicationModule.class})
public interface ApplicationComponent {

    void inject(SplashActivity activiy);

    void inject(BaseActivity activity);

    void inject(DataProvider provider);

    void inject(HttpClient httpClient);

    void inject(ChatActivity activity);

    void inject(DrawActivity activity);

    void inject(EventActivity activity);

    void inject(ListItemActivity activity);

    void inject(NoteActivity activity);

    void inject(BoardItemAdapter adapter);

    void inject(UserAvatarAdapter adapter);

    void inject(BoardFragment fragment);

    void inject(CircleFragment fragment);

    void inject(DiscoverFragment fragment);

    void inject(DrawerFragment fragment);

    void inject(LocationFragment fragment);

    void inject(CirclePushService service);

    void inject(MessageService service);

    void inject(RegistrationIntentService service);
}
