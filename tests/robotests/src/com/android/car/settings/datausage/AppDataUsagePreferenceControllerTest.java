/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.car.settings.datausage;

import static android.app.usage.NetworkStats.Bucket.UID_TETHERING;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.usage.NetworkStats;
import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ApplicationProvider;

import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.LogicalPreferenceGroup;
import com.android.car.settings.common.PreferenceControllerTestHelper;
import com.android.car.settings.common.ProgressBarPreference;
import com.android.car.settings.testutils.ShadowUidDetailProvider;
import com.android.car.settings.testutils.ShadowUserManager;
import com.android.settingslib.net.UidDetail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/** Unit test for {@link AppDataUsagePreferenceController}. */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowUidDetailProvider.class, ShadowUserManager.class})
public class AppDataUsagePreferenceControllerTest {

    private Context mContext;
    private LogicalPreferenceGroup mLogicalPreferenceGroup;
    private TestAppDataUsagePreferenceController mController;
    private PreferenceControllerTestHelper<TestAppDataUsagePreferenceController>
            mPreferenceControllerHelper;

    @Mock
    private UidDetail mUidDetail;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = ApplicationProvider.getApplicationContext();
        mLogicalPreferenceGroup = new LogicalPreferenceGroup(mContext);
        mPreferenceControllerHelper = new PreferenceControllerTestHelper<>(mContext,
                TestAppDataUsagePreferenceController.class, mLogicalPreferenceGroup);
        mController = mPreferenceControllerHelper.getController();

        mPreferenceControllerHelper.markState(Lifecycle.State.CREATED);
    }

    private static class TestAppDataUsagePreferenceController
            extends AppDataUsagePreferenceController {
        private final Queue<NetworkStats.Bucket> mMockedBuckets = new LinkedBlockingQueue<>();

        TestAppDataUsagePreferenceController(Context context, String preferenceKey,
                FragmentController fragmentController,
                CarUxRestrictions uxRestrictions) {
            super(context, preferenceKey, fragmentController, uxRestrictions);
        }

        public void addBucket(NetworkStats.Bucket bucket) {
            mMockedBuckets.add(bucket);
        }

        @Override
        public boolean hasNextBucket(@NonNull NetworkStats unused) {
            return !mMockedBuckets.isEmpty();
        }

        @Override
        public NetworkStats.Bucket getNextBucket(@NonNull NetworkStats unused) {
            return mMockedBuckets.remove();
        }
    }

    @After
    public void tearDown() {
        ShadowUidDetailProvider.reset();
    }

    @Test
    public void defaultInitialize_hasNoPreference() {
        assertThat(mLogicalPreferenceGroup.getPreferenceCount()).isEqualTo(0);
    }

    @Test
    public void onDataLoaded_dataNotLoaded_hasNoPreference() {
        mController.onDataLoaded(null, new int[0]);

        assertThat(mLogicalPreferenceGroup.getPreferenceCount()).isEqualTo(0);
    }

    @Test
    public void onDataLoaded_statsSizeZero_hasNoPreference() {
        mController.onDataLoaded(mock(NetworkStats.class), new int[0]);

        assertThat(mLogicalPreferenceGroup.getPreferenceCount()).isEqualTo(0);
    }

    private NetworkStats.Bucket getMockBucket(int uid, long rxBytes, long txBytes) {
        NetworkStats.Bucket ret = mock(NetworkStats.Bucket.class);
        when(ret.getUid()).thenReturn(uid);
        when(ret.getRxBytes()).thenReturn(rxBytes);
        when(ret.getTxBytes()).thenReturn(txBytes);
        return ret;
    }

    @Test
    public void onDataLoaded_statsLoaded_hasTwoPreference() {
        mController.addBucket(getMockBucket(0, 100, 0));
        mController.addBucket(getMockBucket(UID_TETHERING, 200, 0));

        mController.onDataLoaded(mock(NetworkStats.class), new int[0]);

        assertThat(mLogicalPreferenceGroup.getPreferenceCount()).isEqualTo(2);
    }

    @Test
    public void onDataLoaded_statsLoaded_hasOnePreference() {
        ShadowUidDetailProvider.setUidDetail(mUidDetail);
        mController.addBucket(getMockBucket(0, 100, 0));
        mController.addBucket(getMockBucket(UID_TETHERING, 200, 0));

        mController.onDataLoaded(mock(NetworkStats.class), new int[0]);

        ProgressBarPreference preference1 =
                (ProgressBarPreference) mLogicalPreferenceGroup.getPreference(0);
        ProgressBarPreference preference2 =
                (ProgressBarPreference) mLogicalPreferenceGroup.getPreference(1);
        assertThat(mLogicalPreferenceGroup.getPreferenceCount()).isEqualTo(2);
        assertThat(preference1.getProgress()).isEqualTo(100);
        assertThat(preference2.getProgress()).isEqualTo(50);
    }
}
