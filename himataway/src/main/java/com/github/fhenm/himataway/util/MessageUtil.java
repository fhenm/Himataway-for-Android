package com.github.fhenm.himataway.util;

import android.app.ProgressDialog;
import android.content.Context;
import android.widget.Toast;

import com.github.fhenm.himataway.himatawayApplication;

public class MessageUtil {
    private static ProgressDialog sProgressDialog;

    public static void showToast(String text) {
        himatawayApplication application = himatawayApplication.getApplication();
        Toast.makeText(application, text, Toast.LENGTH_SHORT).show();
    }

    public static void showToast(int id) {
        himatawayApplication application = himatawayApplication.getApplication();
        String text = application.getString(id);
        Toast.makeText(application, text, Toast.LENGTH_SHORT).show();
    }

    public static void showToast(int id, String description) {
        himatawayApplication application = himatawayApplication.getApplication();
        String text = application.getString(id) + "\n" + description;
        Toast.makeText(application, text, Toast.LENGTH_SHORT).show();
    }

    public static void showProgressDialog(Context context, String message) {
        sProgressDialog = new ProgressDialog(context);
        sProgressDialog.setMessage(message);
        sProgressDialog.show();
    }

    public static void dismissProgressDialog() {
        if (sProgressDialog != null)
            try {
                sProgressDialog.dismiss();
            } finally {
                sProgressDialog = null;
            }
    }
}
