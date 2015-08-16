package com.seizonsenryaku.hayailauncher;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

/**
 * Created by Edgar on 04-Aug-15.
 */
public class StatusBarColorHelper {
    public static void setStatusBarColor(Resources resources, Activity activity, int color) {
        //There's no support for colored status bar in versions below KITKAT
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) return;

        final Window window = activity.getWindow();
            //KITKAT path
            window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);


        if(Build.VERSION.SDK_INT > 11) {
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }

        int statusBarHeight = getStatusBarHeight(resources);
            View statusBarDummy = activity.findViewById(R.id.statusBarDummyView);
            statusBarDummy.getLayoutParams().height=statusBarHeight;

    }

    private static int getStatusBarHeight(Resources resources) {
        int result = 0;
        int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId);
        }
        return result;
    }

}
