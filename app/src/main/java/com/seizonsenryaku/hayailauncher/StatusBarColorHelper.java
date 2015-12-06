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

package com.seizonsenryaku.hayailauncher;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;


public class StatusBarColorHelper {

    public static void setStatusBarColor(final Resources resources, final Activity activity,
                                         final int color) {
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

            final int statusBarHeight = getStatusBarHeight(resources);
            final View statusBarDummy = activity.findViewById(R.id.statusBarDummyView);
            statusBarDummy.getLayoutParams().height=statusBarHeight;
            statusBarDummy.setBackgroundColor(color);
        }
    }

    public static int getStatusBarHeight(final Resources resources) {

        final int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        return resourceId > 0 ? resources.getDimensionPixelSize(resourceId) : 0;
    }

    public static int getNavigationBarHeight(final Resources resources) {

        final int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        return resourceId > 0 ? resources.getDimensionPixelSize(resourceId) : 0;
    }


}
