package com.seizonsenryaku.hayailauncher;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import com.seizonsenryaku.hayailauncher.activities.SearchActivity;

public class MyNotificationManager {
	private static final int NOTIFICATION_ID = 0;


	public void showNotification(Context context) {
		Intent resultIntent = new Intent(context, SearchActivity.class);
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
		stackBuilder.addParentStack(SearchActivity.class);
		stackBuilder.addNextIntent(resultIntent);
		PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,
				PendingIntent.FLAG_UPDATE_CURRENT);
		
		Notification notification = new NotificationCompat.Builder(
				context)
				.setSmallIcon(R.drawable.ic_launcher)
				.setContentTitle(context.getString(R.string.title_activity_search))
				.setPriority(NotificationCompat.PRIORITY_MIN)
				.setOngoing(true)
				.setContentIntent(resultPendingIntent)
				.build();
		
		NotificationManager mNotificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);

		mNotificationManager.notify(NOTIFICATION_ID, notification);
	}

	public void cancelNotification(Context context) {

		NotificationManager mNotificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);

		// mId allows you to update the notification later on.
		mNotificationManager.cancel(NOTIFICATION_ID);
	}

}
