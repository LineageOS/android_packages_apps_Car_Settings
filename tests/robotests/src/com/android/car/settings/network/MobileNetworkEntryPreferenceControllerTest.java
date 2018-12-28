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

package com.android.car.settings.network;

import static com.android.car.settings.common.PreferenceController.AVAILABLE;
import static com.android.car.settings.common.PreferenceController.DISABLED_FOR_USER;
import static com.android.car.settings.common.PreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;
import static org.robolectric.shadow.api.Shadow.extract;

import android.car.userlib.CarUserManagerHelper;
import android.content.Context;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.UserManager;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import androidx.lifecycle.Lifecycle;
import androidx.preference.Preference;

import com.android.car.settings.CarSettingsRobolectricTestRunner;
import com.android.car.settings.common.PreferenceControllerTestHelper;
import com.android.car.settings.testutils.ShadowCarUserManagerHelper;
import com.android.car.settings.testutils.ShadowConnectivityManager;
import com.android.car.settings.testutils.ShadowTelephonyManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowNetwork;

@RunWith(CarSettingsRobolectricTestRunner.class)
@Config(shadows = {ShadowCarUserManagerHelper.class, ShadowConnectivityManager.class,
        ShadowTelephonyManager.class})
public class MobileNetworkEntryPreferenceControllerTest {

    private static final String TEST_NETWORK_NAME = "test network name";
    private static final UserInfo TEST_ADMIN_USER = new UserInfo(10, "test_name",
            UserInfo.FLAG_ADMIN);
    private static final UserInfo TEST_NON_ADMIN_USER = new UserInfo(10, "test_name",
            /* flags= */ 0);

    private Context mContext;
    private Preference mPreference;
    private PreferenceControllerTestHelper<MobileNetworkEntryPreferenceController>
            mControllerHelper;
    private MobileNetworkEntryPreferenceController mController;
    @Mock
    private CarUserManagerHelper mCarUserManagerHelper;
    @Mock
    private NetworkCapabilities mNetworkCapabilities;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowCarUserManagerHelper.setMockInstance(mCarUserManagerHelper);
        mContext = RuntimeEnvironment.application;
        mPreference = new Preference(mContext);
        mControllerHelper = new PreferenceControllerTestHelper<>(mContext,
                MobileNetworkEntryPreferenceController.class, mPreference);
        mController = mControllerHelper.getController();

        // Setup to always make preference available.
        getShadowConnectivityManager().clearAllNetworks();
        getShadowConnectivityManager().addNetworkCapabilities(
                ShadowNetwork.newInstance(ConnectivityManager.TYPE_MOBILE), mNetworkCapabilities);
        when(mNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)).thenReturn(
                true);
        when(mCarUserManagerHelper.getCurrentProcessUserInfo()).thenReturn(TEST_ADMIN_USER);
        when(mCarUserManagerHelper.isCurrentProcessUserHasRestriction(
                UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS)).thenReturn(false);
    }

    @After
    public void tearDown() {
        ShadowCarUserManagerHelper.reset();
        ShadowConnectivityManager.reset();
        ShadowTelephonyManager.reset();
    }

    @Test
    public void onStart_phoneStateListenerSet() {
        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_START);
        assertThat(getShadowTelephonyManager().getListenersForFlags(
                PhoneStateListener.LISTEN_SERVICE_STATE).size()).isEqualTo(1);
    }

    @Test
    public void onStop_phoneStateListenerUnset() {
        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_START);
        assertThat(getShadowTelephonyManager().getListenersForFlags(
                PhoneStateListener.LISTEN_SERVICE_STATE).size()).isEqualTo(1);

        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_STOP);
        assertThat(getShadowTelephonyManager().getListenersForFlags(
                PhoneStateListener.LISTEN_SERVICE_STATE).size()).isEqualTo(0);
    }

    @Test
    public void onStart_airplaneModeChangedListenerSet() {
        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_START);
        // One receiver (bluetooth pairing request) is always registered through the manifest.
        assertThat(ShadowApplication.getInstance().getRegisteredReceivers().size()).isGreaterThan(
                0);

        boolean hasMatch = false;
        for (ShadowApplication.Wrapper wrapper :
                ShadowApplication.getInstance().getRegisteredReceivers()) {
            if (wrapper.getIntentFilter().getAction(0) == Intent.ACTION_AIRPLANE_MODE_CHANGED) {
                hasMatch = true;
            }
        }
        assertThat(hasMatch).isTrue();
    }

    @Test
    public void onStop_airplaneModeChangedListenerUnset() {
        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_START);
        int prevSize = ShadowApplication.getInstance().getRegisteredReceivers().size();
        assertThat(prevSize).isGreaterThan(0);
        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_STOP);
        assertThat(ShadowApplication.getInstance().getRegisteredReceivers().size()).isLessThan(
                prevSize);

        boolean hasMatch = false;
        for (ShadowApplication.Wrapper wrapper :
                ShadowApplication.getInstance().getRegisteredReceivers()) {
            if (wrapper.getIntentFilter().getAction(0) == Intent.ACTION_AIRPLANE_MODE_CHANGED) {
                hasMatch = true;
            }
        }
        assertThat(hasMatch).isFalse();
    }

    @Test
    public void getAvailabilityStatus_noMobileNetwork_unsupported() {
        when(mNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)).thenReturn(
                false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_notAdmin_disabledForUser() {
        when(mNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)).thenReturn(
                true);
        when(mCarUserManagerHelper.getCurrentProcessUserInfo()).thenReturn(TEST_NON_ADMIN_USER);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(DISABLED_FOR_USER);
    }

    @Test
    public void getAvailabilityStatus_hasRestriction_disabledForUser() {
        when(mNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)).thenReturn(
                true);
        when(mCarUserManagerHelper.getCurrentProcessUserInfo()).thenReturn(TEST_ADMIN_USER);
        when(mCarUserManagerHelper.isCurrentProcessUserHasRestriction(
                UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS)).thenReturn(true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(DISABLED_FOR_USER);
    }

    @Test
    public void getAvailabilityStatus_hasMobileNetwork_isAdmin_noRestriction_available() {
        when(mNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)).thenReturn(
                true);
        when(mCarUserManagerHelper.getCurrentProcessUserInfo()).thenReturn(TEST_ADMIN_USER);
        when(mCarUserManagerHelper.isCurrentProcessUserHasRestriction(
                UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS)).thenReturn(false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void refreshUi_airplaneModeOn_preferenceDisabled() {
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 1);
        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);
        mController.refreshUi();

        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void refreshUi_airplaneModeOff_preferenceEnabled() {
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0);
        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);
        mController.refreshUi();

        assertThat(mPreference.isEnabled()).isTrue();
    }

    @Test
    public void refreshUi_summarySet() {
        getShadowTelephonyManager().setNetworkOperatorName(TEST_NETWORK_NAME);
        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);
        mController.refreshUi();

        assertThat(mPreference.getSummary()).isEqualTo(TEST_NETWORK_NAME);
    }

    @Test
    public void sendBroadcast_airplaneModeOn_disablePreference() {
        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_START);
        mPreference.setEnabled(true);
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 1);
        mContext.sendBroadcast(new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED));

        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void sendBroadcast_airplaneModeOff_enablePreference() {
        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_START);
        mPreference.setEnabled(false);
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0);
        mContext.sendBroadcast(new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED));

        assertThat(mPreference.isEnabled()).isTrue();
    }

    private ShadowTelephonyManager getShadowTelephonyManager() {
        return (ShadowTelephonyManager) extract(mContext.getSystemService(TelephonyManager.class));
    }

    private ShadowConnectivityManager getShadowConnectivityManager() {
        return (ShadowConnectivityManager) extract(
                mContext.getSystemService(ConnectivityManager.class));
    }
}
