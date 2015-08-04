package com.seizonsenryaku.hayailauncher;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

public class About extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        Resources resources=getResources();
        setStatusBarColor(resources);
    }

    private void setStatusBarColor(Resources resources) {

        //There's no support for colored status bar in versions below KITKAT
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) return;

        Window window = getWindow();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            //LOLLIPOP+ path
            window.setStatusBarColor(resources.getColor(R.color.indigo_700));
        } else {
            //KITKAT path
            window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

            int statusBarHeight = getStatusBarHeight();
            View statusBarDummy = findViewById(R.id.statusBarDummyView);
            statusBarDummy.getLayoutParams().height=statusBarHeight;
            statusBarDummy.setBackgroundColor(resources.getColor(R.color.indigo_700));
        }
    }

    private int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
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
