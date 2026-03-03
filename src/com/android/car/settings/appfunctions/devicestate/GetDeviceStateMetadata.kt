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
import com.android.car.settings.R
import com.google.android.appfunctions.schema.common.v1.devicestate.DeviceStateItemMetadata
import com.google.android.appfunctions.schema.common.v1.devicestate.DeviceStateMetadataResponse
import com.google.android.appfunctions.schema.common.v1.devicestate.PerScreenMetadata
import java.util.Locale

/** Implementation of the getDeviceStateMetadata app function. */
open class GetDeviceStateMetadata(del: Delegate) : AppFunctionBase("getDeviceStateMetadata", del) {

    override fun onExecuteAppFunction(
        request: ExecuteAppFunctionRequest,
        signal: CancellationSignal,
        callback: OutcomeReceiver<ExecuteAppFunctionResponse?, AppFunctionException?>,
    ) {
        val metadata =
            DeviceStateMetadataResponse(
                perScreenMetadata = execute(request, signal),
                deviceLocale = Locale.getDefault().toLanguageTag(),
            )
        val response = buildResponse(metadata)
        callback.onResult(response)
    }

    fun execute(
        request: ExecuteAppFunctionRequest,
        signal: CancellationSignal,
    ): List<PerScreenMetadata> {
        return listOf(buildInvisibleScreen())
    }

    protected fun buildInvisibleScreen(): PerScreenMetadata {
        return PerScreenMetadata(
            id = getString(R.string.invisible_screen_id),
            description = getString(R.string.invisible_screen_hint),
            intentUri = null,
            deviceStateItemsMetadata = listOf(drivingStateMeta()),

        )
    }

    protected fun drivingStateMeta(): DeviceStateItemMetadata {
        val res = delegate.getContext().resources
        return deviceStateItemMetadata(
            keyResId = R.string.invisible_screen_driving_state_key,
            purposeResId = R.string.invisible_screen_driving_state_purpose,
            nameResId = R.string.invisible_screen_driving_state_name,
            sensitiveResId = null,
            writable = null,
            possibleValues = res.getString(
                R.string.invisible_screen_driving_state_values,
                res.getString(R.string.invisible_screen_driving_state_driving),
                res.getString(R.string.invisible_screen_driving_state_parked)
            ),
            R.string.invisible_screen_driving_state_hint,
        )
    }

    protected fun deviceStateItemMetadata(
        keyResId: Int,
        purposeResId: Int,
        nameResId: Int,
        sensitiveResId: Int?,
        writable: Boolean?,
        possibleValues: String?,
        hintResId: Int,
    ): DeviceStateItemMetadata {
        val resources = delegate.getContext().resources
        return DeviceStateItemMetadata(
            key = resources.getString(keyResId),
            purpose = resources.getString(purposeResId),
            name = localizedString(nameResId),
            sensitivity = if (sensitiveResId != null) resources.getString(sensitiveResId) else null,
            writable = writable,
            possibleValues = possibleValues,
            hintText = resources.getString(hintResId),
        )
    }
}
