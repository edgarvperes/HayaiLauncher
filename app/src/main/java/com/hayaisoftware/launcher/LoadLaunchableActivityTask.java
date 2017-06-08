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

import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import com.hayaisoftware.launcher.threading.SimpleTaskConsumerManager;

import java.util.ArrayList;


public class LoadLaunchableActivityTask extends SimpleTaskConsumerManager.Task {
    private final ResolveInfo info;
    private final SharedData mSharedData;

    public LoadLaunchableActivityTask(final ResolveInfo info,
                                      final SharedData sharedData) {
        this.info = info;
        this.mSharedData = sharedData;
    }

    public boolean doTask() {
        final LaunchableActivity launchableActivity = new LaunchableActivity(
                info.activityInfo, info.activityInfo.loadLabel(mSharedData.mPackageManager).toString(), false);
        synchronized (mSharedData.launchablesFromResolve) {
            mSharedData.launchablesFromResolve.add(launchableActivity);
        }
        return true;
    }

    public static class SharedData {
        private final PackageManager mPackageManager;
        private final ArrayList<LaunchableActivity> launchablesFromResolve;

        public SharedData(final PackageManager packageManager,
                          final ArrayList<LaunchableActivity> launchablesFromResolve) {
            this.mPackageManager = packageManager;
            this.launchablesFromResolve=launchablesFromResolve;

        }
    }

}
