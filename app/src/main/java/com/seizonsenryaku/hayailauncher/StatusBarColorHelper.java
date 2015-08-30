package com.seizonsenryaku.hayailauncher;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.util.DisplayMetrics;
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
            //window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);




        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
        //window.setStatusBarColor(Color.TRANSPARENT);
        }


        if(Build.VERSION.SDK_INT > 11) {
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }

        int statusBarHeight = getStatusBarHeight(resources);

        View statusBarDummy = activity.findViewById(R.id.statusBarDummyView);
        statusBarDummy.getLayoutParams().height=statusBarHeight;

        View topFillerView=activity.findViewById(R.id.topFillerView);
        topFillerView.getLayoutParams().height =statusBarHeight;

        View bottomFillerView=activity.findViewById(R.id.bottomFillerView);
        bottomFillerView.getLayoutParams().height =statusBarHeight;




    }

    public static int getStatusBarHeight(Resources resources) {

        int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return resources.getDimensionPixelSize(resourceId);

        }
        return 0;
    }

    public static int getNavigationBarHeight (Resources resources){

        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return resources.getDimensionPixelSize(resourceId);
        }
        return 0;
    }

}
