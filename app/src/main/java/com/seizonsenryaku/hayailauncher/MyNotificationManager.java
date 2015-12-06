package com.seizonsenryaku.hayailauncher;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.seizonsenryaku.hayailauncher.activities.SearchActivity;

public class MyNotificationManager {
	private static final int NOTIFICATION_ID = 0;

    public static int getPriorityFromString(String priority) {
        int i_priority = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            if (priority.toLowerCase().equals("max")) {
                i_priority = Notification.PRIORITY_MAX;
            } else if (priority.toLowerCase().equals("min")) {
                i_priority = Notification.PRIORITY_MIN;
            }
        }
        return i_priority;
    }

    public void showNotification(Context context, int priority) {
        final Intent resultIntent = new Intent(context, SearchActivity.class);
        resultIntent.setAction(Intent.ACTION_MAIN);
        resultIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        final PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        final Notification notification = new Notification.Builder(
                context)
				.setSmallIcon(R.mipmap.launcher_icon)
				.setContentTitle(context.getString(R.string.title_activity_search))
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .getNotification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            notification.priority = priority;
        }

        final NotificationManager mNotificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);

		mNotificationManager.notify(NOTIFICATION_ID, notification);
	}

	public void cancelNotification(Context context) {

        final NotificationManager mNotificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);

		mNotificationManager.cancel(NOTIFICATION_ID);
	}
}
