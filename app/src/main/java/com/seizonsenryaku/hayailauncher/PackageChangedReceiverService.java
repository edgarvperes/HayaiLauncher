package com.seizonsenryaku.hayailauncher;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.seizonsenryaku.hayailauncher.activities.SearchActivity;

/**
 * Created by Edgar on 08-Aug-15.
 */
public class PackageChangedReceiverService extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            final String packageChangedName = intent.getData().getSchemeSpecificPart();

            if (packageChangedName != null && !packageChangedName.isEmpty()) {
                Log.d("Received thing", "EXTRA_CHANGED_COMPONENT_NAME_LIST=" +
                        packageChangedName);
                final SharedPreferences sharedPreferences = context.getSharedPreferences(
                        context.getPackageName() + "_preferences",
                        Context.MODE_PRIVATE);
                sharedPreferences.edit().putBoolean("package_changed", true).apply();
            }
        }

}
