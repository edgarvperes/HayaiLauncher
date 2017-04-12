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

package com.hayaisoftware.launcher;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.hayaisoftware.launcher.util.ContentShare;


public class LaunchableActivity{

    private final ActivityInfo mActivityInfo;
    private final String mActivityLabel;
    private final ComponentName mComponentName;
    private Intent mLaunchIntent;
    private long lastLaunchTime;
    private int usagesQuantity;
    private boolean mShareable;
    private Drawable mActivityIcon;
    private int mPriority;

    public LaunchableActivity(final ActivityInfo activityInfo, final String activityLabel,
                              final boolean isShareable) {
        this.mActivityInfo = activityInfo;
        this.mActivityLabel = activityLabel;
        mComponentName = new ComponentName(activityInfo.packageName, activityInfo.name);
        this.mShareable = isShareable;
    }
    public LaunchableActivity(final ComponentName componentName, final String label,
                              final Drawable activityIcon, final Intent launchIntent){
        this.mComponentName = componentName;
        this.mActivityLabel = label;
        this.mLaunchIntent = launchIntent;
        this.mActivityIcon = activityIcon;
        this.mActivityInfo = null;
    }

    public Intent getLaunchIntent(final String searchString) {
        if (mLaunchIntent != null)
            return mLaunchIntent;
        if (isShareable()) {
            final Intent launchIntent = ContentShare.shareTextIntent(searchString);
            launchIntent.setComponent(getComponent());
            return launchIntent;
        }
        final Intent launchIntent = new Intent(Intent.ACTION_MAIN);
        launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        launchIntent.setComponent(mComponentName);
        return launchIntent;
    }

    public void setLaunchTime() {
        lastLaunchTime = System.currentTimeMillis() / 1000;
    }

    public int getPriority() {
        return mPriority;
    }

    public void setPriority(final int priority) {
        this.mPriority = priority;
    }

    public long getLaunchTime() {
        return lastLaunchTime;
    }

    public void setLaunchTime(long timestamp) {
        lastLaunchTime = timestamp;
    }

    public String getActivityLabel() {
        return mActivityLabel;
    }

    public boolean isIconLoaded() {
        return mActivityIcon != null;
    }

    public synchronized void deleteActivityIcon(){
        mActivityIcon=null;
    }

    public synchronized Drawable getActivityIcon(final PackageManager pm, final Context context,
                                                 final int iconSizePixels) {
        if (!isIconLoaded()) {
            Drawable _activityIcon = null;
                final ActivityManager activityManager =
                        (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                final int iconDpi = activityManager.getLauncherLargeIconDensity();
                try {
                    //noinspection deprecation
                    _activityIcon =
                            pm.getResourcesForActivity(mComponentName).getDrawableForDensity(
                                    mActivityInfo.getIconResource(), iconDpi);

                } catch (PackageManager.NameNotFoundException | Resources.NotFoundException e) {
                    //if we get here, there's no icon to load.
                    //there's nothing to do, as the android default icon will be loaded
                }

                if (_activityIcon == null) {
                    //noinspection deprecation
                    _activityIcon = Resources.getSystem().getDrawable(
                            android.R.mipmap.sym_def_app_icon);
                }


            //rescaling the icon if it is bigger than the target size
            //TODO do this when it is not a bitmap drawable?
            if (_activityIcon instanceof BitmapDrawable) {
                //Log.d("SIZE"," "+_activityIcon.getIntrinsicHeight()+ " not "+iconSizePixels);
                //Log.d("SIZE"," "+_activityIcon.getIntrinsicHeight()+ " not "+iconSizePixels);
                if (_activityIcon.getIntrinsicHeight() > iconSizePixels &&
                        _activityIcon.getIntrinsicWidth() > iconSizePixels) {
                    //noinspection deprecation
                    _activityIcon = new BitmapDrawable(
                            Bitmap.createScaledBitmap(((BitmapDrawable) _activityIcon).getBitmap()
                                    , iconSizePixels, iconSizePixels, false));
                }
            }
            mActivityIcon = _activityIcon;
        }
        return mActivityIcon;
    }

    public ComponentName getComponent() {
        return mComponentName;
    }

    public String getClassName() {
        return mComponentName.getClassName();
    }

    public boolean isShareable() {
        return mShareable;
    }

    public void addUsage() {
        usagesQuantity ++;
    }
    public int getusagesQuantity(){
        return usagesQuantity;
    }
    public void setusagesQuantity(int usagesQuantity){
        this.usagesQuantity = usagesQuantity;
    }
}
