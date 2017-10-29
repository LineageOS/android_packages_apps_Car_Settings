package com.android.car.settings.home;

import static com.android.settingslib.drawer.TileUtils.EXTRA_SETTINGS_ACTION;
import static com.android.settingslib.drawer.TileUtils.META_DATA_PREFERENCE_ICON;
import static com.android.settingslib.drawer.TileUtils.META_DATA_PREFERENCE_TITLE;
import static com.android.settingslib.drawer.TileUtils.META_DATA_PREFERENCE_SUMMARY;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.android.car.list.LaunchAppLineItem;
import com.android.car.list.TypedPagedListAdapter;
import com.android.car.settings.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Loads Activity with TileUtils.EXTRA_SETTINGS_ACTION.
 */
public class ExtraSettingsLoader {
    private static final String TAG = "ExtraSettingsLoader";
    private final Context mContext;

    public ExtraSettingsLoader(Context context) {
        mContext = context;
    }

    public List<TypedPagedListAdapter.LineItem> load() {
        PackageManager pm = mContext.getPackageManager();
        Intent intent = new Intent(EXTRA_SETTINGS_ACTION);

        List<ResolveInfo> results = pm.queryIntentActivitiesAsUser(intent,
                PackageManager.GET_META_DATA, ActivityManager.getCurrentUser());
        List<TypedPagedListAdapter.LineItem> extraSettings = new ArrayList<>();
        for (ResolveInfo resolved : results) {
            if (!resolved.system) {
                // Do not allow any app to add to settings, only system ones.
                continue;
            }
            String title = null;
            String summary = null;
            ActivityInfo activityInfo = resolved.activityInfo;
            Bundle metaData = activityInfo.metaData;
            try {
                Resources res = pm.getResourcesForApplication(activityInfo.packageName);
                if (metaData.containsKey(META_DATA_PREFERENCE_TITLE)) {
                    if (metaData.get(META_DATA_PREFERENCE_TITLE) instanceof Integer) {
                        title = res.getString(metaData.getInt(META_DATA_PREFERENCE_TITLE));
                    } else {
                        title = metaData.getString(META_DATA_PREFERENCE_TITLE);
                    }
                }
                if (metaData.containsKey(META_DATA_PREFERENCE_SUMMARY)) {
                    if (metaData.get(META_DATA_PREFERENCE_SUMMARY) instanceof Integer) {
                        summary = res.getString(metaData.getInt(META_DATA_PREFERENCE_SUMMARY));
                    } else {
                        summary = metaData.getString(META_DATA_PREFERENCE_SUMMARY);
                    }
                } else {
                    Log.d(TAG, "no description.");
                }
            } catch (PackageManager.NameNotFoundException | Resources.NotFoundException e) {
                Log.d(TAG, "Couldn't find info", e);
            }
            if (TextUtils.isEmpty(title)) {
                Log.d(TAG, "no title.");
                title = activityInfo.loadLabel(pm).toString();
            }
            Integer iconRes = null;
            Icon icon = null;
            if (metaData.containsKey(META_DATA_PREFERENCE_ICON)) {
                iconRes = metaData.getInt(META_DATA_PREFERENCE_ICON);
                icon = Icon.createWithResource(activityInfo.packageName, iconRes);
            } else {
                icon = Icon.createWithResource(mContext, R.drawable.ic_settings_gear);
                Log.d(TAG, "no icon.");
            }
            Intent extraSettingIntent =
                    new Intent().setClassName(activityInfo.packageName, activityInfo.name);
            extraSettings.add(new LaunchAppLineItem(
                    title, icon, mContext, summary, extraSettingIntent));
        }
        return extraSettings;
    }
}
