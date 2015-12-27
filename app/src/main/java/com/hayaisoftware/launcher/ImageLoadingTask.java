/* Copyright 2015 Hayai Software
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

package com.hayaisoftware.launcher;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import com.hayaisoftware.launcher.threading.SimpleTaskConsumerManager;


public class ImageLoadingTask extends SimpleTaskConsumerManager.Task {
    private final ImageView mImageView;
    private final LaunchableActivity mLaunchableActivity;
    private final SharedData mSharedData;

    public ImageLoadingTask(final ImageView imageView, final LaunchableActivity launchableActivity,
                            final SharedData sharedData) {
        this.mImageView = imageView;
        this.mLaunchableActivity = launchableActivity;
        this.mSharedData = sharedData;
    }

    public boolean doTask() {
        final Drawable activityIcon =
                mLaunchableActivity.getActivityIcon(mSharedData.mPackageManager, mSharedData.mContext,
                        mSharedData.mIconSizePixels);
        mSharedData.mActivity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                if (mImageView.getTag() == mLaunchableActivity)
                    mImageView.setImageDrawable(activityIcon);
            }
        });
        return true;
    }

    public static class SharedData {
        private final Activity mActivity;
        private final PackageManager mPackageManager;
        private final Context mContext;
        private final int mIconSizePixels;

        public SharedData(final Activity activity, final PackageManager packageManager,
                          final Context context, final int iconSizePixels) {
            this.mActivity = activity;
            this.mPackageManager = packageManager;
            this.mContext = context;
            this.mIconSizePixels = iconSizePixels;
        }
    }

}
