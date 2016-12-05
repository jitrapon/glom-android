package com.abborg.glom.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;

import com.abborg.glom.Const;
import com.abborg.glom.data.DataProvider;
import com.abborg.glom.di.ComponentInjector;

import javax.inject.Inject;

public class SplashActivity extends AppCompatActivity implements
        Handler.Callback {

    @Inject
    DataProvider dataProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ComponentInjector.INSTANCE.getApplicationComponent().inject(this);

        Handler handler = new Handler(this);

        // begin loading data from DB
        dataProvider.setHandler(handler);
        dataProvider.loadDataAsync();
    }

    /**********************************************************
     * Handler Callbacks
     **********************************************************/

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {

            case Const.MSG_INIT_SUCCESS: {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                finish();

                break;
            }
        }

        return false;
    }
}
