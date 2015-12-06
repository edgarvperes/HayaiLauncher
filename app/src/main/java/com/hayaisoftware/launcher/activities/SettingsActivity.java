/*  Copyright 2015 Hayai Software
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

package com.hayaisoftware.launcher.activities;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.Menu;

import com.hayaisoftware.launcher.MyNotificationManager;
import com.hayaisoftware.launcher.R;

public class SettingsActivity extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {

	public static final String KEY_PREF_NOTIFICATION = "pref_notification";
    public static final String KEY_PREF_NOTIFICATION_PRIORITY = "pref_notification_priority";

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.preferences);
	}


	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
        if (key.equals(KEY_PREF_NOTIFICATION) || key.equals(KEY_PREF_NOTIFICATION_PRIORITY)) {
            boolean notificationEnabled =
                    sharedPreferences.getBoolean(KEY_PREF_NOTIFICATION, false);
            MyNotificationManager myNotificationManager = new MyNotificationManager();
			if (notificationEnabled) {
                final String strPriority =
                        sharedPreferences.getString(SettingsActivity.KEY_PREF_NOTIFICATION_PRIORITY,
                                "low");
                final int priority = MyNotificationManager.getPriorityFromString(strPriority);
                myNotificationManager.showNotification(this, priority);
            } else {
                myNotificationManager.cancelNotification(this);
			}
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		//MenuInflater inflater = getMenuInflater();
		//inflater.inflate(R.menu.menu_about, menu);
		return true;
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onResume() {
		super.onResume();
		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onPause() {
		getPreferenceScreen().getSharedPreferences()
				.unregisterOnSharedPreferenceChangeListener(this);
		super.onPause();
		finish();
	}

}
