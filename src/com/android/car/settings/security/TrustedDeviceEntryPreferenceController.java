/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.car.settings.security;


import android.app.Activity;
import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.car.settings.common.ActivityResultCallback;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceController;

/**
 * Business logic for trusted device preference.
 */
public class TrustedDeviceEntryPreferenceController extends
        PreferenceController<Preference> implements ActivityResultCallback {

    // Arbitrary request code for starting CheckLockActivity when trusted device is clicked.
    @VisibleForTesting
    protected static final int REQUEST_CODE = 124;

    public TrustedDeviceEntryPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
    }

    @Override
    protected Class<Preference> getPreferenceType() {
        return Preference.class;
    }

    @Override
    public void processActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            getFragmentController().launchFragment(new ChooseTrustedDeviceFragment());
        }
    }

    @Override
    protected boolean handlePreferenceClicked(Preference preference) {
        getFragmentController().startActivityForResult(new Intent(getContext(),
                CheckLockActivity.class), REQUEST_CODE, /* callback= */this);
        return true;
    }
}
