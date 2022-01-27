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

import static com.google.common.truth.Truth.assertWithMessage;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;

import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.car.settingslib.applications.ApplicationFeatureProvider;
import com.android.car.settingslib.applications.ApplicationFeatureProvider.NumberOfAppsCallback;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(AndroidJUnit4.class)
public final class SyncApplicationFeatureProviderImplTest {

    private static final String TAG = SyncApplicationFeatureProviderImplTest.class.getSimpleName();

    private static final String[] PERMISSIONS = new String[] { "License", "To", "Kill" };

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    private ApplicationFeatureProvider mProvider;

    private SyncApplicationFeatureProviderImpl mSyncProvider;

    @Before
    public void setProvider() {
        mSyncProvider = new SyncApplicationFeatureProviderImpl(mProvider);
    }

    @Test
    public void testGetNumberOfAppsWithAdminGrantedPermissions() {
        doAnswer((inv) -> {
            Log.d(TAG, "answering to " + inv);
            NumberOfAppsCallback callback = (NumberOfAppsCallback) inv.getArguments()[2];
            callback.onNumberOfAppsResult(0x07);
            return null;
        }).when(mProvider).calculateNumberOfAppsWithAdminGrantedPermissions(eq(PERMISSIONS),
                /* async= */ eq(false), any());

        assertWithMessage("getNumberOfAppsWithAdminGrantedPermissions()").that(mSyncProvider
                .getNumberOfAppsWithAdminGrantedPermissions(PERMISSIONS)).isEqualTo(0x07);
    }

    @Test
    public void testGetNumberOfAppsWithAdminGrantedPermissions_timedOut() {
        assertWithMessage("getNumberOfAppsWithAdminGrantedPermissions()").that(mSyncProvider
                .getNumberOfAppsWithAdminGrantedPermissions(PERMISSIONS)).isEqualTo(-1);
    }

    @Test
    public void testGetNumberOfAppsWithAdminGrantedPermissions_interrupted() throws Exception {
        Log.d(TAG, "Starting test on thread " + Thread.currentThread());

        AtomicInteger resultHolder = new AtomicInteger(42);
        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);

        Thread thread = new Thread(() -> {
            Log.d(TAG, "Counting down latch1 " + latch1);
            latch1.countDown();
            Log.d(TAG, "Calling provider on thread " + Thread.currentThread()
                    + "; should block until thread is interrupted");
            int num = mSyncProvider.getNumberOfAppsWithAdminGrantedPermissions(PERMISSIONS);
            Log.d(TAG, "Received result: " + num);
            resultHolder.set(num);
            Log.d(TAG, "Counting down latch2 " + latch2);
            latch2.countDown();
        }, "SyncApplicationFeatureProviderImplTestThread");

        Log.d(TAG, "Starting thread " + thread);
        thread.start();

        Log.d(TAG, "Waiting for latch1 " + latch1);
        latch1.await();

        Log.d(TAG, "Interrupting thread " + thread);
        thread.interrupt();

        Log.d(TAG, "Waiting for latch2 " + latch2);
        latch2.await();

        Log.d(TAG, "Checking result");
        int result = resultHolder.get();
        if (result == 42) {
            fail("Thread not properly interrupted");
        }
        assertWithMessage("getNumberOfAppsWithAdminGrantedPermissions()").that(result)
                .isEqualTo(-1);
    }
}
