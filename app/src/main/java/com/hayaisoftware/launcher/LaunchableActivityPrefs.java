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

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;

public class LaunchableActivityPrefs extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 3;
    private static final String TABLE_NAME = "ActivityLaunchNumbers";
    private static final String KEY_CLASSNAME = "ClassName";
    private static final String KEY_ID = "Id";
    private static final String KEY_LASTLAUNCHTIMESTAMP = "LastLaunchTimestamp";
    private static final String KEY_USAGEQUANTIY = "UsageQuantity";
    private static final String KEY_FAVORITE = "Favorite";
    private static final String TABLE_CREATE = String
            .format("CREATE TABLE %s "
                            + "(%S INTEGER PRIMARY KEY, %s TEXT UNIQUE, %s INTEGER, %s INTEGER, %s INTEGER);",
                    TABLE_NAME, KEY_ID, KEY_CLASSNAME, KEY_LASTLAUNCHTIMESTAMP,
                    KEY_FAVORITE, KEY_USAGEQUANTIY);
    private static final String TABLE_DROP = String.format("DROP TABLE IF EXISTS %s", TABLE_NAME);
    public LaunchableActivityPrefs(Context context) {
        super(context, TABLE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
    }

    public void writePreference(String className, long number, int priority, int usageQuantity) {
        Log.d("LaunchablePrefs", "writePreference running");
        final SQLiteDatabase db = getWritableDatabase();
        final SQLiteStatement countStatement = db.compileStatement(String.format(
                "SELECT COUNT(*) FROM %s WHERE %s = ?", TABLE_NAME,
                KEY_CLASSNAME));
        countStatement.bindString(1, className);
        final long count = countStatement.simpleQueryForLong();
        countStatement.close();
        final SQLiteStatement statement;
        if (count == 0) {
            statement = db.compileStatement("INSERT INTO "
                    + TABLE_NAME + " (" + KEY_CLASSNAME + ", "
                    + KEY_LASTLAUNCHTIMESTAMP + "," + KEY_FAVORITE + "," + KEY_USAGEQUANTIY + ") VALUES(?,?,?,?)");
            statement.bindString(1, className);
            statement.bindLong(2, number);
            statement.bindLong(3, priority);
            statement.bindLong(4, usageQuantity);
        } else {
            statement = db.compileStatement("UPDATE "
                    + TABLE_NAME + " SET " + KEY_LASTLAUNCHTIMESTAMP + "=? , " + KEY_FAVORITE + "=? , " + KEY_USAGEQUANTIY + "=? WHERE "
                    + KEY_CLASSNAME + "=?");
            statement.bindLong(1, number);
            statement.bindLong(2, priority);
            statement.bindLong(3, usageQuantity);
            statement.bindString(4, className);
        }
        statement.executeInsert();
        statement.close();
        db.close();
    }

    public void deletePreference(String className) {
        final SQLiteDatabase db = getWritableDatabase();
        final SQLiteStatement statement = db.compileStatement("DELETE FROM "
                + TABLE_NAME + " WHERE " + KEY_CLASSNAME + "=?");
        statement.bindString(1, className);
        statement.executeInsert();
        statement.close();
        db.close();

    }

    public void setAllPreferences(List<LaunchableActivity> activityList) {

        final SQLiteDatabase db = getReadableDatabase();

        final Cursor cursor = db.query(TABLE_NAME,
                new String[]{KEY_CLASSNAME, KEY_LASTLAUNCHTIMESTAMP, KEY_USAGEQUANTIY, KEY_FAVORITE},
                null, null, null, null, null);

        final AbstractMap<String, ActivityPref> activityPrefMap = new HashMap<>(cursor.getCount());

        if (cursor.moveToFirst()) {
            do {
                ActivityPref activityPref = new ActivityPref();
                activityPref.className = cursor.getString(cursor.getColumnIndex(KEY_CLASSNAME));
                activityPref.priority = cursor.getInt(cursor.getColumnIndex(KEY_FAVORITE));
                activityPref.lastTimestamp = cursor.getInt(cursor.getColumnIndex(KEY_LASTLAUNCHTIMESTAMP));
                activityPref.usagesQuantity = cursor.getInt(cursor.getColumnIndex(KEY_USAGEQUANTIY));
                activityPrefMap.put(activityPref.className, activityPref);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        for (LaunchableActivity activity : activityList) {
            ActivityPref activityPref = activityPrefMap.get(activity.getClassName());
            if (activityPref != null) {
                activityPref.wasUsed = true;
                activity.setLaunchTime(activityPref.lastTimestamp);
                activity.setusagesQuantity(activityPref.usagesQuantity);
                activity.setPriority(activityPref.priority);
            }
        }

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3 && newVersion == 3) {
            db.execSQL(TABLE_DROP);
            onCreate(db);
        }
    }

    private class ActivityPref {
        String className;
        int priority;
        long lastTimestamp;
        boolean wasUsed;
        int usagesQuantity;
    }
}
