/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.car.settings.qc;

import static com.android.car.qc.QCItem.QC_ACTION_TOGGLE_STATE;
import static com.android.car.qc.QCItem.QC_TYPE_ACTION_SWITCH;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

import com.android.car.qc.QCActionItem;
import com.android.car.qc.QCItem;
import com.android.car.qc.QCList;
import com.android.car.qc.QCRow;
import com.android.car.settings.R;
import com.android.car.settings.common.BuildInfoUtil;
import com.android.settingslib.development.DevelopmentSettingsEnabler;

/**
 * Quick control for showing a toggle for enabling the Customization tool overlay.
 */
public class DebugCustomizationOverlayRow extends SettingsQCItem {

    private static final String SERVICE_ENABLED_STATE = "1";
    private static final String SERVICE_DISABLED_STATE = "0";
    private static final String PACKAGE = "com.android.car.customization.tool";
    private static final String SERVICE = PACKAGE + '/' + PACKAGE + ".CustomizationToolService";
    private final ContentResolver mContentResolver;

    public DebugCustomizationOverlayRow(Context context) {
        super(context);
        mContentResolver = context.getContentResolver();
    }

    @Override
    protected QCItem getQCItem() {
        if (!BuildInfoUtil.isDevTesting(getContext())
                || !DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(getContext())) {
            return null;
        }

        QCActionItem actionItem = new QCActionItem.Builder(QC_TYPE_ACTION_SWITCH)
                .setChecked(isCustomizationToolActive())
                .setAction(getBroadcastIntent())
                .build();

        QCList.Builder listBuilder = new QCList.Builder()
                .addRow(new QCRow.Builder()
                        .setTitle(getContext().getString(R.string.show_customization_overlay_title))
                        .addEndItem(actionItem)
                        .build()
                );
        return listBuilder.build();
    }

    @Override
    void onNotifyChange(Intent intent) {
        boolean newState =
                intent.getBooleanExtra(QC_ACTION_TOGGLE_STATE, /* defaultValue= */ false);
        toggleCustomizationTool(newState);
    }

    private boolean isCustomizationToolActive() {
        String accessibilityServices = Settings.Secure.getString(
                mContentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (accessibilityServices == null || accessibilityServices.isEmpty()) {
            return false;
        }
        String serviceStatus = Settings.Secure.getString(
                mContentResolver, Settings.Secure.ACCESSIBILITY_ENABLED);
        return accessibilityServices.contains(SERVICE)
                && SERVICE_ENABLED_STATE.equals(serviceStatus);
    }

    private void toggleCustomizationTool(boolean newState) {
        String newAccessibilityServices;
        String newServiceState;
        String currentServices = Settings.Secure.getString(
                mContentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);

        if (newState) {
            newAccessibilityServices = getCurrentListPlusService(currentServices);
            newServiceState = SERVICE_ENABLED_STATE;
        } else {
            newAccessibilityServices = getCurrentListMinusService(currentServices);
            newServiceState = SERVICE_DISABLED_STATE;
        }

        Settings.Secure.putString(
                mContentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                newAccessibilityServices);
        Settings.Secure.putString(mContentResolver, Settings.Secure.ACCESSIBILITY_ENABLED,
                newServiceState);
    }

    private String getCurrentListPlusService(String currentServices) {
        if (currentServices == null || currentServices.isEmpty()) {
            return SERVICE;
        }
        return currentServices + ":" + SERVICE;
    }

    private String getCurrentListMinusService(String currentServices) {
        if (currentServices == null || currentServices.isEmpty()) {
            return "";
        }
        String newServiceList = currentServices.replace(
                SERVICE, /* replacement= */"").replace(/* target= */"::", /* replacement= */":");
        if (newServiceList.indexOf(':') == 0) {
            newServiceList = newServiceList.substring(/* beginIndex= */ 1);
        }
        if (newServiceList.lastIndexOf(':') == newServiceList.length() - 1) {
            newServiceList = newServiceList.substring(/* beginIndex= */ 0,
                    /* endIndex= */ newServiceList.length() - 1);
        }

        return newServiceList;
    }

    @Override
    protected Uri getUri() {
        return SettingsQCRegistry.DEBUG_CUSTOMIZATION_OVERLAY_URI;
    }

    @Override
    Class getBackgroundWorkerClass() {
        return DefaultQCBackgroundWorker.class;
    }
}
