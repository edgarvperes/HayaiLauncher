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

    // private static final int ACTIVITY_LIST_RESULT = 1;

    private ArrayList<LaunchableActivity> activityInfos;
    private Trie<LaunchableActivity> trie;
    private ArrayAdapter<LaunchableActivity> arrayAdapter;
    private LaunchableActivityPrefs launchableActivityPrefs;
    private SharedPreferences sharedPreferences;
    private Context context;
    private Drawable defaultAppIcon;
    private SimpleTaskConsumerManager imageLoadingConsumersManager;
    private Object uiMutex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);


        sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(this);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        setContentView(R.layout.activity_search);

        launchableActivityPrefs = new LaunchableActivityPrefs(this);
        final EditText editText = (EditText) findViewById(R.id.editText1);
        editText.requestFocus();
        editText.addTextChangedListener(textWatcher);

        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        final PackageManager pm = getPackageManager();
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
                        trie.put(wordSinceLastSpaceBuilder.toString().toLowerCase(), launchableActivity);
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
        AdapterView appListView = (AdapterView) findViewById(R.id.appsContainer);

        registerForContextMenu(appListView);

        arrayAdapter = new ActivityInfoArrayAdapter(this,
                R.layout.app_grid_item, activityInfos);

        ((GridView) appListView).setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState != SCROLL_STATE_IDLE) {
                    hideKeyboard();
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

            }
        });
        appListView.setAdapter(arrayAdapter);


        appListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                launchActivity(activityInfos.get(position));
            }

        });

        if (sharedPreferences.getBoolean(
                SettingsActivity.KEY_PREF_NOTIFICATION, false)) {
            MyNotificationManager myNotificationManager = new MyNotificationManager();
            myNotificationManager.showNotification(this);
        }

        context = getApplicationContext();

        final Resources resources = getResources();
        StatusBarColorHelper.setStatusBarColor(resources, this, resources.getColor(R.color.indigo_700));

        defaultAppIcon = resources.getDrawable(R.drawable.ic_launcher);

        uiMutex=new Object();
        int numThreads = Runtime.getRuntime().availableProcessors() - 1;
        if(numThreads<1) numThreads=1;
        else if(numThreads>7) numThreads=7;
        imageLoadingConsumersManager=new SimpleTaskConsumerManager(numThreads);



    }

    private void hideKeyboard() {
        final EditText searchEditText = (EditText) findViewById(R.id.editText1);
        ((InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE)).
                hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
    }

    @Override
    protected void onDestroy() {
        imageLoadingConsumersManager.destroyAllConsumers(false);
        super.onDestroy();
    }

    public boolean showPopup(View v) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            PopupMenu popup = new PopupMenu(this, v);
            popup.setOnMenuItemClickListener(new PopupEventListener());
            MenuInflater inflater = popup.getMenuInflater();
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
            if (!showPopup(findViewById(R.id.overflow_button))) {
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
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_refresh_app_list:
                refreshAppList();
                return true;
            case R.id.action_about:
                Intent intent_about = new Intent(this, AboutActivity.class);
                startActivity(intent_about);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
                .getMenuInfo();
        View rowView = info.targetView;
        LaunchableActivity launchableActivity = trie.get(((TextView) rowView
                .findViewById(R.id.appLabel)).getText().toString()
                .toLowerCase());
        switch (item.getItemId()) {
            case R.id.appmenu_launch:
                launchActivity(launchableActivity);
                return true;

            case R.id.appmenu_favorite:

                int prevIndex = Collections.binarySearch(activityInfos,
                        launchableActivity);
                activityInfos.remove(prevIndex);
                launchableActivity.setFavorite(!launchableActivity.isFavorite());
                int newIndex = -(Collections.binarySearch(activityInfos,
                        launchableActivity) + 1);
                activityInfos.add(newIndex, launchableActivity);
                launchableActivityPrefs.writePreference(launchableActivity.getClassName(),
                        launchableActivity.getNumberOfLaunches(),
                        launchableActivity.isFavorite());
                arrayAdapter.notifyDataSetChanged();
                break;
            case R.id.appmenu_info:
                Intent intent = new Intent(
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
        if (!showPopup(findViewById(R.id.overflow_button))) {
            openOptionsMenu();
        }

    }

    public void launchActivity(LaunchableActivity launchableActivity) {

        ComponentName componentName = launchableActivity.getComponent();
        Intent launchIntent = new Intent(Intent.ACTION_MAIN);
        launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        launchIntent.setComponent(componentName);

        int prevIndex = Collections.binarySearch(activityInfos,
                launchableActivity);
        activityInfos.remove(prevIndex);
        launchableActivity.incrementLaunches();
        int newIndex = -(Collections.binarySearch(activityInfos,
                launchableActivity) + 1);
        activityInfos.add(newIndex, launchableActivity);
        launchableActivityPrefs.writePreference(componentName.getClassName(),
                launchableActivity.getNumberOfLaunches(),
                launchableActivity.isFavorite());
        final EditText searchEditText = (EditText) findViewById(R.id.editText1);

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
        LayoutInflater inflater;
        PackageManager pm;

        public ActivityInfoArrayAdapter(Context context, int resource,
                                        List<LaunchableActivity> activityInfos) {
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


            ((TextView) view
                    .findViewById(R.id.appLabel))
                    .setText(label);

            final ImageView imageView = (ImageView) view.findViewById(R.id.appIcon);
            if (sharedPreferences.getBoolean("pref_show_icon", true)) {

                synchronized (uiMutex) {
                    imageView.setTag(launchableActivity.getClassName());

                    if (!launchableActivity.isIconLoaded()) {
                        imageView.setImageDrawable(defaultAppIcon);
                        imageLoadingConsumersManager.addTask(
                                new ImageLoadingTask(imageView, launchableActivity,
                                        uiMutex,
                                        SearchActivity.this, pm, context));

                    } else {
                        imageView.setImageDrawable(launchableActivity.getActivityIcon(pm, context));
                    }
                }
            } else {
                imageView.setImageDrawable(launchableActivity.getActivityIcon(pm, context));
            }
            view.findViewById(R.id.appFavorite)
                    .setVisibility(launchableActivity.isFavorite() ? View.VISIBLE : View.INVISIBLE);
            return view;
        }

    }

    TextWatcher textWatcher = new TextWatcher() {

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
