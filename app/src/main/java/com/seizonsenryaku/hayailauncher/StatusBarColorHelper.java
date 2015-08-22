package com.seizonsenryaku.hayailauncher;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;


public class StatusBarColorHelper {

    public static void setStatusBarColor(Resources resources, Activity activity, int color) {
        //There's no support for colored status bar in versions below KITKAT
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) return;

        final Window window = activity.getWindow();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            //LOLLIPOP+ path
            window.setStatusBarColor(color);
        } else {
            //KITKAT path
            window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

            int statusBarHeight = getStatusBarHeight(resources);
            View statusBarDummy = activity.findViewById(R.id.statusBarDummyView);
            statusBarDummy.getLayoutParams().height=statusBarHeight;
            statusBarDummy.setBackgroundColor(color);
        }
    }

    public static int getStatusBarHeight(Resources resources) {
        int result = 0;
        int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId);

        }
        return result;
    }


}
