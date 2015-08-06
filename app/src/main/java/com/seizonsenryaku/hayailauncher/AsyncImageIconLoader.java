package com.seizonsenryaku.hayailauncher;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Edgar on 06-Aug-15.
 */
public class AsyncImageIconLoader implements Runnable {
    public static class Task {
        final ImageView imageView;
        final LaunchableActivity launchableActivity;

        public Task(final ImageView imageView, final LaunchableActivity launchableActivity) {
            this.imageView = imageView;
            this.launchableActivity = launchableActivity;
        }
    }

    private final LinkedBlockingQueue<Task> tasks;
    private final PackageManager pm;
    private final Context context;
    private final Activity activity;
    private final Drawable defaultAppIcon;

    public AsyncImageIconLoader(final PackageManager pm, final Context context,
                                final Activity activity, final Drawable defaultAppIcon) {
        tasks = new LinkedBlockingQueue<>();
        this.pm = pm;
        this.context = context;
        this.activity = activity;
        this.defaultAppIcon=defaultAppIcon;
    }

    @Override
    public void run() {
        do {
            try {
                final Task task = tasks.take();
                final Drawable activityIcon = task.launchableActivity.getActivityIcon(pm, context);
                activity.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            synchronized (AsyncImageIconLoader.this) {
                                if (task.imageView.getDrawable() == defaultAppIcon &&
                                        task.imageView.getTag()==task.launchableActivity.getClassName())
                                        task.imageView.setImageDrawable(activityIcon);
                            }
                        }
                });


            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        } while (true);
    }

    public void addTask(final Task task) {
        try {
            tasks.put(task);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
