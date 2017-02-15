package com.hayaisoftware.launcher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class SdCardMountedReceiver extends BroadcastReceiver {

    public void onReceive(Context context, Intent intent) {

        // this is the list of newly available apps
        String[] packageChangedNames = intent.getExtras().getStringArray(Intent.EXTRA_CHANGED_PACKAGE_LIST);
        Log.d("SD_Card Mounted", packageChangedNames.length+" newly available packages");

        final SharedPreferences sharedPreferences = context.getSharedPreferences(
                context.getPackageName() + "_preferences",
                Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        String names =sharedPreferences.getString("package_changed_names","");

        for(String availablePackage : packageChangedNames)
        {
            // add the changed package to the list of changed packages
            if(!names.contains(availablePackage)) {
                names += " " + availablePackage;
                editor.putString("package_changed_name", names.trim());
            }
        }
        editor.apply();
    }
}
