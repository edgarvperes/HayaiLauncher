package com.seizonsenryaku.hayailauncher.activities;

import android.annotation.TargetApi;
import android.app.Activity;
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
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
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

import com.seizonsenryaku.hayailauncher.ImageLoadingTask;
import com.seizonsenryaku.hayailauncher.LaunchableActivity;
import com.seizonsenryaku.hayailauncher.LaunchableActivityPrefs;
import com.seizonsenryaku.hayailauncher.MyNotificationManager;
import com.seizonsenryaku.hayailauncher.R;
import com.seizonsenryaku.hayailauncher.SimpleTaskConsumerManager;
import com.seizonsenryaku.hayailauncher.StatusBarColorHelper;
import com.seizonsenryaku.hayailauncher.Trie;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class SearchActivity extends Activity {

    private ArrayList<LaunchableActivity> activityInfos;
    private Trie<LaunchableActivity> trie;
    private ArrayAdapter<LaunchableActivity> arrayAdapter;
    private LaunchableActivityPrefs launchableActivityPrefs;
    private SharedPreferences sharedPreferences;
    private Context context;
    private Drawable defaultAppIcon;
    private SimpleTaskConsumerManager imageLoadingConsumersManager;
    private ImageLoadingTask.SharedData imageTasksSharedData;
    private int iconSizePixels;
    private EditText searchEditText;
    private AdapterView appListView;
    private View overflowButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        final PackageManager pm = getPackageManager();
        final Resources resources = getResources();

        //fields:
        searchEditText = (EditText) findViewById(R.id.editText1);
        appListView = (AdapterView<ArrayAdapter<LaunchableActivity>>)
                findViewById(R.id.appsContainer);
        overflowButton = findViewById(R.id.overflow_button);

        context = getApplicationContext();
        sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(this);
        launchableActivityPrefs = new LaunchableActivityPrefs(this);

        defaultAppIcon = resources.getDrawable(R.drawable.ic_launcher);
        iconSizePixels = (int) (resources.getInteger(R.integer.icon_size)
                * resources.getDisplayMetrics().density + 0.5f);

        setupPreferences();
        loadLaunchableApps(pm);
        setupImageLoadingThreads(pm, resources);

        arrayAdapter = new ActivityInfoArrayAdapter(this,
                R.layout.app_grid_item, activityInfos);
        setupViews();

        //change status bar color. only needed on kitkat atm.
        StatusBarColorHelper.setStatusBarColor(resources,
                this, resources.getColor(R.color.indigo_700));

        //display soft keyboard
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    private void setupViews() {
        searchEditText.requestFocus();
        searchEditText.addTextChangedListener(textWatcher);

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
        appListView.setAdapter(arrayAdapter);


        appListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                launchActivity(activityInfos.get(position));
            }

        });
    }

    private void setupPreferences() {
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        if (sharedPreferences.getBoolean(
                SettingsActivity.KEY_PREF_NOTIFICATION, false)) {
            final MyNotificationManager myNotificationManager = new MyNotificationManager();
            myNotificationManager.showNotification(this);
        }
    }

    private void setupImageLoadingThreads(final PackageManager pm, Resources resources) {
        int numThreads = Runtime.getRuntime().availableProcessors() - 1;
        final int maxThreads = resources.getInteger(R.integer.max_imageloading_threads);
        if (numThreads < 1) numThreads = 1;
        else if (numThreads > maxThreads) numThreads = maxThreads;
        imageLoadingConsumersManager = new SimpleTaskConsumerManager(numThreads);
        imageTasksSharedData = new ImageLoadingTask.SharedData(this, pm, context, iconSizePixels);
    }

    private void loadLaunchableApps(final PackageManager pm) {

        final Intent intent = new Intent();
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        final List<ResolveInfo> infoList = pm.queryIntentActivities(intent, 0);
        trie = new Trie<>();

        final StringBuilder wordSinceLastSpaceBuilder = new StringBuilder(64);
        final StringBuilder wordSinceLastCapitalBuilder = new StringBuilder(64);

        for (ResolveInfo info : infoList) {
            final LaunchableActivity launchableActivity = new LaunchableActivity(
                    info.activityInfo, info.activityInfo.loadLabel(pm).toString());

            //don't show this activity in the launcher
            if (launchableActivity.getClassName().equals(this.getClass().getCanonicalName())) {
                continue;
            }

            final String activityLabel = launchableActivity.getActivityLabel().toString();
            final String activityLabelLower = activityLabel.toLowerCase();
            trie.put(activityLabelLower, launchableActivity);

            boolean skippedFirstWord = false;
            boolean previousCharWasUppercaseOrDigit = false;
            for (int i = 0; i < activityLabel.length(); i++) {
                final char character = activityLabel.charAt(i);

                if (Character.isUpperCase(character) || Character.isDigit(character)) {
                    if (wordSinceLastCapitalBuilder.length() > 1
                            && !activityLabel.startsWith(wordSinceLastCapitalBuilder.toString())) {
                        trie.put(wordSinceLastCapitalBuilder.toString().toLowerCase(),
                                launchableActivity);
                        if (!previousCharWasUppercaseOrDigit)
                            wordSinceLastCapitalBuilder.setLength(0);
                    }
                    previousCharWasUppercaseOrDigit = true;
                } else {
                    previousCharWasUppercaseOrDigit = false;
                }
                if (Character.isSpaceChar(character)) {
                    if (skippedFirstWord) {
                        trie.put(wordSinceLastSpaceBuilder.toString().toLowerCase(),
                                launchableActivity);
                        if (wordSinceLastCapitalBuilder.length() > 1 &&
                                wordSinceLastCapitalBuilder.length() !=
                                        wordSinceLastSpaceBuilder.length()) {
                            trie.put(wordSinceLastCapitalBuilder.toString().toLowerCase(),
                                    launchableActivity);
                        }
                    } else {
                        skippedFirstWord = true;
                    }

                    wordSinceLastCapitalBuilder.setLength(0);
                    wordSinceLastSpaceBuilder.setLength(0);
                } else {
                    wordSinceLastCapitalBuilder.append(character);
                    wordSinceLastSpaceBuilder.append(character);
                }
            }
            if (skippedFirstWord && wordSinceLastSpaceBuilder.length() > 0
                    && activityLabel.length() > wordSinceLastSpaceBuilder.length()) {
                trie.put(wordSinceLastSpaceBuilder.toString().toLowerCase(), launchableActivity);
            }
            if (wordSinceLastCapitalBuilder.length() > 1
                    && wordSinceLastCapitalBuilder.length() != wordSinceLastSpaceBuilder.length()) {
                trie.put(wordSinceLastCapitalBuilder.toString().toLowerCase(), launchableActivity);
            }
            wordSinceLastSpaceBuilder.setLength(0);
            wordSinceLastCapitalBuilder.setLength(0);
        }

        activityInfos = new ArrayList<>(infoList.size());
        activityInfos.addAll(trie.getAllStartingWith(""));
        launchableActivityPrefs.setAllPreferences(activityInfos);

        Collections.sort(activityInfos);
    }

    private void hideKeyboard() {

        ((InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE)).
                hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (sharedPreferences.getBoolean("package_changed", false)) {
            final SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("package_changed", false).apply();
            String[] packageChangedNames = sharedPreferences.getString("package_changed_name", "")
                    .split(" ");
            //TODO do something with the package names instead of refreshing the entire app list
            refreshAppList();

            editor.putString("package_changed_name", "");

        }
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

    @TargetApi(11)
    class PopupEventListener implements PopupMenu.OnMenuItemClickListener {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            return onOptionsItemSelected(item);
        }
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (!showPopup(overflowButton)) {
                openOptionsMenu();
            }
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.search_activity_menu, menu);
        return true;

    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.app, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_settings:
                final Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_refresh_app_list:
                refreshAppList();
                return true;
            case R.id.action_about:
                final Intent intent_about = new Intent(this, AboutActivity.class);
                startActivity(intent_about);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
                .getMenuInfo();
        final View rowView = info.targetView;
        final LaunchableActivity launchableActivity = trie.get(((TextView) rowView
                .findViewById(R.id.appLabel)).getText().toString()
                .toLowerCase());
        switch (item.getItemId()) {
            case R.id.appmenu_launch:
                launchActivity(launchableActivity);
                return true;

            case R.id.appmenu_favorite:

                final int prevIndex = Collections.binarySearch(activityInfos,
                        launchableActivity);
                activityInfos.remove(prevIndex);
                launchableActivity.setFavorite(!launchableActivity.isFavorite());
                final int newIndex = -(Collections.binarySearch(activityInfos,
                        launchableActivity) + 1);
                activityInfos.add(newIndex, launchableActivity);
                launchableActivityPrefs.writePreference(launchableActivity.getClassName(),
                        launchableActivity.getNumberOfLaunches(),
                        launchableActivity.isFavorite());
                arrayAdapter.notifyDataSetChanged();
                break;
            case R.id.appmenu_info:
                final Intent intent = new Intent(
                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:"
                        + launchableActivity.getComponent().getPackageName()));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                return true;
            // default:
            // return super.onContextItemSelected(item);
        }

        return false;
    }

    private void refreshAppList() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            recreate();
        } else {
            Intent intentRefresh = new Intent(this, SearchActivity.class);
            finish();
            startActivity(intentRefresh);
        }
    }

    public void onClickSettingsButton(View view) {
        if (!showPopup(overflowButton)) {
            openOptionsMenu();
        }

    }

    public void launchActivity(final LaunchableActivity launchableActivity) {

        final ComponentName componentName = launchableActivity.getComponent();
        final Intent launchIntent = new Intent(Intent.ACTION_MAIN);
        launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        launchIntent.setComponent(componentName);

        final int prevIndex = Collections.binarySearch(activityInfos,
                launchableActivity);
        activityInfos.remove(prevIndex);
        launchableActivity.incrementLaunches();
        final int newIndex = -(Collections.binarySearch(activityInfos,
                launchableActivity) + 1);
        activityInfos.add(newIndex, launchableActivity);
        launchableActivityPrefs.writePreference(componentName.getClassName(),
                launchableActivity.getNumberOfLaunches(),
                launchableActivity.isFavorite());

        searchEditText.clearFocus();
        hideKeyboard();

        try {
            startActivity(launchIntent);
            arrayAdapter.notifyDataSetChanged();
        } catch (ActivityNotFoundException e) {
            launchableActivityPrefs.deletePreference(componentName.getClassName());
            refreshAppList();
        }


    }

    class ActivityInfoArrayAdapter extends ArrayAdapter<LaunchableActivity> {
        final LayoutInflater inflater;
        final PackageManager pm;

        public ActivityInfoArrayAdapter(final Context context, final int resource,
                                        final List<LaunchableActivity> activityInfos) {
            super(context, resource, activityInfos);
            inflater = getLayoutInflater();
            pm = getPackageManager();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            final View view =
                    convertView != null ?
                            convertView : inflater.inflate(R.layout.app_grid_item, null);
            final LaunchableActivity launchableActivity = getItem(position);
            final CharSequence label = launchableActivity.getActivityLabel();
            final TextView appLabelView = (TextView) view.findViewById(R.id.appLabel);
            final ImageView appIconView = (ImageView) view.findViewById(R.id.appIcon);
            final View appFavoriteView = view.findViewById(R.id.appFavorite);

            appLabelView.setText(label);


            if (sharedPreferences.getBoolean("pref_show_icon", true)) {

                appIconView.setTag(launchableActivity.getComponent());

                if (!launchableActivity.isIconLoaded()) {
                    appIconView.setImageDrawable(defaultAppIcon);
                    imageLoadingConsumersManager.addTask(
                            new ImageLoadingTask(appIconView, launchableActivity,
                                    imageTasksSharedData));

                } else {
                    appIconView.setImageDrawable(
                            launchableActivity.getActivityIcon(pm, context, iconSizePixels));
                }
            } else {
                appIconView.setImageDrawable(defaultAppIcon);
            }
            appFavoriteView.setVisibility(
                    launchableActivity.isFavorite() ? View.VISIBLE : View.INVISIBLE);
            return view;
        }

    }

    final TextWatcher textWatcher = new TextWatcher() {

        @Override
        public void onTextChanged(CharSequence s, int start, int before,
                                  int count) {
            HashSet<LaunchableActivity> infoList = trie.getAllStartingWith(s
                    .toString().toLowerCase().trim());
            activityInfos.clear();
            activityInfos.addAll(infoList);
            Collections.sort(activityInfos);
            arrayAdapter.notifyDataSetChanged();
        }


        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                                      int after) {
            // TODO Auto-generated method stub

        }

        @Override
        public void afterTextChanged(Editable s) {
            // TODO Auto-generated method stub

        }
    };

}
