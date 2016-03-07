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

package com.hayaisoftware.launcher.comparators;

import android.util.Log;

import com.hayaisoftware.launcher.LaunchableActivity;

import java.util.Comparator;


public class UsageOrder implements Comparator<LaunchableActivity>{

    @Override
    public int compare(LaunchableActivity lhs, LaunchableActivity rhs) {
        int lhsUsageQuantity = lhs.getusagesQuantity();
        int rhsUsageQuantity = rhs.getusagesQuantity();
        if (lhsUsageQuantity > 0 && rhsUsageQuantity > 0) {
            Log.d(getClass().getName(), "links: " + lhs.getClassName() + " " + lhsUsageQuantity);
            Log.d(getClass().getName(), "rechts: " + rhs.getClassName() + " " + rhsUsageQuantity);
        }
        if (lhsUsageQuantity > rhsUsageQuantity)
            return -1;
        if (lhsUsageQuantity < rhsUsageQuantity)
            return 1;
        return 0;
    }
}
