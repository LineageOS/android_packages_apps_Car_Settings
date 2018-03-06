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
import com.android.internal.widget.LockPatternView;

import java.util.List;

/**
 * Async task to save the chosen lock pattern.
 */
public class SaveLockPatternWorker extends SaveChosenLockWorkerBase {

    private final String TAG = "SaveLockPatternWorker";
    private List<LockPatternView.Cell> mChosenPattern;
    private String mCurrentPattern;

    public void start(LockPatternUtils utils,
            List<LockPatternView.Cell> chosenPattern, String currentPattern, int userId) {
        prepare(utils, userId);

        mCurrentPattern = currentPattern;
        mChosenPattern = chosenPattern;

        start();
    }

    @VisibleForTesting
    void saveLockPattern() {
        getUtils().saveLockPattern(mChosenPattern, mCurrentPattern, getUserId());
    }

    @Override
    protected Intent saveAndVerifyInBackground() {
        Intent result = new Intent();
        boolean isSaveSuccessful = true;

        try {
            saveLockPattern();
        } catch (Exception e) {
            Log.e(TAG, "Save lock exception", e);
            isSaveSuccessful = false;
        }

        result.putExtra(EXTRA_KEY_SUCCESS, isSaveSuccessful);
        return result;
    }

    @Override
    protected void finish(Intent resultData) {
        if (!getUtils().isPatternEverChosen(getUserId())) {
            getUtils().setVisiblePatternEnabled(true, getUserId());
        }

        super.finish(resultData);
    }
}