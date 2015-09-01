package com.seizonsenryaku.hayailauncher;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import com.seizonsenryaku.hayailauncher.threading.SimpleTaskConsumerManager;

/**
 * Created by Edgar on 07-Aug-15.
 */
public class ImageLoadingTask extends SimpleTaskConsumerManager.Task {
    private final ImageView imageView;
    private final LaunchableActivity launchableActivity;
    private final SharedData sharedData;

    public ImageLoadingTask(final ImageView imageView, final LaunchableActivity launchableActivity,
                            final SharedData sharedData) {
        this.imageView = imageView;
        this.launchableActivity = launchableActivity;
        this.sharedData = sharedData;
    }

    public void doTask() {
        final Drawable activityIcon =
                launchableActivity.getActivityIcon(sharedData.packageManager, sharedData.context,
                        sharedData.iconSizePixels);
        sharedData.activity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                if (imageView.getTag() == launchableActivity)
                    imageView.setImageDrawable(activityIcon);
            }
        });
    }

    public static class SharedData {
        private final Activity activity;
        private final PackageManager packageManager;
        private final Context context;
        private final int iconSizePixels;

        public SharedData(final Activity activity, final PackageManager packageManager,
                          final Context context, final int iconSizePixels) {
            this.activity = activity;
            this.packageManager = packageManager;
            this.context = context;
            this.iconSizePixels = iconSizePixels;
        }
    }

}
