package com.seizonsenryaku.hayailauncher;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.util.Log;

public class LaunchableActivity implements Comparable<LaunchableActivity> {
	private ActivityInfo activityInfo;
	private int numberOfLaunches;
	private String activityLabel;
	private Drawable activityIcon;
	private boolean favorite;
    private ComponentName componentName;
	//This limitation is needed to speedup the compareTo function.
	private static final int MAX_LAUNCHES = 16383;

	public LaunchableActivity(ActivityInfo activityInfo, String activityLabel) {
		this.activityInfo = activityInfo;
		this.activityLabel = activityLabel;
        componentName = new ComponentName(activityInfo.packageName, activityInfo.name);
	}

	public void incrementLaunches() {
		if(numberOfLaunches<MAX_LAUNCHES)
			numberOfLaunches++;
	}

	public void setNumberOfLaunches(int numberOfLaunches) {
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

	public Drawable getActivityIcon(PackageManager pm,Context context ) {

		if (activityIcon == null) {
			if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1){
                final ActivityManager activityManager =
                        (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                final int iconDpi = activityManager.getLauncherLargeIconDensity();
				try {
                    activityIcon = pm.getResourcesForActivity(componentName).getDrawableForDensity(
                            activityInfo.getIconResource(), iconDpi);
				} catch (PackageManager.NameNotFoundException e) {
					e.printStackTrace();
				}
                if(activityIcon==null){
                        activityIcon = Resources.getSystem().getDrawableForDensity(
                                android.R.mipmap.sym_def_app_icon, iconDpi);
                }

			}else {

				activityIcon = activityInfo.loadIcon(pm);
                if(activityIcon==null){
                    activityIcon = Resources.getSystem().getDrawable(
                            android.R.mipmap.sym_def_app_icon);
                }
			}
		}
		return activityIcon;
	}

	@Override
	public int compareTo(@NonNull LaunchableActivity another) {
		
		//Criteria 1 (Bit 1) indicates whether the activity is flagged as favorite or not.
		//Criteria 2 (Bits 2 to 16) indicates the number of launches
		//Criteria 3 (Bits 17 to 31) indicates string difference (can be at most Character.MAX_VALUE)
		
		//
		int anotherN = (another.favorite ? 0x40000000
				: 0) + (another.numberOfLaunches<<16);
		int thisN = (this.favorite ? 0x40000000
				: 0) + (this.numberOfLaunches<<16);

		
		anotherN+=this.activityLabel.compareTo(another.activityLabel);

		return anotherN - thisN;
	}

	public ComponentName getComponent() {
		return componentName;
	}

	public String getClassName() {
		return activityInfo.name;
	}
}
