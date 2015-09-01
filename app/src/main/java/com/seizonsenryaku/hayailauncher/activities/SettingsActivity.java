package com.seizonsenryaku.hayailauncher.activities;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.Menu;

import com.seizonsenryaku.hayailauncher.MyNotificationManager;
import com.seizonsenryaku.hayailauncher.R;

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
