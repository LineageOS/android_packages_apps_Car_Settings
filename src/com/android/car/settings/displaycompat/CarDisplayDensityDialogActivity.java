/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.car.settings.displaycompat;

import static android.view.Display.DEFAULT_DISPLAY;

import static com.android.car.oem.tokens.Token.applyOemTokenStyle;
import static com.android.systemui.car.Flags.displayCompatibilityV2;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.car.Car;
import android.car.content.pm.CarPackageManager;
import android.content.ComponentName;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

import com.android.car.settings.R;

/**
 * Activity to show a dialog for setting the Display Density.
 */
public class CarDisplayDensityDialogActivity extends Activity {
    private static final String TAG = "CarDisplayDensityDA";
    private static final String ACTION_SHOW_DIALOG =
            "com.android.car.settings.displayDensity.action.SHOW_DIALOG";
    private static final String EXTRA_KEY_COMPONENT_NAME =
            "com.android.car.settings.displayDensity.extra.COMPONENT_NAME";
    private static final String EXTRA_KEY_USER_ID =
            "com.android.car.settings.displayDensity.extra.USER_ID";
    private static final String EXTRA_KEY_DISPLAY_ID =
            "com.android.car.settings.displayDensity.extra.DISPLAY_ID";

    private Car mCar;
    private CarPackageManager mCarPackageManager;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!displayCompatibilityV2()) {
            finish();
            return;
        }

        if (!ACTION_SHOW_DIALOG.equals(getIntent().getAction())) {
            Log.e(TAG, "Incorrect action sent to the receiver: " + getIntent().getAction());
            finish();
            return;
        }
        ComponentName componentName =
                getIntent().getParcelableExtra(EXTRA_KEY_COMPONENT_NAME, ComponentName.class);
        if (componentName == null) {
            Log.e(TAG, "No component sent in extra with key: " + EXTRA_KEY_COMPONENT_NAME);
            finish();
            return;
        }
        int userId = getIntent().getIntExtra(EXTRA_KEY_USER_ID, this.getUserId());
        int displayId = getIntent().getIntExtra(EXTRA_KEY_DISPLAY_ID, DEFAULT_DISPLAY);

        if (mCarPackageManager == null) {
            if (mCar == null) {
                mCar = Car.createCar(this);
            }
            if (mCar == null) {
                Log.e(TAG, "Could not get a car instance");
                finish();
                return;
            }
            mCarPackageManager = mCar.getCarManager(CarPackageManager.class);
        }

        applyOemTokenStyle(this);

        setContentView(R.layout.two_column_radio_dialog_activity);
        CarDisplayDensityDialogHelper dialogHelper = new CarDisplayDensityDialogHelper(this,
                mCarPackageManager, /* dismissDialogRunnable= */ () -> finish(), componentName,
                userId, displayId);

        View dialogView = findViewById(R.id.dialog_view);
        if (dialogView == null) {
            throw new IllegalStateException(
                    "Display density dialog requires the dialog container with id "
                            + "\"dialog_view\".");
        }
        dialogHelper.setupDialog(dialogView);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCar != null) {
            mCar.disconnect();
            mCar = null;
        }
        mCarPackageManager = null;
    }
}
