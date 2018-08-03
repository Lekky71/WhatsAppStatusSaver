package com.hashcode.whatsstatussaver;

import android.app.Application;

import com.google.firebase.analytics.FirebaseAnalytics;

/**
 * Created by HashCode on 3:25 PM 03/08/2018.
 */
public class ApplicationInstance extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseAnalytics.getInstance(getApplicationContext());
    }
}
