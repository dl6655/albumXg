package com.lq.albumXg;

import android.app.Application;

/**
 * Created by dingli on 2015-9-14.
 */
public class AlbumApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ImageHelper.initConfig(this);
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= TRIM_MEMORY_MODERATE) {
            ImageHelper.clearMemoryCache();
        }
    }
}
