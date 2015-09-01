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
    private static final int navigationBarHeightMultiplier = 1;
    private static final int gridViewTopRowExtraPaddingInDP = 56;
    private static final int marginFromNavigationBarInDp = 16;
    private static final int gridItemHeightInDp = 96;
    private final Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private int statusBarHeight;
    private ArrayList<LaunchableActivity> activityInfos;
    private ArrayList<LaunchableActivity> shareableActivityInfos;
    private Trie<LaunchableActivity> trie;
    private ArrayAdapter<LaunchableActivity> arrayAdapter;
    private HashMap<String, List<LaunchableActivity>> launchableActivityPackageNameHashMap;
    private LaunchableActivityPrefs launchableActivityPrefs;
    private SharedPreferences sharedPreferences;
    private Context context;
    private Drawable defaultAppIcon;
    private SimpleTaskConsumerManager imageLoadingConsumersManager;
    private ImageLoadingTask.SharedData imageTasksSharedData;
    private int iconSizePixels;
    private EditText searchEditText;
    private View clearButton;


    private final TextWatcher textWatcher = new TextWatcher() {

        @Override
        public void onTextChanged(CharSequence s, int start, int before,
                                  int count) {
            clearButton.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
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
    private InputMethodManager inputMethodManager;
    private AdapterView appListView;
    private PackageManager pm;
    private View overflowButtonTopleft;
    private int column_count;

    //used only in function getAllSubwords. they are here as class fields to avoid
    // object re-allocation.
    private StringBuilder wordSinceLastSpaceBuilder;
    private StringBuilder wordSinceLastCapitalBuilder;


    private int gridViewTopRowHeight;
    private int gridViewBottomRowHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        pm = getPackageManager();

        final Resources resources = getResources();

        //fields:
        launchableActivityPackageNameHashMap = new HashMap<>();
        shareableActivityInfos = new ArrayList<>();
        trie = new Trie<>();
        wordSinceLastSpaceBuilder = new StringBuilder(64);
        wordSinceLastCapitalBuilder = new StringBuilder(64);

        searchEditText = (EditText) findViewById(R.id.editText1);
        appListView = (GridView) findViewById(R.id.appsContainer);
        clearButton = findViewById(R.id.clear_button);
        overflowButtonTopleft = findViewById(R.id.overflow_button_topleft);
        context = getApplicationContext();
        inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        statusBarHeight = StatusBarColorHelper.getStatusBarHeight(resources);
        final DisplayMetrics displayMetrics = resources.getDisplayMetrics();
        final float displayDensity = displayMetrics.density;
        final int gridViewTopRowExtraPaddingInPixels =
                Math.round(displayDensity * gridViewTopRowExtraPaddingInDP);
        final int marginFromNavigationBarInPixels =
                Math.round(displayDensity * marginFromNavigationBarInDp);
        final int gridItemHeightInPixels =
                Math.round(displayDensity * gridItemHeightInDp);
        int statusBarMultiplierPaddings = setPaddingHeights();
        gridViewTopRowHeight = statusBarMultiplierPaddings * statusBarHeight +
                gridViewTopRowExtraPaddingInPixels;
        gridViewBottomRowHeight = gridItemHeightInPixels + navigationBarHeightMultiplier *
                StatusBarColorHelper.getNavigationBarHeight(getResources()) +
                marginFromNavigationBarInPixels;

        float dpWidth = displayMetrics.widthPixels / displayDensity;
        final float itemWidth = 72;//TODO remove magic number
        column_count =  Math.round(dpWidth/itemWidth) - 1;

        sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        launchableActivityPrefs = new LaunchableActivityPrefs(this);

        //noinspection deprecation
        defaultAppIcon = resources.getDrawable(R.drawable.ic_blur_on_black_48dp);
        iconSizePixels = resources.getDimensionPixelSize(R.dimen.app_icon_size);


        setupPreferences();
        loadShareableApps();
        loadLaunchableApps();
        setupImageLoadingThreads(resources);
        setupViews();
    }

    private void loadShareableApps() {
        List<ResolveInfo> infoList = ContentShare.getTextReceivers(pm);
        activityInfos = new ArrayList<>(infoList.size());
        arrayAdapter = new ActivityInfoArrayAdapter(this,
                R.layout.app_grid_item, activityInfos);
        for (ResolveInfo info : infoList) {
            final LaunchableActivity launchableActivity = new LaunchableActivity(
                    info.activityInfo, info.loadLabel(pm).toString(), true);
            shareableActivityInfos.add(launchableActivity);
        }
        updateApps(shareableActivityInfos, false);
    }

    @Override
        protected void onResume() {
        super.onResume();
        searchEditText.clearFocus();
        searchEditText.requestFocus();
    }

    public int setPaddingHeights() {
        int statusBarPaddings = 2;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {

            final Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

            final View statusBarDummy = findViewById(R.id.statusBarDummyView);
            statusBarDummy.getLayoutParams().height = statusBarHeight;
            statusBarPaddings++;
        }

        final View topFillerView = findViewById(R.id.topFillerView);
        topFillerView.getLayoutParams().height = statusBarHeight;

        final View bottomFillerView = findViewById(R.id.bottomFillerView);
        bottomFillerView.getLayoutParams().height = statusBarHeight;

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
        findViewById(R.id.masterLayout).setBackgroundDrawable(
                WallpaperManager.getInstance(this).getFastDrawable());

        searchEditText.addTextChangedListener(textWatcher);
        searchEditText.setImeActionLabel(getString(R.string.launch), EditorInfo.IME_ACTION_GO);
        searchEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    if (!activityInfos.isEmpty()) {
                        launchActivity(activityInfos.get(0));
                        return true;
                    }
                }
                return false;
            }
        });
        registerForContextMenu(appListView);

        ((GridView) appListView).setOnScrollListener(new AbsListView.OnScrollListener() {
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
        appListView.setAdapter(arrayAdapter);


        appListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position >= column_count) {
                    launchActivity(activityInfos.get(position - column_count));
                }
            }

        });


    }

    private void setupPreferences() {
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        if (sharedPreferences.getBoolean(SettingsActivity.KEY_PREF_NOTIFICATION, false)) {
            final MyNotificationManager myNotificationManager = new MyNotificationManager();
            final String strPriority =
                    sharedPreferences.getString(SettingsActivity.KEY_PREF_NOTIFICATION_PRIORITY,
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
        imageLoadingConsumersManager = new SimpleTaskConsumerManager(numThreads);
        imageTasksSharedData = new ImageLoadingTask.SharedData(this, pm, context, iconSizePixels);
    }

    private void updateApps(final List<LaunchableActivity> updatedActivityInfos, boolean addToTrie) {

        for (LaunchableActivity launchableActivity : updatedActivityInfos) {
            final String packageName = launchableActivity.getComponent().getPackageName();
            launchableActivityPackageNameHashMap.remove(packageName);
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
                    trie.put(subword, launchableActivity);
                }
            }
            final String packageName = launchableActivity.getComponent().getPackageName();

            List<LaunchableActivity> launchableActivitiesToUpdate =
                    launchableActivityPackageNameHashMap.remove(packageName);
            if (launchableActivitiesToUpdate == null) {
                launchableActivitiesToUpdate = new LinkedList<>();
            }
            launchableActivitiesToUpdate.add(launchableActivity);
            launchableActivityPackageNameHashMap.put(packageName, launchableActivitiesToUpdate);
        }
        Log.d("SearchActivity", "updated activities: " + updatedActivityInfos.size());
        launchableActivityPrefs.setAllPreferences(updatedActivityInfos);
        updateVisibleApps();
    }

    private List<String> getAllSubwords(String line) {
        final ArrayList<String> subwords = new ArrayList<>();
        for (int i = 0; i < line.length(); i++) {
            final char character = line.charAt(i);

            if (Character.isUpperCase(character) || Character.isDigit(character)) {
                if (wordSinceLastCapitalBuilder.length() > 1) {
                    subwords.add(wordSinceLastCapitalBuilder.toString().toLowerCase());
                }
                wordSinceLastCapitalBuilder.setLength(0);

            }
            if (Character.isSpaceChar(character)) {
                subwords.add(wordSinceLastSpaceBuilder.toString().toLowerCase());
                if (wordSinceLastCapitalBuilder.length() > 1 &&
                        wordSinceLastCapitalBuilder.length() !=
                                wordSinceLastSpaceBuilder.length()) {
                    subwords.add(wordSinceLastCapitalBuilder.toString().toLowerCase());
                }
                wordSinceLastCapitalBuilder.setLength(0);
                wordSinceLastSpaceBuilder.setLength(0);
            } else {
                wordSinceLastCapitalBuilder.append(character);
                wordSinceLastSpaceBuilder.append(character);
            }
        }
        if (wordSinceLastSpaceBuilder.length() > 0) {
            subwords.add(wordSinceLastSpaceBuilder.toString().toLowerCase());
        }
        if (wordSinceLastCapitalBuilder.length() > 1
                && wordSinceLastCapitalBuilder.length() != wordSinceLastSpaceBuilder.length()) {
            subwords.add(wordSinceLastCapitalBuilder.toString().toLowerCase());
        }
        wordSinceLastSpaceBuilder.setLength(0);
        wordSinceLastCapitalBuilder.setLength(0);
        return subwords;
    }

    private void updateVisibleApps() {
        final HashSet<LaunchableActivity> infoList =
                trie.getAllStartingWith(stripAccents(searchEditText.getText()
                        .toString().toLowerCase().trim()));
        activityInfos.clear();
        activityInfos.addAll(infoList);
        activityInfos.addAll(shareableActivityInfos);
        Collections.sort(activityInfos);
        Log.d("DEBUG_SEARCH", activityInfos.size() + "");

        arrayAdapter.notifyDataSetChanged();
    }

    private void removeActivitiesFromPackage(String packageName) {
        final List<LaunchableActivity> launchableActivitiesToRemove =
                launchableActivityPackageNameHashMap.remove(packageName);
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
                trie.remove(subword,launchableActivityToRemove);
            }
            if (activityInfos.remove(launchableActivityToRemove))
                activityListChanged = true;
            //TODO DEBUGME if uncommented the next line causes a crash.
            //launchableActivityPrefs.deletePreference(className);
        }

        if (activityListChanged)
            arrayAdapter.notifyDataSetChanged();
    }

    private boolean isCurrentLauncher() {
        final Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        final ResolveInfo resolveInfo =
                pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return resolveInfo != null &&
                context.getPackageName().equals(resolveInfo.activityInfo.packageName);

    }

    private String stripAccents(final String s) {
        return pattern.matcher(Normalizer.normalize(s, Normalizer.Form.NFKD)).replaceAll("");
    }

    private void loadLaunchableApps() {

        List<ResolveInfo> infoList = ContentShare.getLaunchableResolveInfos(pm);
        activityInfos = new ArrayList<>(infoList.size());
        arrayAdapter = new ActivityInfoArrayAdapter(this,
                R.layout.app_grid_item, activityInfos);
        ArrayList<LaunchableActivity> launchablesFromResolve=new ArrayList<>(infoList.size());
        for(ResolveInfo info:infoList){
            final LaunchableActivity launchableActivity = new LaunchableActivity(
                    info.activityInfo, info.activityInfo.loadLabel(pm).toString(), false);
            launchablesFromResolve.add(launchableActivity);
        }
        updateApps(launchablesFromResolve, true);
    }

    private void showKeyboard() {
        inputMethodManager.showSoftInput(searchEditText, 0);
    }

    private void hideKeyboard() {
        inputMethodManager.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
    }

    private void handlePackageChanged() {
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        final String[] packageChangedNames = sharedPreferences.getString("package_changed_name", "")
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
            final List<ResolveInfo> infoList = pm.queryIntentActivities(intent, 0);

            //we don't actually need to run removeActivitiesFromPackage if the package
            // is being installed
            removeActivitiesFromPackage(packageName);


            if (infoList.isEmpty()) {
                Log.d("SearchActivity", "No activities in list. Uninstall detected!");
                updateVisibleApps();
            } else {
                Log.d("SearchActivity", "Activities in list. Install/update detected!");
                ArrayList<LaunchableActivity> launchablesFromResolve=new ArrayList<>(infoList.size());
                for(ResolveInfo info:infoList){
                    final LaunchableActivity launchableActivity = new LaunchableActivity(
                            info.activityInfo, info.activityInfo.loadLabel(pm).toString(), false);
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
        if (imageLoadingConsumersManager != null)
            imageLoadingConsumersManager.destroyAllConsumers(false);
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
            if (!showPopup(overflowButtonTopleft)) {
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
        if (!showPopup(overflowButtonTopleft)) {
            openOptionsMenu();
        }

    }

    public void launchActivity(final LaunchableActivity launchableActivity) {

        final ComponentName componentName = launchableActivity.getComponent();

        hideKeyboard();

        try {
            startActivity(launchableActivity.getLaunchIntent(searchEditText.getText().toString()));
            launchableActivity.incrementLaunches();
            Collections.sort(activityInfos);
            arrayAdapter.notifyDataSetChanged();
        } catch (ActivityNotFoundException e) {
            //this should only happen when the launcher still hasn't updated the file list after
            //an activity removal.
            Toast.makeText(context, getString(R.string.activity_not_found),
                    Toast.LENGTH_SHORT).show();
        }


    }

    public void onClickClearButton(View view) {
        searchEditText.setText("");
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
            return super.getCount() + column_count;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            final View view =
                    convertView != null ?
                            convertView : inflater.inflate(R.layout.app_grid_item, parent, false);
            final AbsListView.LayoutParams params =
                    (AbsListView.LayoutParams) view.getLayoutParams();

            if (position < column_count) {
                params.height = gridViewTopRowHeight;
                view.setLayoutParams(params);
                view.setVisibility(View.INVISIBLE);
            } else {
                if (position == (getCount() - 1)) {
                    params.height = gridViewBottomRowHeight;
                } else {
                    params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                }
                view.setLayoutParams(params);
                view.setVisibility(View.VISIBLE);
                final LaunchableActivity launchableActivity = getItem(position - column_count);
                final CharSequence label = launchableActivity.getActivityLabel();
                final TextView appLabelView = (TextView) view.findViewById(R.id.appLabel);
                final ImageView appIconView = (ImageView) view.findViewById(R.id.appIcon);
                final View appShareIndicator = view.findViewById(R.id.appShareIndicator);

                appLabelView.setText(label);

                appIconView.setTag(launchableActivity);
                if (!launchableActivity.isIconLoaded()) {
                    appIconView.setImageDrawable(defaultAppIcon);
                    imageLoadingConsumersManager.addTask(
                            new ImageLoadingTask(appIconView, launchableActivity,
                                    imageTasksSharedData));
                } else {
                    appIconView.setImageDrawable(
                            launchableActivity.getActivityIcon(pm, context, iconSizePixels));
                }
                appShareIndicator.setVisibility(
                        launchableActivity.isShareable() ? View.VISIBLE : View.GONE);
            }
            return view;
        }

    }

}
