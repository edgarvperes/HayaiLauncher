package com.seizonsenryaku.testwidget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class SearchActivity extends Activity {

	// private static final int ACTIVITY_LIST_RESULT = 1;

	private List<LaunchableActivity> activityInfos;
	private Trie<LaunchableActivity> trie;
	private ArrayAdapter<LaunchableActivity> arrayAdapter;
	private LaunchableActivityPrefs launchableActivityPrefs;
    private SharedPreferences sharedPreferences;
	public OnLongClickListener onLongClickAppRow = new OnLongClickListener() {

		@Override
		public boolean onLongClick(View v) {
			openContextMenu(v);
			return true;
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
        sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(this);
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		setContentView(R.layout.activity_search);

		launchableActivityPrefs = new LaunchableActivityPrefs(this);
		EditText editText = (EditText) findViewById(R.id.editText1);
		editText.requestFocus();
		editText.addTextChangedListener(textWatcher);
		getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
		PackageManager pm = getPackageManager();
		Intent intent = new Intent();
		intent.setAction(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		List<ResolveInfo> infoList = pm.queryIntentActivities(intent, 0);
		trie = new Trie<LaunchableActivity>();
		activityInfos = new ArrayList<LaunchableActivity>();
		for (ResolveInfo info : infoList) {
			CharSequence activityLabel = info.activityInfo.loadLabel(pm);
			LaunchableActivity launchableActivity = new LaunchableActivity(
					info.activityInfo, activityLabel.toString());
			launchableActivityPrefs.setPreferences(launchableActivity);
            trie.put(activityLabel.toString().toLowerCase(Locale.US),
                        launchableActivity);

		}
		activityInfos.addAll(trie.getAllStartingWith(""));

		Collections.sort(activityInfos);
		ListView appListView = (ListView) findViewById(R.id.listView1);
		registerForContextMenu(appListView);

		arrayAdapter = new ActivityInfoArrayAdapter(this,
				R.layout.app_list_item, activityInfos);
		appListView.setAdapter(arrayAdapter);
		appListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int arg2,
					long arg3) {
				onClickAppRow(view);

			}

		});

		if (sharedPreferences.getBoolean(
				SettingsActivity.KEY_PREF_NOTIFICATION, false)) {
			MyNotificationManager myNotificationManager = new MyNotificationManager();
			myNotificationManager.showNotification(this);
		}

	}

	public void showPopup(View v) {
		// PopupMenu popup = new PopupMenu(this, v);
		// MenuInflater inflater = popup.getMenuInflater();
		// popup.setOnMenuItemClickListener(this);
		// inflater.inflate(R.menu.overflow, popup.getMenu());

		// popup.show();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		// getMenuInflater().inflate(R.menu.search, menu);
		return false;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.app, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		View rowView = info.targetView;
		LaunchableActivity launchableActivity = trie.get(((TextView) rowView
				.findViewById(R.id.appListTopText)).getText().toString()
				.toLowerCase(Locale.US));
		switch (item.getItemId()) {
		case R.id.appmenu_launch:
			onClickAppRow(rowView);
			return true;
			// case R.id.delete:
			// deleteNote(info.id);
			// return true;
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

	public void onClickSettingsButton(View view) {
		Intent intent = new Intent(this, SettingsActivity.class);
		startActivity(intent);
	}

	public void onClickAppRow(View view) {
		TextView textView = ((TextView) view
				.findViewById(com.seizonsenryaku.testwidget.R.id.appListTopText));

		LaunchableActivity launchableActivity = trie.get(textView.getText()
				.toString().toLowerCase(Locale.US));

		ComponentName componentName = launchableActivity.getComponent();
		// .getActivityInfo();
		Intent launchIntent = new Intent(Intent.ACTION_MAIN);
		launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		launchIntent.setComponent(componentName);
		// getWindow().setSoftInputMode(
		// WindowManager.LayoutParams.SOFT_INPUT_STATE_);
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
        try {
            startActivity(launchIntent);
            arrayAdapter.notifyDataSetChanged();
        }catch(ActivityNotFoundException e){
            if (android.os.Build.VERSION.SDK_INT >=  Build.VERSION_CODES.HONEYCOMB)
            {
                launchableActivityPrefs.deletePreference(componentName.getClassName());
                super.recreate();
            }
            else
            {
                startActivity(getIntent());
                finish();
            }
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

			View view;
			if (convertView != null) {
				view = convertView;
			} else {
				view = inflater.inflate(R.layout.app_list_item, null);
				// view.setOnLongClickListener(onLongClickAppRow);
			}
			LaunchableActivity launchableActivity = getItem(position);
			// ActivityInfo activityInfo =
			// launchableActivity.getActivityInfo();
            if(sharedPreferences.getBoolean("pref_show_icon",true)) {
                Drawable icon = launchableActivity.getActivityIcon(pm);
                ((ImageView) view
                        .findViewById(com.seizonsenryaku.testwidget.R.id.imageView1))
                        .setImageDrawable(icon);
            } else {
                ((ImageView) view
                        .findViewById(com.seizonsenryaku.testwidget.R.id.imageView1))
                        .setImageDrawable(null);
            }
			CharSequence label = launchableActivity.getActivityLabel();
			// CharSequence label = activityInfo.processName;
			String bottomText;
			int numberOfLaunches = launchableActivity.getNumberOfLaunches();
			switch (numberOfLaunches) {
			case 0:
				bottomText = getString(R.string.app_item_bottom_never);
				break;
			case 1:
				bottomText = getString(R.string.app_item_bottom_once,
						numberOfLaunches);
				break;
			default:
				bottomText = getString(R.string.app_item_bottom_several,
						numberOfLaunches);
				break;
			}

			((TextView) view
					.findViewById(com.seizonsenryaku.testwidget.R.id.appListTopText))
					.setText(label);

			((TextView) view
					.findViewById(com.seizonsenryaku.testwidget.R.id.appListBottomText))
					.setText(bottomText);


			
			((TextView) view
					.findViewById(com.seizonsenryaku.testwidget.R.id.appListFavoriteText))
					.setVisibility(launchableActivity.isFavorite()?View.VISIBLE:View.INVISIBLE);
			return view;
		}

	};

	TextWatcher textWatcher = new TextWatcher() {

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
			List<LaunchableActivity> infoList = trie.getAllStartingWith(s
					.toString().toLowerCase(Locale.US));
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
