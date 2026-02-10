/*
 * Copyright 2026 The Android Open Source Project
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
package com.android.car.settings.appfunctions.devicestate

import android.app.appfunctions.ExecuteAppFunctionRequest
import android.os.CancellationSignal
import com.android.car.settings.R
import com.google.android.appfunctions.schema.common.v1.devicestate.DeviceStateItem
import com.google.android.appfunctions.schema.common.v1.devicestate.PerScreenDeviceStates

/** Implementation of the getUncategorizedDeviceState app function. */
open class GetUncategorizedDeviceState(delegate: Delegate) :
    GetDeviceState("getUncategorizedDeviceState", delegate) {

    override fun execute(
        request: ExecuteAppFunctionRequest,
        signal: CancellationSignal,
    ): List<PerScreenDeviceStates> {
        return listOf(buildInvisibleScreen())
    }

    protected fun buildInvisibleScreen(): PerScreenDeviceStates {
        return PerScreenDeviceStates(
            id = getString(R.string.invisible_screen_id),
            description = getString(R.string.invisible_screen_hint),
            intentUri = null,
            deviceStateItems = listOf(drivingState()),
        )
    }

    protected fun drivingState(): DeviceStateItem {
        val isDriving = true // TODO: get the actual state.
        val stateResId =
            if (isDriving) {
                R.string.invisible_screen_driving_state_driving
            } else {
                R.string.invisible_screen_driving_state_parked
            }
        return deviceStateItem(
            R.string.invisible_screen_driving_state_key,
            R.string.invisible_screen_driving_state_purpose,
            R.string.invisible_screen_driving_state_name,
            R.string.invisible_screen_driving_state_hint,
            getString(stateResId),
        )
    }
}
