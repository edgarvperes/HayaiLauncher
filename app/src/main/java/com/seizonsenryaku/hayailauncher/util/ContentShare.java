package com.seizonsenryaku.hayailauncher.util;

import android.content.Context;
import android.content.Intent;

/**
 * Created by Administrador on 18/08/2015.
 */
public class ContentShare {


    public static void shareText(Context context, String text){

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, text);
        sendIntent.setType("text/plain");
        context.startActivity(sendIntent);
    }
}
