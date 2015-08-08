package com.seizonsenryaku.hayailauncher;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class LaunchableActivityPrefs extends SQLiteOpenHelper {

    private class ActivityPref {
        String className;
        boolean favorite;
        int numberOfLaunches;
        boolean wasUsed;
    }

    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "ActivityLaunchNumbers";
    private static final String KEY_CLASSNAME = "ClassName";
    private static final String KEY_ID = "Id";
    private static final String KEY_NUMBEROFLAUNCHES = "NumberOfLaunches";
    private static final String KEY_FAVORITE = "Favorite";
    private static final String TABLE_CREATE = String
            .format("CREATE TABLE %s "
                            + "(%S INTEGER PRIMARY KEY, %s TEXT UNIQUE, %s INTEGER, %s INTEGER);",
                    TABLE_NAME, KEY_ID, KEY_CLASSNAME, KEY_NUMBEROFLAUNCHES,
                    KEY_FAVORITE);

    public LaunchableActivityPrefs(Context context) {
        super(context, TABLE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
        db.close();
    }

    public void writePreference(String className, int number, boolean favorite) {
        SQLiteDatabase db = getWritableDatabase();
        SQLiteStatement countStatement = db.compileStatement(String.format(
                "SELECT COUNT(*) FROM %s WHERE %s = ?", TABLE_NAME,
                KEY_CLASSNAME));
        countStatement.bindString(1, className);
        long count = countStatement.simpleQueryForLong();
        if (count <= 0) {
            SQLiteStatement statement = db.compileStatement("INSERT INTO "
                    + TABLE_NAME + " (" + KEY_CLASSNAME + ", "
                    + KEY_NUMBEROFLAUNCHES + "," + KEY_FAVORITE + ") VALUES(?,?,?)");
            statement.bindString(1, className);
            statement.bindLong(2, number);
            statement.bindLong(3, favorite ? 1L : 0L);
            statement.executeInsert();
        } else {
            SQLiteStatement statement = db.compileStatement("UPDATE "
                    + TABLE_NAME + " SET " + KEY_NUMBEROFLAUNCHES + "=? , " + KEY_FAVORITE + "=? WHERE "
                    + KEY_CLASSNAME + "=?");
            statement.bindLong(1, number);
            statement.bindLong(2, favorite ? 1L : 0L);
            statement.bindString(3, className);
            statement.executeInsert();

        }
        db.close();
    }

    public void deletePreference(String className) {
        SQLiteDatabase db = getWritableDatabase();
        SQLiteStatement statement = db.compileStatement("DELETE FROM "
                + TABLE_NAME + " WHERE " + KEY_CLASSNAME + "=?");
        statement.bindString(1, className);
        statement.executeInsert();
        db.close();

    }

    public void setAllPreferences(List<LaunchableActivity> activityList) {

        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.query(TABLE_NAME,
                new String[]{KEY_CLASSNAME, KEY_NUMBEROFLAUNCHES, KEY_FAVORITE},
                null, null, null, null, null);


        HashMap<String, ActivityPref> activityPrefMap = new HashMap<>(cursor.getCount());

        if (cursor.moveToFirst()) {
            do {
                ActivityPref activityPref = new ActivityPref();
                activityPref.className = cursor.getString(cursor.getColumnIndex(KEY_CLASSNAME));
                activityPref.favorite = cursor.getInt(cursor.getColumnIndex(KEY_FAVORITE)) == 1;
                activityPref.numberOfLaunches = cursor.getInt(cursor.getColumnIndex(KEY_NUMBEROFLAUNCHES));
                activityPrefMap.put(activityPref.className, activityPref);
            } while (cursor.moveToNext());
        }
        db.close();
        for (LaunchableActivity activity : activityList) {
            ActivityPref activityPref = activityPrefMap.get(activity.getClassName());
            if (activityPref != null) {
                activityPref.wasUsed=true;
                activity.setNumberOfLaunches(activityPref.numberOfLaunches);
                activity.setFavorite(activityPref.favorite);
            }
        }

        Collection<ActivityPref> allLoadedPrefs=activityPrefMap.values();
        for(ActivityPref activityPref:allLoadedPrefs){
            if(!activityPref.wasUsed){
                deletePreference(activityPref.className);
            }
        }

    }


    public void setPreferences(LaunchableActivity launchableActivity) {
        long numberOfLaunches;
        int favorite;
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.query(TABLE_NAME,
                new String[]{KEY_NUMBEROFLAUNCHES, KEY_FAVORITE},
                KEY_CLASSNAME + "=?", new String[]{launchableActivity.getClassName()}, null, null,
                null);


        if (cursor.moveToFirst()) {
            favorite = cursor.getInt(cursor.getColumnIndex(KEY_FAVORITE));
            numberOfLaunches = cursor.getInt(cursor.getColumnIndex(KEY_NUMBEROFLAUNCHES));

        } else {
            numberOfLaunches = favorite = 0;
        }
        db.close();
        launchableActivity.setNumberOfLaunches((int) numberOfLaunches);
        launchableActivity.setFavorite(favorite == 1);

    }

    @Override
    public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
        // TODO Auto-generated method stub

    }

}
