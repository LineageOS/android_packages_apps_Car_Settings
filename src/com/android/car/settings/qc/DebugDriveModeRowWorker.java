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

import android.car.Car;
import android.car.drivingstate.CarDrivingStateManager;
import android.content.Context;
import android.net.Uri;

import java.io.IOException;

/**
 * Worker for {@link DebugDriveModeRow}.
 */
public class DebugDriveModeRowWorker extends SettingsQCBackgroundWorker<DebugDriveModeRow> {

    private CarDrivingStateManager mDrivingStateManager;

    public DebugDriveModeRowWorker(Context context, Uri uri) {
        super(context, uri);
        Car car = Car.createCar(getContext());
        if (car != null) {
            mDrivingStateManager =
                    (CarDrivingStateManager) car.getCarManager(Car.CAR_DRIVING_STATE_SERVICE);
        }
    }

    @Override
    protected void onQCItemSubscribe() {
        notifyQCItemChange();
        if (mDrivingStateManager != null) {
            mDrivingStateManager.registerListener(event -> notifyQCItemChange());
        }
    }

    @Override
    protected void onQCItemUnsubscribe() {
        if (mDrivingStateManager != null) {
            mDrivingStateManager.unregisterListener();
        }
    }

    @Override
    public void close() throws IOException {
    }
}
