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

package com.android.car.settings;

import static com.android.dx.mockito.inline.extended.ExtendedMockito.doAnswer;
import static com.android.dx.mockito.inline.extended.ExtendedMockito.mockitoSession;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import android.car.Car;
import android.car.Car.CarServiceLifecycleListener;
import android.car.CarOccupantZoneManager;
import android.car.CarOccupantZoneManager.OccupantZoneConfigChangeListener;
import android.car.CarOccupantZoneManager.OccupantZoneInfo;
import android.car.VehicleAreaSeat;
import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoSession;

@RunWith(AndroidJUnit4.class)
public class CarSettingsApplicationTest {

    private Context mContext = ApplicationProvider.getApplicationContext();
    private MockitoSession mSession;

    @Mock
    private Car mCar;
    @Mock
    protected CarOccupantZoneManager mCarOccupantZoneManager;

    public final OccupantZoneInfo mZoneInfoDriver = new OccupantZoneInfo(0,
            CarOccupantZoneManager.OCCUPANT_TYPE_DRIVER,
            VehicleAreaSeat.SEAT_ROW_1_LEFT);
    public final OccupantZoneInfo mZoneInfoPassenger = new OccupantZoneInfo(1,
            CarOccupantZoneManager.OCCUPANT_TYPE_FRONT_PASSENGER,
            VehicleAreaSeat.SEAT_ROW_1_RIGHT);
    private CarSettingsApplication mCarSettingsApplication;
    private CarServiceLifecycleListener mCarServiceLifecycleListener;
    private OccupantZoneConfigChangeListener mConfigChangeListener;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mSession = mockitoSession().mockStatic(Car.class,
                withSettings().lenient()).startMocking();

        doAnswer(invocation -> {
            mCarServiceLifecycleListener = invocation.getArgument(3);
            return mCar;
        }).when(() -> Car.createCar(any(), any(), anyLong(), any()));

        doAnswer(invocation -> {
            mConfigChangeListener = (OccupantZoneConfigChangeListener) invocation.getArgument(0);
            return null;
        }).when(mCarOccupantZoneManager).registerOccupantZoneConfigChangeListener(any());

        mCarSettingsApplication = new CarSettingsApplication();
        mCarSettingsApplication.onCreate();
    }

    @After
    public void tearDown() {
        if (mSession != null) {
            mSession.finishMocking();
        }
    }

    @Test
    public void onLifecycleChanged_carServiceNotReady() {
        mCarServiceLifecycleListener.onLifecycleChanged(null, false);

        assertThat(mCarSettingsApplication.getMyOccupantZoneType()).isEqualTo(
                CarOccupantZoneManager.OCCUPANT_TYPE_INVALID);
    }

    @Test
    public void onLifecycleChanged_carServiceReady_zoneInfoDriver() {
        when(mCar.getCarManager(Car.CAR_OCCUPANT_ZONE_SERVICE)).thenReturn(mCarOccupantZoneManager);
        when(mCarOccupantZoneManager.getMyOccupantZone()).thenReturn(mZoneInfoDriver);
        mCarServiceLifecycleListener.onLifecycleChanged(mCar, true);

        assertThat(mCarSettingsApplication.getMyOccupantZoneType()).isEqualTo(
                CarOccupantZoneManager.OCCUPANT_TYPE_DRIVER);
    }

    @Test
    public void onLifecycleChanged_carServiceReady_zoneInfoPassenger() {
        when(mCar.getCarManager(Car.CAR_OCCUPANT_ZONE_SERVICE)).thenReturn(mCarOccupantZoneManager);
        when(mCarOccupantZoneManager.getMyOccupantZone()).thenReturn(mZoneInfoPassenger);
        mCarServiceLifecycleListener.onLifecycleChanged(mCar, true);

        assertThat(mCarSettingsApplication.getMyOccupantZoneType()).isEqualTo(
                CarOccupantZoneManager.OCCUPANT_TYPE_FRONT_PASSENGER);
    }

    @Test
    public void onLifecycleChanged_carServiceReady_zoneInfoNull() {
        when(mCar.getCarManager(Car.CAR_OCCUPANT_ZONE_SERVICE)).thenReturn(mCarOccupantZoneManager);
        when(mCarOccupantZoneManager.getMyOccupantZone()).thenReturn(null);
        mCarServiceLifecycleListener.onLifecycleChanged(mCar, true);

        assertThat(mCarSettingsApplication.getMyOccupantZoneType()).isEqualTo(
                CarOccupantZoneManager.OCCUPANT_TYPE_INVALID);
    }

    @Test
    public void onLifecycleChanged_carServiceReady_occupantZoneServiceIsNull() {
        when(mCar.getCarManager(Car.CAR_OCCUPANT_ZONE_SERVICE)).thenReturn(null);
        mCarServiceLifecycleListener.onLifecycleChanged(mCar, true);

        assertThat(mCarSettingsApplication.getMyOccupantZoneType()).isEqualTo(
                CarOccupantZoneManager.OCCUPANT_TYPE_INVALID);
    }

    @Test
    public void onLifecycleChanged_carServiceCrashed_usePreviousZoneType() {
        when(mCar.getCarManager(Car.CAR_OCCUPANT_ZONE_SERVICE)).thenReturn(mCarOccupantZoneManager);
        when(mCarOccupantZoneManager.getMyOccupantZone()).thenReturn(mZoneInfoDriver);
        mCarServiceLifecycleListener.onLifecycleChanged(mCar, true);

        assertThat(mCarSettingsApplication.getMyOccupantZoneType()).isEqualTo(
                CarOccupantZoneManager.OCCUPANT_TYPE_DRIVER);

        mCarServiceLifecycleListener.onLifecycleChanged(null, false);

        assertThat(mCarSettingsApplication.getMyOccupantZoneType()).isEqualTo(
                CarOccupantZoneManager.OCCUPANT_TYPE_DRIVER);
    }

    @Test
    public void onOccupantZoneConfigChanged_flagDisplay() {
        when(mCar.getCarManager(Car.CAR_OCCUPANT_ZONE_SERVICE)).thenReturn(mCarOccupantZoneManager);
        when(mCarOccupantZoneManager.getMyOccupantZone()).thenReturn(mZoneInfoDriver);
        mCarServiceLifecycleListener.onLifecycleChanged(mCar, true);
        mConfigChangeListener.onOccupantZoneConfigChanged(
                CarOccupantZoneManager.ZONE_CONFIG_CHANGE_FLAG_DISPLAY);

        assertThat(mCarSettingsApplication.getMyOccupantZoneType()).isEqualTo(
                CarOccupantZoneManager.OCCUPANT_TYPE_DRIVER);
    }

    @Test
    public void onOccupantZoneConfigChanged_flagUser_zoneChanged() {
        when(mCar.getCarManager(Car.CAR_OCCUPANT_ZONE_SERVICE)).thenReturn(mCarOccupantZoneManager);
        when(mCarOccupantZoneManager.getMyOccupantZone()).thenReturn(mZoneInfoDriver);
        mCarServiceLifecycleListener.onLifecycleChanged(mCar, true);

        assertThat(mCarSettingsApplication.getMyOccupantZoneType()).isEqualTo(
                CarOccupantZoneManager.OCCUPANT_TYPE_DRIVER);

        when(mCarOccupantZoneManager.getMyOccupantZone()).thenReturn(mZoneInfoPassenger);
        mConfigChangeListener.onOccupantZoneConfigChanged(
                CarOccupantZoneManager.ZONE_CONFIG_CHANGE_FLAG_USER);

        assertThat(mCarSettingsApplication.getMyOccupantZoneType()).isEqualTo(
                CarOccupantZoneManager.OCCUPANT_TYPE_FRONT_PASSENGER);
    }
}
