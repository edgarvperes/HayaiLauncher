/* Copyright 2015 Hayai Software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hayaisoftware.launcher;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.hayaisoftware.launcher.activities.SearchActivity;

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
				.setSmallIcon(R.drawable.ic_notification)
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
