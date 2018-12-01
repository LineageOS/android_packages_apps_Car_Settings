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

package com.android.car.settings.common;

import android.content.Context;

/**
 * Concrete {@link BasePreferenceController} with an accessible {@link AvailabilityStatus}.
 */
public class FakeBasePreferenceController extends BasePreferenceController {

    @AvailabilityStatus
    private int mAvailabilityStatus = AVAILABLE;

    public FakeBasePreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController) {
        super(context, preferenceKey, fragmentController);
    }

    @Override
    @AvailabilityStatus
    public int getAvailabilityStatus() {
        return mAvailabilityStatus;
    }

    public void setAvailabilityStatus(@AvailabilityStatus int availabilityStatus) {
        mAvailabilityStatus = availabilityStatus;
    }
}
