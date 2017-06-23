package com.example.developerhaoz.glideutils;

import android.app.Application;

/**
 * Created by developerHaoz on 2017/6/23.
 */

public class MyApplication extends Application {

    private static MyApplication sInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        if(sInstance == null){
            sInstance = this;
        }
    }

    public static MyApplication getInstance(){
        return sInstance;
    }
}
