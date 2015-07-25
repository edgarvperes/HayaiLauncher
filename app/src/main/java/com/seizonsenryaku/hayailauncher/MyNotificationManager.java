package com.seizonsenryaku.hayailauncher;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

//public class MyNotificationManager extends BroadcastReceiver {
public class MyNotificationManager {
	private static final int NOTIFICATION_ID = 0;
	//private static final String NOTIFICATION_DELETE_ACTION = "NOTIFICATION_DELETED";

	public void showNotification(Context context) {
		// Creates an explicit intent for an Activity in your app
		Intent resultIntent = new Intent(context, SearchActivity.class);

		// The stack builder object will contain an artificial back stack for
		// the
		// started Activity.
		// This ensures that navigating backward from the Activity leads out of
		// your application to the Home screen.
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
		// Adds the back stack for the Intent (but not the Intent itself)
		stackBuilder.addParentStack(SearchActivity.class);
		// Adds the Intent that starts the Activity to the top of the stack
		stackBuilder.addNextIntent(resultIntent);
		PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,
				PendingIntent.FLAG_UPDATE_CURRENT);
		
		//Intent intent = new Intent(context, MyNotificationManager.class);
		//intent.setAction(NOTIFICATION_DELETE_ACTION);

		//context.registerReceiver(this, new IntentFilter(
		//		NOTIFICATION_DELETE_ACTION));
		
		Notification notification = new NotificationCompat.Builder(
				context)
				.setSmallIcon(R.drawable.ic_launcher)
				.setContentTitle("Quick Launcher")
				//.setContentText("Go to settings to disable the notification.")
				.setPriority(NotificationCompat.PRIORITY_MIN)
				.setOngoing(true)
				.setContentIntent(resultPendingIntent)
				//.setDeleteIntent(
				//		PendingIntent.getBroadcast(context, 0, intent, 0))
				.build();
		
		NotificationManager mNotificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);

		// mId allows you to update the notification later on.
		mNotificationManager.notify(NOTIFICATION_ID, notification);
	}

	public void cancelNotification(Context context) {

		NotificationManager mNotificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);

		// mId allows you to update the notification later on.
		mNotificationManager.cancel(NOTIFICATION_ID);
		// context.unregisterReceiver(this);
	}

	//@Override
	//public void onReceive(Context context, Intent intent) {
//		String action = intent.getAction();
		//Log.d(getClass().getSimpleName(), "receivedintent");
		//if (action.equals(NOTIFICATION_DELETE_ACTION)) {
//			SharedPreferences sharedPreferences = PreferenceManager
					//.getDefaultSharedPreferences(context);
			//sharedPreferences.edit().putBoolean(
					//SettingsActivity.KEY_PREF_NOTIFICATION, false);
		//}
		//context.unregisterReceiver(this);
	//}

}
