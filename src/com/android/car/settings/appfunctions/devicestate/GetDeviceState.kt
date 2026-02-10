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

import android.app.appfunctions.AppFunctionException
import android.app.appfunctions.ExecuteAppFunctionRequest
import android.app.appfunctions.ExecuteAppFunctionResponse
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import com.google.android.appfunctions.schema.common.v1.devicestate.DeviceStateResponse
import com.google.android.appfunctions.schema.common.v1.devicestate.PerScreenDeviceStates
import java.util.Locale

/**
 * Parent class for any app function returning a {@link DeviceStateResponse} which is a List of
 * PerScreenDeviceStates.
 */
abstract class GetDeviceState(id: String, delegate: Delegate) : AppFunctionBase(id, delegate) {

    abstract fun execute(
        request: ExecuteAppFunctionRequest,
        signal: CancellationSignal,
    ): List<PerScreenDeviceStates>

    final override fun onExecuteAppFunction(
        request: ExecuteAppFunctionRequest,
        signal: CancellationSignal,
        callback: OutcomeReceiver<ExecuteAppFunctionResponse?, AppFunctionException?>,
    ) {
        val deviceState =
            DeviceStateResponse(
                perScreenDeviceStates = execute(request, signal),
                deviceLocale = Locale.getDefault().toLanguageTag(),
            )
        val response = buildResponse(deviceState)
        callback.onResult(response)
    }
}
