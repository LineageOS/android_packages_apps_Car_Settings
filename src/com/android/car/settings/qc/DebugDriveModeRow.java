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

import android.annotation.SuppressLint;
import android.car.Car;
import android.car.drivingstate.CarDrivingStateEvent;
import android.car.drivingstate.CarDrivingStateManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.android.car.qc.QCActionItem;
import com.android.car.qc.QCItem;
import com.android.car.qc.QCList;
import com.android.car.qc.QCRow;
import com.android.car.settings.R;
import com.android.car.settings.common.BuildInfoUtil;
import com.android.car.settings.common.CarUxRestrictionsHelper;
import com.android.settingslib.development.DevelopmentSettingsEnabler;

/**
 * A {@link SettingsQCItem} that allows the user to toggle the driving mode on/off.
 */
public class DebugDriveModeRow extends SettingsQCItem {
    private CarDrivingStateManager mDrivingStateManager;
    private CarUxRestrictionsHelper mHelper;

    public DebugDriveModeRow(Context context) {
        super(context);
        Car car = Car.createCar(getContext());
        if (car != null) {
            mDrivingStateManager =
                    (CarDrivingStateManager) car.getCarManager(Car.CAR_DRIVING_STATE_SERVICE);
        }
        mHelper = new CarUxRestrictionsHelper(context, restrictionInfo -> {/* Do nothing. */});
    }

    @Override
    protected QCItem getQCItem() {
        if (!BuildInfoUtil.isDevTesting(getContext())
                || !DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(getContext())
                || mDrivingStateManager == null) {
            return null;
        }

        QCActionItem actionItem = new QCActionItem.Builder(QC_TYPE_ACTION_SWITCH)
                .setChecked(isDrivingModeOn())
                .setAction(getBroadcastIntent())
                .build();

        QCList.Builder listBuilder = new QCList.Builder()
                .addRow(new QCRow.Builder()
                        .setTitle(getContext().getString(R.string.driving_mode_title))
                        .addEndItem(actionItem)
                        .build()
                );
        return listBuilder.build();
    }

    private boolean isDrivingModeOn() {
        // TODO(b/369680494): Use mDrivingStateManager.getCurrentCarDrivingState() when it is
        //  updated.
        return mHelper.getCarUxRestrictions().isRequiresDistractionOptimization();
    }

    @Override
    @SuppressLint("MissingPermission")
    void onNotifyChange(Intent intent) {
        boolean newState =
                intent.getBooleanExtra(QC_ACTION_TOGGLE_STATE, /* defaultValue= */ false);

        if (mDrivingStateManager != null) {
            mDrivingStateManager.injectDrivingState(
                    newState ? CarDrivingStateEvent.DRIVING_STATE_MOVING
                            : CarDrivingStateEvent.DRIVING_STATE_PARKED);
        }
    }

    @Override
    protected Uri getUri() {
        return SettingsQCRegistry.DEBUG_DRIVING_MODE_URI;
    }

    @Override
    Class getBackgroundWorkerClass() {
        return DebugDriveModeRowWorker.class;
    }
}
