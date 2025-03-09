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

package com.android.car.settings.common;

import android.car.Car;
import android.car.VehicleAreaType;
import android.car.VehiclePropertyIds;
import android.car.hardware.CarPropertyValue;
import android.car.hardware.property.CarPropertyManager;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.ArrayRes;

import com.android.car.settings.R;

import java.util.Set;

/**
 * Contains utility functions for build info.
 */
// TODO(b/371116800): move this class to car-apps-common library
public class BuildInfoUtil {

    private BuildInfoUtil() {
    }

    /**
     * Returns true for builds that are for testing for developers.
     */
    public static boolean isDevTesting(Context context) {
        return (Build.IS_ENG || Build.IS_USERDEBUG) && isSupportedBenchOrEmulator(context);
    }

    /**
     * Returns true for builds that are on benches or emulators.
     */
    public static boolean isSupportedBenchOrEmulator(Context context) {
        return isEmulator() || isSupportedDebugDevice(context) || isSupportedDebugDeviceExcludeCar(
                context);
    }

    private static boolean isEmulator() {
        return Build.IS_EMULATOR;
    }

    private static boolean isSupportedDebugDevice(Context context) {
        return isDebugDeviceIncluded(context, R.array.config_debug_support_devices);
    }

    private static boolean isDebugDeviceIncluded(Context context, @ArrayRes int resId) {
        Set<String> supportedDevices = Set.of(context.getResources().getStringArray(resId));
        return supportedDevices.contains(Build.DEVICE);
    }

    private static boolean isSupportedDebugDeviceExcludeCar(Context context) {
        return isDebugDeviceIncluded(context, R.array.config_debug_support_devices_exclude_car)
                && !isRealCar(context);

    }

    /**
     * Please make sure the VIN numbers on the benches are reset before using this function,
     * follow the instructions in b/267517048.
     */
    private static boolean isRealCar(Context context) {
        Car car = Car.createCar(context);
        CarPropertyManager carPropertyManager = null;
        if (car != null) {
            carPropertyManager = (CarPropertyManager) car.getCarManager(Car.PROPERTY_SERVICE);
        }
        if (carPropertyManager != null) {
            try {
                CarPropertyValue carPropertyValue = carPropertyManager.getProperty(
                        VehiclePropertyIds.INFO_VIN, VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL);
                if (carPropertyValue != null && carPropertyValue.getPropertyStatus()
                        == CarPropertyValue.STATUS_AVAILABLE) {
                    if (TextUtils.isDigitsOnly((CharSequence) carPropertyValue.getValue())) {
                        return Long.valueOf((String) carPropertyValue.getValue()) != 0;
                    } else {
                        return true;
                    }
                }
            } catch (Exception e) {
                // For the situations where there are exceptions, the status of the device is
                // uncertain, so it will be treated as a real car in order to avoid showing the
                // debug only features.
                return true;
            }
        }
        return false;
    }
}
