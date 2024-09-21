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
import android.os.Build;
import android.sysprop.DisplayProperties;

import com.android.car.qc.QCActionItem;
import com.android.car.qc.QCItem;
import com.android.car.qc.QCList;
import com.android.car.qc.QCRow;
import com.android.car.settings.R;
import com.android.settingslib.development.DevelopmentSettingsEnabler;
import com.android.settingslib.development.SystemPropPoker;

/**
 * Quick control for showing a toggle for the layout bounds.
 */
public class DebugLayoutBoundsRow extends SettingsQCItem {
    public DebugLayoutBoundsRow(Context context) {
        super(context);
    }

    @Override
    protected QCItem getQCItem() {
        if (!(Build.IS_USERDEBUG || Build.IS_ENG)
                || !DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(getContext())) {
            return null;
        }
        QCActionItem actionItem = new QCActionItem.Builder(QC_TYPE_ACTION_SWITCH)
                .setChecked(DisplayProperties.debug_layout().orElse(false))
                .setAction(getBroadcastIntent())
                .build();

        QCList.Builder listBuilder = new QCList.Builder()
                .addRow(new QCRow.Builder()
                        .setTitle(getContext().getString(R.string.show_layout_bounds_title))
                        .addEndItem(actionItem)
                        .build()
                );
        return listBuilder.build();
    }

    @Override
    void onNotifyChange(Intent intent) {
        boolean newState = intent.getBooleanExtra(QC_ACTION_TOGGLE_STATE, /* defaultValue */ false);
        DisplayProperties.debug_layout(newState);
        SystemPropPoker.getInstance().poke();
    }



    @Override
    protected Uri getUri() {
        return SettingsQCRegistry.DEBUG_LAYOUT_BOUNDS_URI;
    }

    @Override
    Class getBackgroundWorkerClass() {
        return DebugLayoutBoundsRowWorker.class;
    }
}
