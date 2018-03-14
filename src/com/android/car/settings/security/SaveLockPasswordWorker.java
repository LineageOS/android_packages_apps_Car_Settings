/*
 * Copyright (C) 2018 Google Inc.
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


import android.content.Intent;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import com.android.internal.widget.LockPatternUtils;

/**
 * Worker to store chosen password using LockPatternUtils.
 */
public class SaveLockPasswordWorker extends SaveChosenLockWorkerBase {

    private final String TAG = "SavePasswordWorker";
    private String mEnteredPassword;
    private String mCurrentPassword;
    private int mRequestedQuality;


    public void start(LockPatternUtils utils,
            String enteredPassword, String currentPassword, int requestedQuality, int userId) {
        prepare(utils, userId);

        mEnteredPassword = enteredPassword;
        mCurrentPassword = currentPassword;
        mRequestedQuality = requestedQuality;

        start();
    }

    @VisibleForTesting
    void saveLockPassword() {
        getUtils().saveLockPassword(mEnteredPassword, mCurrentPassword, mRequestedQuality,
            getUserId());
    }

    @Override
    protected Intent saveAndVerifyInBackground() {
        Intent result = new Intent();
        boolean isSaveSuccessful = true;

        try {
            saveLockPassword();
        } catch (Exception e) {
            Log.e(TAG, "Save lock exception", e);
            isSaveSuccessful = false;
        }

        result.putExtra(EXTRA_KEY_SUCCESS, isSaveSuccessful);
        return result;
    }
}