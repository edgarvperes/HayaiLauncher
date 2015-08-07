package com.seizonsenryaku.hayailauncher;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

/**
 * Created by Edgar on 07-Aug-15.
 */
public class ImageLoadingTask extends SimpleTaskConsumer.Task{
    private final ImageView imageView;
    private final LaunchableActivity launchableActivity;
    private final Object uiMutex;
    private final Activity activity;
    private final PackageManager packageManager;
    private final Context context;

    public ImageLoadingTask(final ImageView imageView, final LaunchableActivity launchableActivity,
                            final Object uiMutex, final Activity activity,
                            final PackageManager packageManager,
                            final Context context) {
        this.imageView = imageView;
        this.launchableActivity = launchableActivity;
        this.uiMutex=uiMutex;
        this.activity = activity;
        this.packageManager=packageManager;
        this.context=context;
    }


    public boolean doTask(){
        final Drawable activityIcon = launchableActivity.getActivityIcon(packageManager, context);
        activity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                synchronized (uiMutex) {
                    if (imageView.getTag() == launchableActivity.getClassName())
                        imageView.setImageDrawable(activityIcon);
                }
            }
        });
        return false;
    }

}
