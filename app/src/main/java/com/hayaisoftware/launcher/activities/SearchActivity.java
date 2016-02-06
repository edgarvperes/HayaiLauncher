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

package com.hayaisoftware.launcher.activities;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.hayaisoftware.launcher.ImageLoadingTask;
import com.hayaisoftware.launcher.LaunchableActivity;
import com.hayaisoftware.launcher.LaunchableActivityPrefs;
import com.hayaisoftware.launcher.LoadLaunchableActivityTask;
import com.hayaisoftware.launcher.PackageChangedReceiver;
import com.hayaisoftware.launcher.R;
import com.hayaisoftware.launcher.ShortcutNotificationManager;
import com.hayaisoftware.launcher.StatusBarColorHelper;
import com.hayaisoftware.launcher.Trie;
import com.hayaisoftware.launcher.comparators.AlphabeticalOrder;
import com.hayaisoftware.launcher.comparators.PinToTop;
import com.hayaisoftware.launcher.comparators.RecentOrder;
import com.hayaisoftware.launcher.threading.SimpleTaskConsumerManager;
import com.hayaisoftware.launcher.util.ContentShare;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public class SearchActivity extends Activity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final int sNavigationBarHeightMultiplier = 1;
    private static final int sGridViewTopRowExtraPaddingInDP = 56;
    private static final int sMarginFromNavigationBarInDp = 16;
    private static final int sGridItemHeightInDp = 96;
    private static final int sInitialArrayListSize = 300;

    private final Pattern mPattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private int mStatusBarHeight;
    private ArrayList<LaunchableActivity> mActivityInfos;
    private ArrayList<LaunchableActivity> mShareableActivityInfos;
    private Trie<LaunchableActivity> mTrie;
    private ArrayAdapter<LaunchableActivity> mArrayAdapter;
    private HashMap<String, List<LaunchableActivity>> mLaunchableActivityPackageNameHashMap;
    private LaunchableActivityPrefs mLaunchableActivityPrefs;
    private SharedPreferences mSharedPreferences;
    private Context mContext;
    private Drawable mDefaultAppIcon;
    private SimpleTaskConsumerManager mImageLoadingConsumersManager;
    private ImageLoadingTask.SharedData mImageTasksSharedData;
    private int mIconSizePixels;
    private EditText mSearchEditText;
    private View mClearButton;
    private int mNumOfCores;
    private BroadcastReceiver mPackageChangedReceiver;

    private Comparator<LaunchableActivity> mPinToTopComparator;
    private Comparator<LaunchableActivity> mRecentOrderComparator;
    private Comparator<LaunchableActivity> mAlphabeticalOrderComparator;
    private InputMethodManager mInputMethodManager;
    private AdapterView mAppListView;
    private PackageManager mPm;
    private View mOverflowButtonTopleft;
    private int mColumnCount;
    //used only in function getAllSubwords. they are here as class fields to avoid
    // object re-allocation.
    private StringBuilder mWordSinceLastSpaceBuilder;
    private StringBuilder mWordSinceLastCapitalBuilder;
    private int mGridViewTopRowHeight;
    private int mGridViewBottomRowHeight;
    private boolean mShouldOrderByRecents;
    private final TextWatcher mTextWatcher = new TextWatcher() {

        @Override
        public void onTextChanged(CharSequence s, int start, int before,
                                  int count) {
            mClearButton.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            updateVisibleApps();
        }


        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            //do nothing
        }


        @Override
        public void afterTextChanged(Editable s) {
            //do nothing
        }


    };
    private boolean mDisableIcons;
    private boolean mAutoKeyboard;
    private boolean mIsCacheClear;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_search);

        mPm = getPackageManager();

        final Resources resources = getResources();

        //fields:
        mLaunchableActivityPackageNameHashMap = new HashMap<>();
        mShareableActivityInfos = new ArrayList<>(sInitialArrayListSize);
        mActivityInfos = new ArrayList<>(sInitialArrayListSize);
        mTrie = new Trie<>();
        mWordSinceLastSpaceBuilder = new StringBuilder(64);
        mWordSinceLastCapitalBuilder = new StringBuilder(64);

        mSearchEditText = (EditText) findViewById(R.id.editText1);
        mAppListView = (GridView) findViewById(R.id.appsContainer);
        mClearButton = findViewById(R.id.clear_button);
        mOverflowButtonTopleft = findViewById(R.id.overflow_button_topleft);
        mContext = getApplicationContext();
        mInputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        mStatusBarHeight = StatusBarColorHelper.getStatusBarHeight(resources);
        final DisplayMetrics displayMetrics = resources.getDisplayMetrics();
        final float displayDensity = displayMetrics.density;
        final int gridViewTopRowExtraPaddingInPixels =
                Math.round(displayDensity * sGridViewTopRowExtraPaddingInDP);
        final int marginFromNavigationBarInPixels =
                Math.round(displayDensity * sMarginFromNavigationBarInDp);
        final int gridItemHeightInPixels =
                Math.round(displayDensity * sGridItemHeightInDp);
        int statusBarMultiplierPaddings = setPaddingHeights();
        mGridViewTopRowHeight = statusBarMultiplierPaddings * mStatusBarHeight +
                gridViewTopRowExtraPaddingInPixels;
        mGridViewBottomRowHeight = gridItemHeightInPixels + sNavigationBarHeightMultiplier *
                StatusBarColorHelper.getNavigationBarHeight(getResources()) +
                marginFromNavigationBarInPixels;

        mColumnCount = resources.getInteger(R.integer.app_grid_columns);

        mSharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(this);
        setupPreferences();
        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
        mLaunchableActivityPrefs = new LaunchableActivityPrefs(this);

        //noinspection deprecation
        mDefaultAppIcon = Resources.getSystem().getDrawable(
                android.R.mipmap.sym_def_app_icon);
        mIconSizePixels = resources.getDimensionPixelSize(R.dimen.app_icon_size);


        mPinToTopComparator = new PinToTop();
        mRecentOrderComparator = new RecentOrder();
        mAlphabeticalOrderComparator = new AlphabeticalOrder();


        mNumOfCores = Runtime.getRuntime().availableProcessors();

        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addDataScheme("package");
        mPackageChangedReceiver = new PackageChangedReceiver();
        registerReceiver(mPackageChangedReceiver, filter);

        loadLaunchableApps();

        //loadShareableApps();
        setupImageLoadingThreads(resources);
        setupViews();

    }

    private void loadShareableApps() {
        List<ResolveInfo> infoList = ContentShare.getTextReceivers(mPm);

        for (ResolveInfo info : infoList) {
            final LaunchableActivity launchableActivity = new LaunchableActivity(
                    info.activityInfo, info.loadLabel(mPm).toString(), true);
            mShareableActivityInfos.add(launchableActivity);
        }
        updateApps(mShareableActivityInfos, false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsCacheClear = false;
        mSearchEditText.setText("");
        mSearchEditText.clearFocus();
        if (mAutoKeyboard) {
            mSearchEditText.requestFocus();
        }
    }

    public int setPaddingHeights() {
        int statusBarPaddings = 2;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {

            final Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

            final View statusBarDummy = findViewById(R.id.statusBarDummyView);
            statusBarDummy.getLayoutParams().height = mStatusBarHeight;
            statusBarPaddings++;
        }

        final View topFillerView = findViewById(R.id.topFillerView);
        topFillerView.getLayoutParams().height = mStatusBarHeight;

        final View bottomFillerView = findViewById(R.id.bottomFillerView);
        bottomFillerView.getLayoutParams().height = mStatusBarHeight;

        return statusBarPaddings;
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

        if (mAutoKeyboard) {
            showKeyboard();

            //HACK putting showKeyboard event to the end of the Ui Thread running queue
            // to make sure the keyboard opens.
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showKeyboard();
                }
            });
        } else {
            hideKeyboard();
        }


    }

    private void setupViews() {

        mSearchEditText.addTextChangedListener(mTextWatcher);
        mSearchEditText.setImeActionLabel(getString(R.string.launch), EditorInfo.IME_ACTION_GO);
        mSearchEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                if (actionId == EditorInfo.IME_ACTION_GO) {
                    Log.d("KEYBOARD", "ACTION_GO");
                    return openFirstActivity();
                }
                return false;
            }
        });
        mSearchEditText.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    Log.d("KEYBOARD", "ENTER_KEY");
                    return openFirstActivity();
                }
                return false;
            }
        });
        registerForContextMenu(mAppListView);

        ((GridView) mAppListView).setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState != SCROLL_STATE_IDLE) {
                    hideKeyboard();
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                                 int visibleItemCount, int totalItemCount) {

            }
        });
        //noinspection unchecked
        mAppListView.setAdapter(mArrayAdapter);


        mAppListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position >= mColumnCount) {
                    launchActivity(mActivityInfos.get(position - mColumnCount));
                }
            }

        });


    }

    private boolean openFirstActivity() {
        if (!mActivityInfos.isEmpty()) {
            launchActivity(mActivityInfos.get(0));
            return true;
        }
        return false;
    }

    private void setupPreferences() {
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        if (mSharedPreferences.getBoolean(SettingsActivity.KEY_PREF_NOTIFICATION, false)) {
            final ShortcutNotificationManager shortcutNotificationManager = new ShortcutNotificationManager();
            final String strPriority =
                    mSharedPreferences.getString(SettingsActivity.KEY_PREF_NOTIFICATION_PRIORITY,
                            "low");
            final int priority = ShortcutNotificationManager.getPriorityFromString(strPriority);
            shortcutNotificationManager.showNotification(this, priority);
        }
        mShouldOrderByRecents =
                mSharedPreferences.getString("pref_app_preferred_order", "recent").equals("recent");
        mDisableIcons =
                mSharedPreferences.getBoolean("pref_disable_icons", false);
        mAutoKeyboard =
                mSharedPreferences.getBoolean("pref_autokeyboard", false);
    }

    private void setupImageLoadingThreads(final Resources resources) {

        mImageLoadingConsumersManager =
                new SimpleTaskConsumerManager(getOptimalNumberOfThreads(resources),
                        mActivityInfos.size());
        mImageTasksSharedData = new ImageLoadingTask.SharedData(this, mPm, mContext, mIconSizePixels);
    }


    private int getOptimalNumberOfThreads(final Resources resources) {
        final int maxThreads = resources.getInteger(R.integer.max_imageloading_threads);
        int numThreads = mNumOfCores - 1;
        //clamp numThreads
        if (numThreads < 1) numThreads = 1;
        else if (numThreads > maxThreads) numThreads = maxThreads;
        return numThreads;
    }

    private void updateApps(final List<LaunchableActivity> updatedActivityInfos, boolean addToTrie) {

        for (LaunchableActivity launchableActivity : updatedActivityInfos) {
            final String packageName = launchableActivity.getComponent().getPackageName();
            mLaunchableActivityPackageNameHashMap.remove(packageName);
        }

        final String thisClassCanonicalName = this.getClass().getCanonicalName();
        for (LaunchableActivity launchableActivity : updatedActivityInfos) {
            final String className = launchableActivity.getComponent().getClassName();
            //don't show this activity in the launcher
            if (className.equals(thisClassCanonicalName)) {
                continue;
            }

            if (addToTrie) {
                final String activityLabel = launchableActivity.getActivityLabel();
                final List<String> subwords = getAllSubwords(stripAccents(activityLabel));
                for (String subword : subwords) {
                    mTrie.put(subword, launchableActivity);
                }
            }
            final String packageName = launchableActivity.getComponent().getPackageName();

            List<LaunchableActivity> launchableActivitiesToUpdate =
                    mLaunchableActivityPackageNameHashMap.remove(packageName);
            if (launchableActivitiesToUpdate == null) {
                launchableActivitiesToUpdate = new LinkedList<>();
            }
            launchableActivitiesToUpdate.add(launchableActivity);
            mLaunchableActivityPackageNameHashMap.put(packageName, launchableActivitiesToUpdate);
        }
        Log.d("SearchActivity", "updated activities: " + updatedActivityInfos.size());
        mLaunchableActivityPrefs.setAllPreferences(updatedActivityInfos);
        updateVisibleApps();
    }

    private List<String> getAllSubwords(String line) {
        final ArrayList<String> subwords = new ArrayList<>();
        for (int i = 0; i < line.length(); i++) {
            final char character = line.charAt(i);

            if (Character.isUpperCase(character) || Character.isDigit(character)) {
                if (mWordSinceLastCapitalBuilder.length() > 1) {
                    subwords.add(mWordSinceLastCapitalBuilder.toString().toLowerCase());
                }
                mWordSinceLastCapitalBuilder.setLength(0);

            }
            if (Character.isSpaceChar(character)) {
                subwords.add(mWordSinceLastSpaceBuilder.toString().toLowerCase());
                if (mWordSinceLastCapitalBuilder.length() > 1 &&
                        mWordSinceLastCapitalBuilder.length() !=
                                mWordSinceLastSpaceBuilder.length()) {
                    subwords.add(mWordSinceLastCapitalBuilder.toString().toLowerCase());
                }
                mWordSinceLastCapitalBuilder.setLength(0);
                mWordSinceLastSpaceBuilder.setLength(0);
            } else {
                mWordSinceLastCapitalBuilder.append(character);
                mWordSinceLastSpaceBuilder.append(character);
            }
        }
        if (mWordSinceLastSpaceBuilder.length() > 0) {
            subwords.add(mWordSinceLastSpaceBuilder.toString().toLowerCase());
        }
        if (mWordSinceLastCapitalBuilder.length() > 1
                && mWordSinceLastCapitalBuilder.length() != mWordSinceLastSpaceBuilder.length()) {
            subwords.add(mWordSinceLastCapitalBuilder.toString().toLowerCase());
        }
        mWordSinceLastSpaceBuilder.setLength(0);
        mWordSinceLastCapitalBuilder.setLength(0);
        return subwords;
    }

    private void updateVisibleApps() {
        final HashSet<LaunchableActivity> infoList =
                mTrie.getAllStartingWith(stripAccents(mSearchEditText.getText()
                        .toString().toLowerCase().trim()));
        mActivityInfos.clear();
        mActivityInfos.addAll(infoList);
        mActivityInfos.addAll(mShareableActivityInfos);
        sortApps();
        Log.d("DEBUG_SEARCH", mActivityInfos.size() + "");

        mArrayAdapter.notifyDataSetChanged();
    }

    private void sortApps() {
        Collections.sort(mActivityInfos, mAlphabeticalOrderComparator);
        if (mShouldOrderByRecents) {
            Collections.sort(mActivityInfos, mRecentOrderComparator);
        }
        Collections.sort(mActivityInfos, mPinToTopComparator);
    }

    private void removeActivitiesFromPackage(String packageName) {
        final List<LaunchableActivity> launchableActivitiesToRemove =
                mLaunchableActivityPackageNameHashMap.remove(packageName);
        if (launchableActivitiesToRemove == null) {
            return;
        }
        boolean activityListChanged = false;

        for (LaunchableActivity launchableActivityToRemove : launchableActivitiesToRemove) {
            final String className = launchableActivityToRemove.getClassName();
            Log.d("SearchActivity", "removing activity " + className);
            String activityLabel = launchableActivityToRemove.getActivityLabel();
            final List<String> subwords = getAllSubwords(stripAccents(activityLabel));
            for (String subword : subwords) {
                mTrie.remove(subword, launchableActivityToRemove);
            }
            if (mActivityInfos.remove(launchableActivityToRemove))
                activityListChanged = true;
            //TODO DEBUGME if uncommented the next line causes a crash.
            //mLaunchableActivityPrefs.deletePreference(className);
        }

        if (activityListChanged)
            mArrayAdapter.notifyDataSetChanged();
    }

    private boolean isCurrentLauncher() {
        final Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        final ResolveInfo resolveInfo =
                mPm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return resolveInfo != null &&
                mContext.getPackageName().equals(resolveInfo.activityInfo.packageName);

    }

    private String stripAccents(final String s) {
        return mPattern.matcher(Normalizer.normalize(s, Normalizer.Form.NFKD)).replaceAll("");
    }

    private void loadLaunchableApps() {

        List<ResolveInfo> infoList = ContentShare.getLaunchableResolveInfos(mPm);
        mArrayAdapter = new ActivityInfoArrayAdapter(this,
                R.layout.app_grid_item, mActivityInfos);
        ArrayList<LaunchableActivity> launchablesFromResolve = new ArrayList<>(infoList.size());

        if (mNumOfCores <= 1) {
            for (ResolveInfo info : infoList) {
                final LaunchableActivity launchableActivity = new LaunchableActivity(
                        info.activityInfo, info.activityInfo.loadLabel(mPm).toString(), false);
                launchablesFromResolve.add(launchableActivity);
            }
        } else {
            SimpleTaskConsumerManager simpleTaskConsumerManager =
                    new SimpleTaskConsumerManager(mNumOfCores, infoList.size());

            LoadLaunchableActivityTask.SharedData sharedAppLoadData =
                    new LoadLaunchableActivityTask.SharedData(mPm, launchablesFromResolve);
            for (ResolveInfo info : infoList) {
                LoadLaunchableActivityTask loadLaunchableActivityTask =
                        new LoadLaunchableActivityTask(info, sharedAppLoadData);
                simpleTaskConsumerManager.addTask(loadLaunchableActivityTask);
            }

            //Log.d("MultithreadStartup","waiting for completion of all tasks");
            simpleTaskConsumerManager.destroyAllConsumers(true, true);
            //Log.d("MultithreadStartup", "all tasks ok");
        }
        updateApps(launchablesFromResolve, true);
    }

    private void showKeyboard() {
        mInputMethodManager.showSoftInput(mSearchEditText, 0);
    }

    private void hideKeyboard() {
        mInputMethodManager.hideSoftInputFromWindow(mSearchEditText.getWindowToken(), 0);
    }

    private void handlePackageChanged() {
        final SharedPreferences.Editor editor = mSharedPreferences.edit();
        final String[] packageChangedNames = mSharedPreferences.getString("package_changed_name", "")
                .split(" ");
        editor.putString("package_changed_name", "");
        editor.apply();

        for (String packageName : packageChangedNames) {
            packageName = packageName.trim();
            if (packageName.isEmpty()) continue;

            final Intent intent = new Intent();
            intent.setAction(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setPackage(packageName);
            Log.d("SearchActivity", "changed: " + packageName);
            final List<ResolveInfo> infoList = mPm.queryIntentActivities(intent, 0);

            //we don't actually need to run removeActivitiesFromPackage if the package
            // is being installed
            removeActivitiesFromPackage(packageName);


            if (infoList.isEmpty()) {
                Log.d("SearchActivity", "No activities in list. Uninstall detected!");
                updateVisibleApps();
            } else {
                Log.d("SearchActivity", "Activities in list. Install/update detected!");
                ArrayList<LaunchableActivity> launchablesFromResolve = new ArrayList<>(infoList.size());
                for (ResolveInfo info : infoList) {
                    final LaunchableActivity launchableActivity = new LaunchableActivity(
                            info.activityInfo, info.activityInfo.loadLabel(mPm).toString(), false);
                    launchablesFromResolve.add(launchableActivity);
                }
                updateApps(launchablesFromResolve, true);
            }

        }


    }

    @Override
    public void onBackPressed() {
        if (!isCurrentLauncher())
            moveTaskToBack(false);
    }

    @Override
    protected void onDestroy() {
        if (mImageLoadingConsumersManager != null)
            mImageLoadingConsumersManager.destroyAllConsumers(false);
        unregisterReceiver(mPackageChangedReceiver);
        Log.d("HayaiLauncher", "Hayai is ded");
        super.onDestroy();
    }

    public void showPopup(View v) {
        final PopupMenu popup = new PopupMenu(this, v);
        popup.setOnMenuItemClickListener(new PopupEventListener());
        final MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.search_activity_menu, popup.getMenu());
        popup.show();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        //does this need to run in uiThread?
        if (key.equals("package_changed_name") && !sharedPreferences.getString(key, "").isEmpty()) {
            handlePackageChanged();
        } else if (key.equals("pref_app_preferred_order")) {
            mShouldOrderByRecents =
                    mSharedPreferences.getString("pref_app_preferred_order", "recent").equals("recent");
            sortApps();
            mArrayAdapter.notifyDataSetChanged();
        } else if (key.equals("pref_disable_icons")) {
            recreate();
        } else if (key.equals("pref_autokeyboard")) {
            mAutoKeyboard = mSharedPreferences.getBoolean("pref_autokeyboard", false);
        }


    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            showPopup(mOverflowButtonTopleft);
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (!mIsCacheClear && level == TRIM_MEMORY_COMPLETE)
            clearCaches();

    }

    private void clearCaches() {
        mIsCacheClear = true;
        for (LaunchableActivity launchableActivity : mActivityInfos) {
            launchableActivity.deleteActivityIcon();
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (menuInfo instanceof AdapterContextMenuInfo) {
            AdapterContextMenuInfo adapterMenuInfo = (AdapterContextMenuInfo) menuInfo;
            menu.setHeaderTitle(
                    ((LaunchableActivity) adapterMenuInfo.targetView
                            .findViewById(R.id.appIcon).getTag()).getActivityLabel());
        }
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.app, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_settings:
                final Intent intentSettings = new Intent(this, SettingsActivity.class);
                startActivity(intentSettings);
                return true;
            case R.id.action_refresh_app_list:
                recreate();
                return true;
            case R.id.action_system_settings:
                final Intent intentSystemSettings = new Intent(Settings.ACTION_SETTINGS);
                intentSystemSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intentSystemSettings);
                return true;
            case R.id.action_manage_apps:
                final Intent intentManageApps = new Intent(Settings.ACTION_APPLICATION_SETTINGS);
                intentManageApps.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intentManageApps);
                return true;
            case R.id.action_set_wallpaper:
                final Intent intentWallpaperPicker = new Intent(Intent.ACTION_SET_WALLPAPER);
                startActivity(intentWallpaperPicker);
                return true;
            case R.id.action_about:
                final Intent intentAbout = new Intent(this, AboutActivity.class);
                startActivity(intentAbout);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
                .getMenuInfo();
        final View itemView = info.targetView;
        final LaunchableActivity launchableActivity =
                (LaunchableActivity) itemView.findViewById(R.id.appIcon).getTag();
        switch (item.getItemId()) {
            case R.id.appmenu_launch:
                launchActivity(launchableActivity);
                return true;
            case R.id.appmenu_info:
                final Intent intent = new Intent(
                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:"
                        + launchableActivity.getComponent().getPackageName()));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                return true;
            case R.id.appmenu_onplaystore:
                final Intent intentPlayStore = new Intent(Intent.ACTION_VIEW);
                intentPlayStore.setData(Uri.parse("market://details?id=" +
                        launchableActivity.getComponent().getPackageName()));
                startActivity(intentPlayStore);
                return true;
            case R.id.appmenu_pin_to_top:
                launchableActivity.setPriority(launchableActivity.getPriority() == 0 ? 1 : 0);
                mLaunchableActivityPrefs.writePreference(launchableActivity.getClassName(),
                        launchableActivity.getLaunchTime(), launchableActivity.getPriority());
                sortApps();
                mArrayAdapter.notifyDataSetChanged();
                return true;
            default:
                return false;
        }

    }

    public void onClickSettingsButton(View view) {
        showPopup(mOverflowButtonTopleft);


    }

    public void launchActivity(final LaunchableActivity launchableActivity) {

        hideKeyboard();
        try {
            startActivity(launchableActivity.getLaunchIntent(mSearchEditText.getText().toString()));
            launchableActivity.setLaunchTime();
            mLaunchableActivityPrefs.writePreference(launchableActivity.getClassName(),
                    launchableActivity.getLaunchTime(), launchableActivity.getPriority());
            sortApps();
            mArrayAdapter.notifyDataSetChanged();
        } catch (ActivityNotFoundException e) {
            //this should only happen when the launcher still hasn't updated the file list after
            //an activity removal.
            Toast.makeText(mContext, getString(R.string.activity_not_found),
                    Toast.LENGTH_SHORT).show();
        }


    }

    public void onClickClearButton(View view) {
        mSearchEditText.setText("");
    }

    class PopupEventListener implements PopupMenu.OnMenuItemClickListener {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            return onOptionsItemSelected(item);
        }
    }

    class ActivityInfoArrayAdapter extends ArrayAdapter<LaunchableActivity> {
        final LayoutInflater inflater;

        public ActivityInfoArrayAdapter(final Context context, final int resource,
                                        final List<LaunchableActivity> activityInfos) {

            super(context, resource, activityInfos);
            inflater = getLayoutInflater();
        }

        @Override
        public int getCount() {
            return super.getCount() + mColumnCount;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            final View view =
                    convertView != null ?
                            convertView : inflater.inflate(R.layout.app_grid_item, parent, false);
            final AbsListView.LayoutParams params =
                    (AbsListView.LayoutParams) view.getLayoutParams();

            if (position < mColumnCount) {
                params.height = mGridViewTopRowHeight;
                view.setLayoutParams(params);
                view.setVisibility(View.INVISIBLE);
            } else {
                if (position == (getCount() - 1)) {
                    params.height = mGridViewBottomRowHeight;
                } else {
                    params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                }
                view.setLayoutParams(params);
                view.setVisibility(View.VISIBLE);
                final LaunchableActivity launchableActivity = getItem(position - mColumnCount);
                final CharSequence label = launchableActivity.getActivityLabel();
                final TextView appLabelView = (TextView) view.findViewById(R.id.appLabel);
                final ImageView appIconView = (ImageView) view.findViewById(R.id.appIcon);
                final View appShareIndicator = view.findViewById(R.id.appShareIndicator);
                final View appPinToTop = view.findViewById(R.id.appPinToTop);

                appLabelView.setText(label);

                appIconView.setTag(launchableActivity);
                if (!launchableActivity.isIconLoaded()) {
                    appIconView.setImageDrawable(mDefaultAppIcon);
                    if (!mDisableIcons)
                        mImageLoadingConsumersManager.addTask(
                                new ImageLoadingTask(appIconView, launchableActivity,
                                        mImageTasksSharedData));
                } else {
                    appIconView.setImageDrawable(
                            launchableActivity.getActivityIcon(mPm, mContext, mIconSizePixels));
                }
                appShareIndicator.setVisibility(
                        launchableActivity.isShareable() ? View.VISIBLE : View.GONE);
                appPinToTop.setVisibility(
                        launchableActivity.getPriority() > 0 ? View.VISIBLE : View.GONE);

            }
            return view;
        }

    }

}
