package com.seizonsenryaku.hayailauncher;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;

public class LaunchableActivity implements Comparable<LaunchableActivity> {
    private final ActivityInfo activityInfo;
    private final String activityLabel;
    private final ComponentName componentName;
    private int numberOfLaunches;
    private Drawable activityIcon;
    private boolean favorite;

    //This limitation is needed to speedup the compareTo function.
    private static final int MAX_LAUNCHES = 16383;

    public LaunchableActivity(final ActivityInfo activityInfo, final String activityLabel) {
        this.activityInfo = activityInfo;
        this.activityLabel = activityLabel;
        componentName = new ComponentName(activityInfo.packageName, activityInfo.name);
    }

    public void incrementLaunches() {
        if (numberOfLaunches < MAX_LAUNCHES)
            numberOfLaunches++;
    }

    public void setNumberOfLaunches(final int numberOfLaunches) {
        this.numberOfLaunches = numberOfLaunches;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public int getNumberOfLaunches() {
        return numberOfLaunches;
    }

    public CharSequence getActivityLabel() {
        return activityLabel;
    }

    public boolean isIconLoaded() {
        return activityIcon != null;
    }

    public synchronized Drawable getActivityIcon(final PackageManager pm, final Context context,
                                                 final int iconSizePixels) {
        if (!isIconLoaded()) {
            Drawable _activityIcon = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                final ActivityManager activityManager =
                        (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                final int iconDpi = activityManager.getLauncherLargeIconDensity();
                try {
                    _activityIcon = pm.getResourcesForActivity(componentName).getDrawableForDensity(
                            activityInfo.getIconResource(), iconDpi);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }

                if (_activityIcon == null) {
                    _activityIcon = Resources.getSystem().getDrawable(
                            android.R.mipmap.sym_def_app_icon);
                }

            } else {
                _activityIcon = activityInfo.loadIcon(pm);
            }

            //rescaling the icon if it is bigger than the target size
            //TODO do this when it is not a bitmap drawable?
            if (_activityIcon instanceof BitmapDrawable) {
                if (_activityIcon.getIntrinsicHeight() > iconSizePixels &&
                        _activityIcon.getIntrinsicWidth() > iconSizePixels) {
                    _activityIcon = new BitmapDrawable(
                            Bitmap.createScaledBitmap(((BitmapDrawable) _activityIcon).getBitmap()
                                    , iconSizePixels, iconSizePixels, false));
                }
            }
            activityIcon = _activityIcon;
        }
        return activityIcon;
    }

    @Override
    public int compareTo(@NonNull final LaunchableActivity another) {

        //Criteria 1 (Bit 1) indicates whether the activity is flagged as favorite or not.
        //Criteria 2 (Bits 2 to 16) indicates the number of launches
        //Criteria 3 (Bits 17 to 31) indicates string difference (can be at most Character.MAX_VALUE)

        //
        final int thisN = (this.favorite ? 0x40000000
                : 0) + (this.numberOfLaunches << 16);

        final int anotherN = (another.favorite ? 0x40000000
                : 0) + (another.numberOfLaunches << 16) +
                activityLabel.compareTo(another.activityLabel);

        return anotherN - thisN;
    }

    public ComponentName getComponent() {
        return componentName;
    }

    public String getClassName() {
        return activityInfo.name;
    }
}
