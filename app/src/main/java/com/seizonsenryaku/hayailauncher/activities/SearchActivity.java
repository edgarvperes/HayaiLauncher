package com.seizonsenryaku.hayailauncher.activities;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
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
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
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

import com.seizonsenryaku.hayailauncher.ImageLoadingTask;
import com.seizonsenryaku.hayailauncher.LaunchableActivity;
import com.seizonsenryaku.hayailauncher.LaunchableActivityPrefs;
import com.seizonsenryaku.hayailauncher.MyNotificationManager;
import com.seizonsenryaku.hayailauncher.R;
import com.seizonsenryaku.hayailauncher.StatusBarColorHelper;
import com.seizonsenryaku.hayailauncher.Trie;
import com.seizonsenryaku.hayailauncher.threading.SimpleTaskConsumerManager;
import com.seizonsenryaku.hayailauncher.util.ContentShare;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
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

        float dpWidth = displayMetrics.widthPixels / displayDensity;
        final float itemWidth = 72;//TODO remove magic number
        mColumnCount = (int) (dpWidth / itemWidth) - 1;

        mSharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(this);
        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
        mLaunchableActivityPrefs = new LaunchableActivityPrefs(this);

        //noinspection deprecation
        mDefaultAppIcon = resources.getDrawable(R.drawable.ic_blur_on_black_48dp);
        mIconSizePixels = resources.getDimensionPixelSize(R.dimen.app_icon_size);


        setupPreferences();
        loadLaunchableApps();
        loadShareableApps();
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
        mSearchEditText.clearFocus();
        mSearchEditText.requestFocus();
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
        showKeyboard();

        //HACK putting showKeyboard event to the end of the Ui Thread running queue
        // to make sure the keyboard opens.
        final Thread keyboardEventPosterThread = new Thread(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showKeyboard();
                    }
                });
            }
        });
        keyboardEventPosterThread.start();


    }

    private void setupViews() {
        //noinspection deprecation
        ((ImageView) findViewById(R.id.backgroundView)).setImageDrawable(
                WallpaperManager.getInstance(this).getFastDrawable());

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
            final MyNotificationManager myNotificationManager = new MyNotificationManager();
            final String strPriority =
                    mSharedPreferences.getString(SettingsActivity.KEY_PREF_NOTIFICATION_PRIORITY,
                            "low");
            final int priority = MyNotificationManager.getPriorityFromString(strPriority);
            myNotificationManager.showNotification(this, priority);
        }
    }

    private void setupImageLoadingThreads(final Resources resources) {
        final int maxThreads = resources.getInteger(R.integer.max_imageloading_threads);
        int numThreads = Runtime.getRuntime().availableProcessors() - 1;
        //clamp numThreads
        if (numThreads < 1) numThreads = 1;
        else if (numThreads > maxThreads) numThreads = maxThreads;
        mImageLoadingConsumersManager = new SimpleTaskConsumerManager(numThreads);
        mImageTasksSharedData = new ImageLoadingTask.SharedData(this, mPm, mContext, mIconSizePixels);
    }

    private void updateApps(final List<LaunchableActivity> updatedActivityInfos, boolean addToTrie) {

        for (LaunchableActivity launchableActivity : updatedActivityInfos) {
            final String packageName = launchableActivity.getComponent().getPackageName();
            mLaunchableActivityPackageNameHashMap.remove(packageName);
        }

        for (LaunchableActivity launchableActivity : updatedActivityInfos) {
            final String className = launchableActivity.getComponent().getClassName();
            //don't show this activity in the launcher
            if (className.equals(this.getClass().getCanonicalName())) {
                continue;
            }

            if (addToTrie) {
                final String activityLabel = launchableActivity.getActivityLabel().toString();
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
        Collections.sort(mActivityInfos);
        Log.d("DEBUG_SEARCH", mActivityInfos.size() + "");

        mArrayAdapter.notifyDataSetChanged();
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
            String activityLabel = launchableActivityToRemove.getActivityLabel().toString();
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
        for (ResolveInfo info : infoList) {
            final LaunchableActivity launchableActivity = new LaunchableActivity(
                    info.activityInfo, info.activityInfo.loadLabel(mPm).toString(), false);
            launchablesFromResolve.add(launchableActivity);
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
        super.onDestroy();
    }

    public boolean showPopup(View v) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            final PopupMenu popup = new PopupMenu(this, v);
            popup.setOnMenuItemClickListener(new PopupEventListener());
            final MenuInflater inflater = popup.getMenuInflater();
            inflater.inflate(R.menu.search_activity_menu, popup.getMenu());
            popup.show();
            return true;
        }
        return false;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("package_changed_name") && !sharedPreferences.getString(key, "").isEmpty()) {
            //does this need to run in uiThread?
            handlePackageChanged();
        }
    }

    public boolean onKeyUp(int keyCode, @NonNull KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (!showPopup(mOverflowButtonTopleft)) {
                openOptionsMenu();
            }
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.search_activity_menu, menu);
        return true;

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
            default:
                return false;
        }

    }

    @Override
    public void recreate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            super.recreate();
        } else {
            final Intent intentRefresh = new Intent(this, SearchActivity.class);
            finish();
            startActivity(intentRefresh);
        }
    }

    public void onClickSettingsButton(View view) {
        if (!showPopup(mOverflowButtonTopleft)) {
            openOptionsMenu();
        }

    }

    public void launchActivity(final LaunchableActivity launchableActivity) {

        final ComponentName componentName = launchableActivity.getComponent();

        hideKeyboard();

        try {
            startActivity(launchableActivity.getLaunchIntent(mSearchEditText.getText().toString()));
            launchableActivity.incrementLaunches();
            Collections.sort(mActivityInfos);
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

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
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

                appLabelView.setText(label);

                appIconView.setTag(launchableActivity);
                if (!launchableActivity.isIconLoaded()) {
                    appIconView.setImageDrawable(mDefaultAppIcon);
                    mImageLoadingConsumersManager.addTask(
                            new ImageLoadingTask(appIconView, launchableActivity,
                                    mImageTasksSharedData));
                } else {
                    appIconView.setImageDrawable(
                            launchableActivity.getActivityIcon(mPm, mContext, mIconSizePixels));
                }
                appShareIndicator.setVisibility(
                        launchableActivity.isShareable() ? View.VISIBLE : View.GONE);
            }
            return view;
        }

    }

}
