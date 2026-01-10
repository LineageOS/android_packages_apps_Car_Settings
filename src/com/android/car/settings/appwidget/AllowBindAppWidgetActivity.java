/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.car.settings.appwidget;

import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.UserHandle;

import androidx.activity.ComponentActivity;

import com.android.car.settings.R;
import com.android.car.settings.common.Logger;
import com.android.car.ui.AlertDialogBuilder;
import com.android.internal.annotations.VisibleForTesting;

/**
 * This activity is displayed when an app launches the BIND_APPWIDGET intent. This allows apps
 * that don't have the BIND_APPWIDGET permission to bind specific widgets.
 */
public class AllowBindAppWidgetActivity extends ComponentActivity implements
        DialogInterface.OnClickListener {
    private static final Logger LOG = new Logger(AllowBindAppWidgetActivity.class);
    private static final String TAG = "AllowBindAppWidgetActivity";
    @VisibleForTesting
    AlertDialog mDialog;
    private boolean mAlwaysUse;
    private int mAppWidgetId;
    private Bundle mBindOptions;
    private UserHandle mProfile;
    private ComponentName mComponentName;
    private String mCallingPackage;
    private AppWidgetManager mAppWidgetManager;
    // Indicates whether this activity was closed because of a click
    private boolean mClicked;

    @Override
    public void onClick(DialogInterface dialog, int which) {

        mClicked = true;
        if (which == AlertDialog.BUTTON_POSITIVE && mAppWidgetId != -1 && mComponentName != null
                && mCallingPackage != null) {
            try {
                boolean bound = mAppWidgetManager.bindAppWidgetIdIfAllowed(mAppWidgetId, mProfile,
                        mComponentName, mBindOptions);
                if (bound) {
                    Intent result = new Intent();
                    result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                    setResult(RESULT_OK, result);
                }
            } catch (Exception e) {
                LOG.v("Error binding widget with id " + mAppWidgetId + " and component "
                        + mComponentName);
            }

            if (mAlwaysUse != mAppWidgetManager.hasBindAppWidgetPermission(mCallingPackage,
                    mProfile.getIdentifier())) {
                mAppWidgetManager.setBindAppWidgetPermission(mCallingPackage, mAlwaysUse);
            }
        }
        finish();
    }

    @Override
    protected void onPause() {
        if (!mClicked) { // RESULT_CANCELED
            finish();
        }
        super.onPause();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setResult(RESULT_CANCELED); // By default, set the result to cancelled
        Intent intent = getIntent();
        if (intent == null) {
            finish();
            return;
        }

        CharSequence label = "";
        try {
            mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
            mProfile = intent.getParcelableExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE);
            if (mProfile == null) {
                mProfile = android.os.Process.myUserHandle();
            }
            mComponentName = intent.getParcelableExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER);
            mBindOptions = intent.getParcelableExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS);
            mCallingPackage = getCallingPackage();
            PackageManager pm = getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(mCallingPackage, 0);
            label = pm.getApplicationLabel(ai);
        } catch (Exception e) {
            mAppWidgetId = -1;
            mComponentName = null;
            mCallingPackage = null;
        }
        if (mCallingPackage == null || mComponentName == null || mAppWidgetId == -1) {
            LOG.v("Error getting parameters");
            finish();
            return;
        }
        mAppWidgetManager = AppWidgetManager.getInstance(this);
        String widgetLabel = getWidgetLabel();

        AlertDialogBuilder builder = new AlertDialogBuilder(this);
        builder.setTitle(getString(R.string.allow_bind_app_widget_activity_allow_bind_title));
        builder.setMessage(
                getString(R.string.allow_bind_app_widget_activity_allow_bind, widgetLabel, label));
        builder.setPositiveButton(getString(R.string.dialog_create_button_text), this);
        builder.setNegativeButton(getString(android.R.string.cancel), this);

        mAlwaysUse = mAppWidgetManager.hasBindAppWidgetPermission(mCallingPackage,
                mProfile.getIdentifier());
        builder.setMultiChoiceItems(new CharSequence[]{
                        getString(R.string.allow_bind_app_widget_activity_always_allow_bind,
                                label)},
                new boolean[]{mAlwaysUse}, (dialog, which, checkStatus) -> {
                    mAlwaysUse = checkStatus;
                });

        mDialog = builder.create();
        mDialog.show();
    }

    private String getWidgetLabel() {
        String label = "";
        for (AppWidgetProviderInfo providerInfo : mAppWidgetManager.getInstalledProviders()) {
            if (providerInfo.provider.equals(mComponentName)) {
                label = providerInfo.loadLabel(getPackageManager());
                break;
            }
        }
        return label;
    }
}
