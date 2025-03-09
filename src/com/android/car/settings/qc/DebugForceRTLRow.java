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

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.sysprop.DisplayProperties;

import com.android.car.qc.QCActionItem;
import com.android.car.qc.QCItem;
import com.android.car.qc.QCList;
import com.android.car.qc.QCRow;
import com.android.car.settings.R;
import com.android.car.settings.common.BuildInfoUtil;
import com.android.internal.app.LocalePicker;
import com.android.settingslib.development.DevelopmentSettingsEnabler;

/**
 * Quick control for showing a toggle for force-showing RTL layout direction.
 */
public class DebugForceRTLRow extends SettingsQCItem {
    private static final int SETTING_VALUE_ON = 1;
    private static final int SETTING_VALUE_OFF = 0;

    public DebugForceRTLRow(Context context) {
        super(context);
    }

    @Override
    protected QCItem getQCItem() {
        if (!BuildInfoUtil.isDevTesting(getContext())
                || !DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(getContext())) {
            return null;
        }
        int rtlLayoutMode = Settings.Global.getInt(getContext().getContentResolver(),
                Settings.Global.DEVELOPMENT_FORCE_RTL, SETTING_VALUE_OFF);
        QCActionItem actionItem = new QCActionItem.Builder(QC_TYPE_ACTION_SWITCH)
                .setChecked(rtlLayoutMode != SETTING_VALUE_OFF)
                .setAction(getBroadcastIntent())
                .build();

        QCList.Builder listBuilder = new QCList.Builder()
                .addRow(new QCRow.Builder()
                        .setTitle(getContext().getString(R.string.show_force_rtl_title))
                        .addEndItem(actionItem)
                        .build()
                );
        return listBuilder.build();
    }

    @Override
    void onNotifyChange(Intent intent) {
        boolean newState =
                intent.getBooleanExtra(QC_ACTION_TOGGLE_STATE, /* defaultValue = */ false);
        writeToForceRtlLayoutSetting(newState);
    }

    private void writeToForceRtlLayoutSetting(boolean isEnabled) {
        Settings.Global.putInt(getContext().getContentResolver(),
                Settings.Global.DEVELOPMENT_FORCE_RTL,
                isEnabled ? SETTING_VALUE_ON : SETTING_VALUE_OFF);
        DisplayProperties.debug_force_rtl(isEnabled);

        LocalePicker.updateLocales(getContext().getResources().getConfiguration().getLocales());
    }

    @Override
    protected Uri getUri() {
        return SettingsQCRegistry.DEBUG_FORCE_RTL_URI;
    }

    @Override
    Class getBackgroundWorkerClass() {
        return DefaultQCBackgroundWorker.class;
    }
}
