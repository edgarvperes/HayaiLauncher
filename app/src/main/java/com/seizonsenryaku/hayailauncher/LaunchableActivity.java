package com.seizonsenryaku.hayailauncher;

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
import android.os.Build;
import android.support.annotation.NonNull;

import com.seizonsenryaku.hayailauncher.util.ContentShare;

public class LaunchableActivity implements Comparable<LaunchableActivity> {
    //This limitation is needed to speedup the compareTo function.
    private static final int MAX_LAUNCHES = 16383;
    private final ActivityInfo mActivityInfo;
    private final String mActivityLabel;
    private final ComponentName mComponentName;
    private final Intent mLaunchIntent;
    private int mNumberOfLaunches;
    private boolean mShareable;
    private Drawable mActivityIcon;
    private boolean mFavorite;

    public LaunchableActivity(final ActivityInfo activityInfo, final String activityLabel,
                              boolean isShareable) {
        this.mActivityInfo = activityInfo;
        this.mActivityLabel = activityLabel;
        mComponentName = new ComponentName(activityInfo.packageName, activityInfo.name);
        mLaunchIntent = null; //create one "on demand"
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

    public Intent getLaunchIntent(String searchString) {
        if (mLaunchIntent != null)
            return mLaunchIntent;
        if (isShareable()) {
            final Intent launchIntent = ContentShare.shareTextIntent(searchString);
            launchIntent.setComponent(getComponent());
            return launchIntent;
        }
        final Intent launchIntent = new Intent(Intent.ACTION_MAIN);
        launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        launchIntent.setComponent(mComponentName);
        return launchIntent;
    }
        
    public void incrementLaunches() {
        if (mNumberOfLaunches < MAX_LAUNCHES)
            mNumberOfLaunches++;
    }

    public boolean isFavorite() {
        return mFavorite;
    }

    public void setFavorite(boolean favorite) {
        this.mFavorite = favorite;
    }

    public int getNumberOfLaunches() {
        return mNumberOfLaunches;
    }

    public void setNumberOfLaunches(final int numberOfLaunches) {
        this.mNumberOfLaunches = numberOfLaunches;
    }

    public CharSequence getActivityLabel() {
        return mActivityLabel;
    }

    public boolean isIconLoaded() {
        return mActivityIcon != null;
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
                    //noinspection deprecation
                    _activityIcon =
                            pm.getResourcesForActivity(mComponentName).getDrawableForDensity(
                                    mActivityInfo.getIconResource(), iconDpi);

                } catch (PackageManager.NameNotFoundException | Resources.NotFoundException e) {
                    e.printStackTrace();
                }

                if (_activityIcon == null) {
                    //noinspection deprecation
                    _activityIcon = Resources.getSystem().getDrawable(
                            android.R.mipmap.sym_def_app_icon);
                }

            } else {
                _activityIcon = mActivityInfo.loadIcon(pm);
            }

            //rescaling the icon if it is bigger than the target size
            //TODO do this when it is not a bitmap drawable?
            if (_activityIcon instanceof BitmapDrawable) {
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

    @Override
    public int compareTo(@NonNull final LaunchableActivity another) {

        //Criteria 1 (Bit 1) indicates whether the activity is flagged as shareable or not.
        //Criteria 2 (Bits 2 to 16) indicates the number of launches
        //Criteria 3 (Bits 17 to 31) indicates string difference (can be at most Character.MAX_VALUE)

        //
        final int thisN = (this.mShareable ? 0
                : 0x40000000) + (this.mNumberOfLaunches << 16);

        final int anotherN = (another.mShareable ? 0
                : 0x40000000) + (another.mNumberOfLaunches << 16) +
                mActivityLabel.compareTo(another.mActivityLabel);

        return anotherN - thisN;
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
}
