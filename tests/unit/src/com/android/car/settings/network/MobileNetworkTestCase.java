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

package com.android.car.settings.network;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.TelephonyNetworkSpecifier;
import android.os.UserManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.lifecycle.LifecycleOwner;
import androidx.test.core.app.ApplicationProvider;

import com.android.car.settings.common.FragmentController;
import com.android.car.settings.testutils.TestLifecycleOwner;

import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class MobileNetworkTestCase {

    protected static final String TEST_DISPLAY_NAME = "test display name";
    protected static final int TEST_SUBSCRIPTION_ID = 1;
    protected static final int TEST_OTHER_SUBSCRIPTION_ID = 2;

    protected Context mContext = spy(ApplicationProvider.getApplicationContext());
    protected LifecycleOwner mLifecycleOwner;
    protected CarUxRestrictions mCarUxRestrictions;

    @Mock
    protected FragmentController mFragmentController;
    @Mock
    protected UserManager mUserManager;
    @Mock
    protected SubscriptionManager mSubscriptionManager;
    @Mock
    protected Network mNetwork;
    @Mock
    protected NetworkCapabilities mNetworkCapabilities;
    @Mock
    protected TelephonyNetworkSpecifier mTelephonyNetworkSpecifier;
    @Mock
    protected TelephonyManager mTelephonyManager;
    @Mock
    private ConnectivityManager mConnectivityManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mLifecycleOwner = new TestLifecycleOwner();

        // Setup to always make preference available.
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        when(mContext.getSystemService(SubscriptionManager.class)).thenReturn(mSubscriptionManager);
        when(mContext.getSystemService(ConnectivityManager.class)).thenReturn(mConnectivityManager);
        when(mContext.getSystemService(TelephonyManager.class)).thenReturn(mTelephonyManager);

        when(mUserManager.isAdminUser()).thenReturn(true);
        when(mUserManager.hasUserRestriction(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS))
                .thenReturn(false);

        when(mNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)).thenReturn(
                true);
        when(mConnectivityManager.getNetworkCapabilities(mNetwork))
                .thenReturn(mNetworkCapabilities);
        when(mConnectivityManager.getAllNetworks()).thenReturn(new Network[]{mNetwork});
        when(mNetworkCapabilities.getNetworkSpecifier()).thenReturn(mTelephonyNetworkSpecifier);
        when(mTelephonyNetworkSpecifier.getSubscriptionId()).thenReturn(TEST_SUBSCRIPTION_ID);
        when(mTelephonyManager.createForSubscriptionId(anyInt())).thenReturn(mTelephonyManager);
        when(mTelephonyManager.getNetworkOperatorName()).thenReturn(TEST_DISPLAY_NAME);
        when(mTelephonyManager.getSimState()).thenReturn(TelephonyManager.SIM_STATE_PRESENT);

        mCarUxRestrictions = new CarUxRestrictions.Builder(/* reqOpt= */ true,
                CarUxRestrictions.UX_RESTRICTIONS_BASELINE, /* timestamp= */ 0).build();
    }

    protected SubscriptionInfo createSubscriptionInfo(int subId, int simSlotIndex,
            String displayName) {
        SubscriptionInfo subInfo = new SubscriptionInfo(subId, /* iccId= */ "",
                simSlotIndex, displayName, /* carrierName= */ "",
                /* nameSource= */ 0, /* iconTint= */ 0, /* number= */ "",
                /* roaming= */ 0, /* icon= */ null, /* mcc= */ "", "mncString",
                /* countryIso= */ "", /* isEmbedded= */ false,
                /* accessRules= */ null, /* cardString= */ "");
        return subInfo;
    }
}
