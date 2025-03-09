/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.car.settings.privacy;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import android.Manifest;
import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.SensorPrivacyManager;
import android.os.Process;
import android.os.UserHandle;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.LogicalPreferenceGroup;
import com.android.car.settings.common.PreferenceControllerTestUtil;
import com.android.car.settings.testutils.TestLifecycleOwner;
import com.android.dx.mockito.inline.extended.ExtendedMockito;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoSession;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class MicrophoneInfotainmentAppsPreferenceControllerTest {

    private static final int USER_ID1 = 1;
    private static final int USER_ID2 = 2;
    private LifecycleOwner mLifecycleOwner;
    private Context mContext = ApplicationProvider.getApplicationContext();
    private LogicalPreferenceGroup mPreference;
    private MicrophoneInfotainmentAppsPreferenceController mPreferenceController;
    private CarUxRestrictions mCarUxRestrictions;
    private MockitoSession mSession;
    private CharSequence mBadgedAppLabel1 = "badgedAppLabel1";
    private CharSequence mBadgedAppLabel2 = "badgedAppLabel2";
    @Mock
    private FragmentController mFragmentController;
    @Mock
    private PackageManager mMockPackageManager;
    @Mock
    private SensorPrivacyManager mMockSensorPrivacyManager;
    private UserHandle mUserHandle;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mLifecycleOwner = new TestLifecycleOwner();
        mSession = ExtendedMockito.mockitoSession()
                .mockStatic(PermissionUtils.class, withSettings().lenient())
                .startMocking();

        mUserHandle = Process.myUserHandle();
        mCarUxRestrictions = new CarUxRestrictions.Builder(/* reqOpt= */ true,
                CarUxRestrictions.UX_RESTRICTIONS_BASELINE, /* timestamp= */ 0).build();
        PreferenceManager preferenceManager = new PreferenceManager(mContext);
        PreferenceScreen screen = preferenceManager.createPreferenceScreen(mContext);
        mPreference = new LogicalPreferenceGroup(mContext);
        screen.addPreference(mPreference);
        mPreferenceController = new MicrophoneInfotainmentAppsPreferenceController(mContext,
                /* preferenceKey= */ "key", mFragmentController, mCarUxRestrictions,
                mMockPackageManager, mMockSensorPrivacyManager);
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
    }

    @After
    public void tearDown() {
        if (mSession != null) {
            mSession.finishMocking();
        }
    }

    @Test
    public void onCreate_verifySensorType_isMicrophone() {
        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.onStart(mLifecycleOwner);

        assertThat(mPreferenceController.getSensorPrivacyManager()).isEqualTo(
                mMockSensorPrivacyManager);
        assertThat(mPreferenceController.getPrivacySensorType()).isEqualTo(
                SensorPrivacyManager.Sensors.MICROPHONE);
    }

    @Test
    public void onCreate_verifyChildrenPreference() throws PackageManager.NameNotFoundException {
        initChildren();
        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.onStart(mLifecycleOwner);

        assertThat(mPreference.getPreferenceCount()).isEqualTo(2);
        assertThat(mPreference.getPreference(0).getTitle()).isEqualTo(mBadgedAppLabel1);
        assertThat(mPreference.getPreference(1).getTitle()).isEqualTo(mBadgedAppLabel2);
    }

    @Test
    public void onCreate_triggerListener_uiUpdate() throws PackageManager.NameNotFoundException {
        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.onStart(mLifecycleOwner);
        assertThat(mPreference.getPreferenceCount()).isEqualTo(0);

        initChildren();
        SensorPrivacyManager.OnSensorPrivacyChangedListener.SensorPrivacyChangedParams params =
                mock(SensorPrivacyManager.OnSensorPrivacyChangedListener
                        .SensorPrivacyChangedParams.class);
        mPreferenceController.getListener().onSensorPrivacyChanged(params);

        assertThat(mPreference.getPreferenceCount()).isEqualTo(2);
        assertThat(mPreference.getPreference(0).getTitle()).isEqualTo(mBadgedAppLabel1);
        assertThat(mPreference.getPreference(1).getTitle()).isEqualTo(mBadgedAppLabel2);
    }

    private void initChildren() throws PackageManager.NameNotFoundException {
        String packageName1 = "packageName1";
        CharSequence appLabel1 = "appLabel1";
        PackageInfo packageInfo1 = createPackageInfo(packageName1, USER_ID1, appLabel1,
                mBadgedAppLabel1);
        String packageName2 = "packageName2";
        CharSequence appLabel2 = "appLabel2";
        PackageInfo packageInfo2 = createPackageInfo(packageName2, USER_ID2, appLabel2,
                mBadgedAppLabel2);
        when(PermissionUtils.getPackagesWithPermissionGroup(
                mContext, Manifest.permission_group.MICROPHONE, mUserHandle,
                /* showSystem= */ false)).thenReturn(List.of(packageInfo1, packageInfo2));
    }

    private PackageInfo createPackageInfo(String packageName, int uid, CharSequence appLabel,
            CharSequence badgedAppLabel) throws PackageManager.NameNotFoundException {
        ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.packageName = packageName;
        applicationInfo.uid = uid;

        PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = packageName;
        packageInfo.applicationInfo = applicationInfo;
        int userId = mUserHandle.getIdentifier();
        when(mMockPackageManager.getApplicationInfoAsUser(
                packageName, PackageManager.GET_META_DATA, userId)).thenReturn(applicationInfo);
        when(mMockPackageManager.getApplicationLabel(applicationInfo)).thenReturn(appLabel);
        when(mMockPackageManager.getUserBadgedLabel(appLabel, mUserHandle)).thenReturn(
                badgedAppLabel);

        return packageInfo;
    }
}
