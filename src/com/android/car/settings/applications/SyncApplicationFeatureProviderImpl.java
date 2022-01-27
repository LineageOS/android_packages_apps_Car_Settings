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

package com.android.car.settings.applications;

import com.android.car.settings.common.Logger;
import com.android.car.settingslib.applications.ApplicationFeatureProvider;
import com.android.car.settingslib.applications.ApplicationFeatureProvider.NumberOfAppsCallback;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Default implementation of {@link SyncApplicationFeatureProvider}.
 */
public final class SyncApplicationFeatureProviderImpl implements SyncApplicationFeatureProvider {

    private final Logger mLogger = new Logger(getClass());
    private final ApplicationFeatureProvider mApplicationFeatureProvider;

    public SyncApplicationFeatureProviderImpl(ApplicationFeatureProvider provider) {
        mApplicationFeatureProvider = provider;
    }

    @Override
    public int getNumberOfAppsWithAdminGrantedPermissions(String[] permissions) {
        SyncNumberOfAppsCallback callback = new SyncNumberOfAppsCallback();

        mLogger.d("Calling async provider for permissions " + Arrays.toString(permissions));
        mApplicationFeatureProvider.calculateNumberOfAppsWithAdminGrantedPermissions(permissions,
                /* async= */ false, callback);

        return callback.getResult();
    }

    private final class SyncNumberOfAppsCallback implements NumberOfAppsCallback {

        private static final int TIMEOUT_MS = 20_000;

        private final CountDownLatch mLatch = new CountDownLatch(1);
        private int mResult;

        @Override
        public void onNumberOfAppsResult(int num) {
            mLogger.d("Received result: " + num);
            mResult = num;
            mLatch.countDown();
        }

        int getResult() {
            mLogger.d("Waiting for result on " + Thread.currentThread());
            int result = -1;
            try {
                // It should never time out, but better be cautious (hence the long TIMEOUT_MS)
                if (mLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                    result = mResult;
                } else {
                    mLogger.e("Timed out after " + TIMEOUT_MS + " ms");
                }
            } catch (InterruptedException e) {
                mLogger.w("Interrupted waiting for result", e);
                Thread.currentThread().interrupt();
            }
            return result;
        }
    }
}
