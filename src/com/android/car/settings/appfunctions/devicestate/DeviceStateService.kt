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

import android.app.appfunctions.AppFunction
import android.app.appfunctions.AppFunctionException
import android.app.appfunctions.AppFunctionException.ERROR_FUNCTION_NOT_FOUND
import android.app.appfunctions.AppFunctionService
import android.app.appfunctions.ExecuteAppFunctionRequest
import android.app.appfunctions.ExecuteAppFunctionResponse
import android.content.Context
import android.content.pm.SigningInfo
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import com.android.car.settings.common.Logger
import kotlinx.coroutines.runBlocking

interface IdentifiableAppFunction : AppFunction {
    fun getId(): String
}

/** Service that handles all AppFunction execution in CarSettings. */
open class DeviceStateService : AppFunctionService(), Delegate {

    private companion object {
        val LOG = Logger(DeviceStateService::class.java)
    }

    private val appFunctionsMap: Map<String, IdentifiableAppFunction> by lazy {
        getAppFunctions().associateBy { it.getId() }
    }

    override fun getContext(): Context {
        return this
    }

    open fun getAppFunctions(): List<IdentifiableAppFunction> {
        return listOf(GetUncategorizedDeviceState(this))
    }

    override fun onExecuteFunction(
        request: ExecuteAppFunctionRequest,
        callingPackage: String,
        callingPackageSigningInfo: SigningInfo,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<ExecuteAppFunctionResponse, AppFunctionException>,
    ) {
        LOG.d("onExecuteFunction: " + request.functionIdentifier)

        val supportedFun = appFunctionsMap[request.functionIdentifier]
        if (supportedFun == null) {
            callback.onError(
                AppFunctionException(
                    ERROR_FUNCTION_NOT_FOUND,
                    "${request.functionIdentifier} not supported.",
                )
            )
            return
        }
        runBlocking { supportedFun.onExecuteAppFunction(request, cancellationSignal, callback) }
    }
}
