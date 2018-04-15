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

import com.android.internal.widget.LockPatternView;

import java.util.List;

/**
 * An invisible retained worker fragment to track the AsyncWork that saves the chosen lock pattern.
 */
public class SavePatternWorker extends SaveLockWorkerBase {

    private List<LockPatternView.Cell> mChosenPattern;
    private String mCurrentPattern;

    void start(int userId, List<LockPatternView.Cell> chosenPattern, String currentPattern) {
        init(userId);
        mCurrentPattern = currentPattern;
        mChosenPattern = chosenPattern;
        start();
    }

    @Override
    void saveLock() {
        getUtils().saveLockPattern(mChosenPattern, mCurrentPattern, getUserId());

        if (!getUtils().isPatternEverChosen(getUserId())) {
            getUtils().setVisiblePatternEnabled(true, getUserId());
        }
    }
}
