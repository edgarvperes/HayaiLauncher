package com.seizonsenryaku.hayailauncher.activities;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.seizonsenryaku.hayailauncher.R;
import com.seizonsenryaku.hayailauncher.StatusBarColorHelper;

public class AboutActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        Resources resources=getResources();
        StatusBarColorHelper.setStatusBarColor(resources, this, resources.getColor(R.color.indigo_700));
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }
}
