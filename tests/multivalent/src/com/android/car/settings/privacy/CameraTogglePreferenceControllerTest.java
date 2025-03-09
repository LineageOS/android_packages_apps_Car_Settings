/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static com.android.car.settings.common.PreferenceController.AVAILABLE;
import static com.android.car.settings.common.PreferenceController.AVAILABLE_FOR_VIEWING;
import static com.android.car.settings.common.PreferenceController.DISABLED_FOR_PROFILE;
import static com.android.car.settings.common.PreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.hardware.SensorPrivacyManager;
import android.hardware.SensorPrivacyManager.OnSensorPrivacyChangedListener;
import android.os.UserHandle;
import android.os.UserManager;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.lifecycle.LifecycleOwner;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.car.settings.common.ColoredSwitchPreference;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceControllerTestUtil;
import com.android.car.settings.testutils.TestLifecycleOwner;
import com.android.internal.camera.flags.Flags;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class CameraTogglePreferenceControllerTest {
    private LifecycleOwner mLifecycleOwner;
    private Context mContext = spy(ApplicationProvider.getApplicationContext());
    private ColoredSwitchPreference mSwitchPreference;
    private CameraTogglePreferenceController mPreferenceController;
    private CarUxRestrictions mCarUxRestrictions;
    private UserHandle mUserHandle;

    @Mock
    private FragmentController mFragmentController;
    @Mock
    private SensorPrivacyManager mMockSensorPrivacyManager;
    @Mock
    private OnSensorPrivacyChangedListener.SensorPrivacyChangedParams mSensorPrivacyChangedParams;
    @Mock
    private UserManager mMockUserManager;
    @Captor
    private ArgumentCaptor<OnSensorPrivacyChangedListener> mListener;

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Before
    @UiThreadTest
    public void setUp() {
        mSetFlagsRule.disableFlags(Flags.FLAG_CAMERA_PRIVACY_ALLOWLIST);
        mLifecycleOwner = new TestLifecycleOwner();
        MockitoAnnotations.initMocks(this);
        when(mContext.getSystemService(UserManager.class)).thenReturn(mMockUserManager);
        mUserHandle = mContext.getUser();
        setCameraMuteFeatureAvailable(true);

        mCarUxRestrictions = new CarUxRestrictions.Builder(/* reqOpt= */ true,
                CarUxRestrictions.UX_RESTRICTIONS_BASELINE, /* timestamp= */ 0).build();

        mSwitchPreference = new ColoredSwitchPreference(mContext);
        mPreferenceController = new CameraTogglePreferenceController(mContext,
                /* preferenceKey= */ "key", mFragmentController, mCarUxRestrictions,
                mMockSensorPrivacyManager);
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mSwitchPreference);
    }

    @Test
    public void cameraMuteUnavailable_preferenceIsHidden() {
        setCameraMuteFeatureAvailable(false);
        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.onStart(mLifecycleOwner);

        assertThat(mPreferenceController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void cameraMuteAvailable_preferenceIsShown() {
        setCameraMuteFeatureAvailable(true);
        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.onStart(mLifecycleOwner);

        assertThat(mPreferenceController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void baseUserRestricted_preferenceIsShown_zoneWrite() {
        when(mMockSensorPrivacyManager
                .supportsSensorToggle(eq(SensorPrivacyManager.Sensors.MICROPHONE)))
                .thenReturn(true);
        when(mMockUserManager.hasBaseUserRestriction(eq(UserManager.DISALLOW_CAMERA_TOGGLE),
                eq(mUserHandle))).thenReturn(true);
        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.onStart(mLifecycleOwner);

        PreferenceControllerTestUtil.assertAvailability(
                mPreferenceController.getAvailabilityStatus(), DISABLED_FOR_PROFILE);
    }

    @Test
    public void userRestricted_preferenceIsShown_zoneWrite() {
        when(mMockSensorPrivacyManager
                .supportsSensorToggle(eq(SensorPrivacyManager.Sensors.MICROPHONE)))
                .thenReturn(true);
        when(mMockUserManager.hasBaseUserRestriction(eq(UserManager.DISALLOW_CAMERA_TOGGLE),
                eq(mUserHandle))).thenReturn(false);
        when(mMockUserManager.hasUserRestriction(eq(UserManager.DISALLOW_CAMERA_TOGGLE)))
                .thenReturn(true);
        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.onStart(mLifecycleOwner);

        PreferenceControllerTestUtil.assertAvailability(
                mPreferenceController.getAvailabilityStatus(), AVAILABLE_FOR_VIEWING);
    }

    @Test
    public void onPreferenceClicked_clickCameraEnabled_shouldSetPrivacySensor() {
        initializePreference(/* isCameraEnabled= */ true);
        assertThat(mSwitchPreference.isChecked()).isTrue();

        mSwitchPreference.performClick();

        verify(mMockSensorPrivacyManager).setSensorPrivacyForProfileGroup(
                eq(SensorPrivacyManager.Sources.SETTINGS),
                eq(SensorPrivacyManager.Sensors.CAMERA),
                eq(true));
        setIsSensorPrivacyEnabled(true);

        mListener.getValue().onSensorPrivacyChanged(mSensorPrivacyChangedParams);

        assertThat(mSwitchPreference.isChecked()).isFalse();
    }

    @Test
    public void onPreferenceClicked_clickCameraDisabled_shouldClearPrivacySensor() {
        initializePreference(/* isCameraEnabled= */ false);
        assertThat(mSwitchPreference.isChecked()).isFalse();

        mSwitchPreference.performClick();

        verify(mMockSensorPrivacyManager).setSensorPrivacyForProfileGroup(
                eq(SensorPrivacyManager.Sources.SETTINGS),
                eq(SensorPrivacyManager.Sensors.CAMERA),
                eq(false));
        setIsSensorPrivacyEnabled(false);
        mPreferenceController.refreshUi();

        assertThat(mSwitchPreference.isChecked()).isTrue();
    }

    @Test
    public void onListenerUpdate_cameraDisabled_shouldUpdateChecked() {
        initializePreference(/* isCameraEnabled= */ false);

        setIsSensorPrivacyEnabled(false);
        mPreferenceController.refreshUi();

        assertThat(mSwitchPreference.isChecked()).isTrue();
    }

    @Test
    public void onListenerUpdate_cameraEnabled_shouldUpdateChecked() {
        initializePreference(/* isCameraEnabled= */ true);

        setIsSensorPrivacyEnabled(true);
        mListener.getValue().onSensorPrivacyChanged(mSensorPrivacyChangedParams);

        assertThat(mSwitchPreference.isChecked()).isFalse();
    }

    @Test
    public void onStop_removesSensorPrivacyListener() {
        initializePreference(/* isCameraEnabled= */ true);
        mPreferenceController.onStop(mLifecycleOwner);

        verify(mMockSensorPrivacyManager).removeSensorPrivacyListener(
                SensorPrivacyManager.Sensors.CAMERA, mListener.getValue());
    }

    private void initializePreference(boolean isCameraEnabled) {
        setIsSensorPrivacyEnabled(!isCameraEnabled);
        mPreferenceController.onCreate(mLifecycleOwner);
        mPreferenceController.onStart(mLifecycleOwner);
        if (isCameraEnabled) {
            verify(mMockSensorPrivacyManager).addSensorPrivacyListener(
                    eq(SensorPrivacyManager.Sensors.CAMERA), mListener.capture());
        }
    }

    private void setIsSensorPrivacyEnabled(boolean isMuted) {
        when(mMockSensorPrivacyManager.isSensorPrivacyEnabled(
                eq(SensorPrivacyManager.Sensors.CAMERA))).thenReturn(isMuted);
    }

    private void setCameraMuteFeatureAvailable(boolean isAvailable) {
        when(mMockSensorPrivacyManager
                .supportsSensorToggle(eq(SensorPrivacyManager.Sensors.CAMERA)))
                .thenReturn(isAvailable);
        when(mMockUserManager.hasBaseUserRestriction(eq(UserManager.DISALLOW_CAMERA_TOGGLE),
                eq(mUserHandle))).thenReturn(!isAvailable);
    }
}
