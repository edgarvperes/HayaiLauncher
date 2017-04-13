/*
 * Copyright (c) 2015-2017 Hayai Software
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hayaisoftware.launcher.activities;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.view.Menu;

import com.hayaisoftware.launcher.R;
import com.hayaisoftware.launcher.ShortcutNotificationManager;

public class SettingsActivity extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {

	public static final String KEY_PREF_NOTIFICATION = "pref_notification";
    public static final String KEY_PREF_NOTIFICATION_PRIORITY = "pref_notification_priority";
	public static final String KEY_PREF_AUTO_KEYBOARD = "pref_autokeyboard";
    public static final String KEY_PREF_ALLOW_ROTATION = "pref_allow_rotation";

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.preferences);

		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
			//remove priority preference (not supported)
			PreferenceCategory notificationCategory =
					(PreferenceCategory) findPreference("pref_category_notification");
			ListPreference priorityPreference =
					(ListPreference) findPreference("pref_notification_priority");
			notificationCategory.removePreference(priorityPreference);
		}
	}


	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
        if (key.equals(KEY_PREF_NOTIFICATION) || key.equals(KEY_PREF_NOTIFICATION_PRIORITY)) {
            boolean notificationEnabled =
                    sharedPreferences.getBoolean(KEY_PREF_NOTIFICATION, false);
            ShortcutNotificationManager shortcutNotificationManager = new ShortcutNotificationManager();
			shortcutNotificationManager.cancelNotification(this);
			if (notificationEnabled) {
                final String strPriority =
                        sharedPreferences.getString(SettingsActivity.KEY_PREF_NOTIFICATION_PRIORITY,
                                "low");
                final int priority = ShortcutNotificationManager.getPriorityFromString(strPriority);
                shortcutNotificationManager.showNotification(this, priority);
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
