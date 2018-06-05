package com.github.charleslzq.samplejava;

import android.app.Application;

import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.Logger;

public class SampleApplication extends Application {
    private static String LOG_DIRECTORY = "/logs/java";

    @Override
    public void onCreate() {
        super.onCreate();
        System.setProperty("rx2.buffer-size", "20");

        Logger.addLogAdapter(new AndroidLogAdapter());

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                Logger.e(e, "Exception occur at %s", t.getName());
            }
        });
    }
}
