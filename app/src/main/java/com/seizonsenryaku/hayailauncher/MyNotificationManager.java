package com.seizonsenryaku.hayailauncher;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.seizonsenryaku.hayailauncher.activities.SearchActivity;

public class MyNotificationManager {
	private static final int NOTIFICATION_ID = 0;

    public static int getPriorityFromString(String priority) {
        int i_priority = 0;
        if (priority.toLowerCase().equals("max")) {
            i_priority = NotificationCompat.PRIORITY_MAX;
        } else if (priority.toLowerCase().equals("min")) {
            i_priority = NotificationCompat.PRIORITY_MIN;
        }
        return i_priority;
    }

    public void showNotification(Context context, int priority) {
        Intent resultIntent = new Intent(context, SearchActivity.class);
        resultIntent.setAction(Intent.ACTION_MAIN);
        resultIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

		Notification notification = new NotificationCompat.Builder(
				context)
				.setSmallIcon(R.drawable.ic_launcher)
				.setContentTitle(context.getString(R.string.title_activity_search))
                .setPriority(priority)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .build();

        NotificationManager mNotificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);

		mNotificationManager.notify(NOTIFICATION_ID, notification);
	}

	public void cancelNotification(Context context) {

        final NotificationManager mNotificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);

		mNotificationManager.cancel(NOTIFICATION_ID);
	}
}
