package com.seizonsenryaku.hayailauncher;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class Main extends AppWidgetProvider {

	@Override
	public void onEnabled(Context context) {
		// TODO Auto-generated method stub
		super.onEnabled(context);
		
		
	}
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		Intent intent = new Intent(context, SearchActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
		RemoteViews views= new RemoteViews(context.getPackageName(), R.layout.main);
		views.setOnClickPendingIntent(R.id.fakeSearchTextView, pendingIntent);
		appWidgetManager.updateAppWidget(appWidgetIds, views);
	}
}
