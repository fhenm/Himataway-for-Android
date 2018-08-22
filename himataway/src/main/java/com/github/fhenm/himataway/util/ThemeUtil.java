package com.github.fhenm.himataway.util;

import android.app.Activity;
import android.content.res.Resources;
import android.util.TypedValue;
import android.widget.TextView;

import com.github.fhenm.himataway.R;
import com.github.fhenm.himataway.settings.BasicSettings;

public class ThemeUtil {
    private static Resources.Theme sTheme;

    public static void setTheme(Activity activity) {
        if (BasicSettings.getThemeName().equals("black")) {
            activity.setTheme(R.style.BlackTheme);
        } else {
            activity.setTheme(R.style.WhiteTheme);
        }
        sTheme = activity.getTheme();
    }

    public static void setThemeTextColor(TextView view, int resourceId) {
        TypedValue outValue = new TypedValue();
        if (sTheme != null) {
            sTheme.resolveAttribute(resourceId, outValue, true);
            view.setTextColor(outValue.data);
        }
    }

    public static int getThemeTextColor(int resourceId) {
        TypedValue outValue = new TypedValue();
        if (sTheme != null) {
            sTheme.resolveAttribute(resourceId, outValue, true);
        }
        return outValue.data;
    }
}
