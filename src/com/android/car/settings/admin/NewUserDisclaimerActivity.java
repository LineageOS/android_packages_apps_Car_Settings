/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.car.settings.admin;

import android.app.Activity;
import android.car.Car;
import android.car.admin.CarDevicePolicyManager;
import android.os.Bundle;
import android.widget.Button;

import com.android.car.settings.R;
import com.android.car.settings.common.Logger;
import com.android.internal.annotations.VisibleForTesting;

/**
 * Shows a disclaimer when a new user is added in a device that is managed by a device owner.
 */
public final class NewUserDisclaimerActivity extends Activity {
    private static final Logger LOG = new Logger(NewUserDisclaimerActivity.class);

    private Car mCar;
    private CarDevicePolicyManager mCarDevicePolicyManager;
    private Button mAcceptButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.new_user_disclaimer);

        mAcceptButton = findViewById(R.id.accept_button);
        mAcceptButton.setOnClickListener((v) -> accept());

        mCar = Car.createCar(this);
        mCarDevicePolicyManager = (CarDevicePolicyManager) mCar.getCarManager(
                Car.CAR_DEVICE_POLICY_SERVICE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        LOG.d("showing UI");
        mCarDevicePolicyManager.setUserDisclaimerShown(getUser());
    }

    @VisibleForTesting
    Button getAcceptButton() {
        return mAcceptButton;
    }

    private void accept() {
        LOG.d("user accepted");
        mCarDevicePolicyManager.setUserDisclaimerAcknowledged(getUser());
        finish();
    }
}
