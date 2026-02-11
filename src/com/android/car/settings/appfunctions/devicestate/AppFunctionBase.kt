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

import android.app.appfunctions.ExecuteAppFunctionResponse
import android.app.appsearch.GenericDocument
import android.content.Context
import android.content.res.Configuration
import com.android.car.settings.appfunctions.GenericDocumentToPlatformConverter
import com.google.android.appfunctions.schema.common.v1.devicestate.DeviceStateItem
import com.google.android.appfunctions.schema.common.v1.devicestate.LocalizedString
import java.util.Locale

interface Delegate {
    fun getContext(): Context
}

/**
 *     Base class for the app functions implementations.
 *     Subclasses
 *       - GetDeviceState for app functions returning a DeviceStateResponse
 *              getUncategorizedDeviceState
 *              getStorageDeviceState
 *              getBatteryDeviceState
 *              getMobileDataUsageDeviceState
 *              getPermissionsDeviceState
 *              getNotificationsDeviceState
 *              getWellbeingDeviceState
 *              getAppsDeviceState
 *       - GetDeviceStateItem for app functions returning a DeviceStateItemResponse
 *              getDeviceStateItem
 *       - GetDeviceStateMetadata for app functions returning a DeviceStateMetadataResponse
 *              getDeviceStateMetadata
 *       - SetDeviceState for app functions returning a SetDeviceStateItemResponse
 *              setDeviceStateItem
 *              adjustNumericDeviceStateItemByPercentage
 *              offsetNumericDeviceStateItemByValue
 */
abstract class AppFunctionBase(private val id: String, val delegate: Delegate) :
    IdentifiableAppFunction {

    protected val englishContext: Context by lazy {
        val context = delegate.getContext()
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(Locale.ENGLISH)
        return@lazy context.createConfigurationContext(configuration)
    }

    override fun getId(): String {
        return id
    }

    protected fun buildResponse(responseData: Any): ExecuteAppFunctionResponse {
        val jetpackDoc = androidx.appsearch.app.GenericDocument.fromDocumentClass(responseData)
        val platformDoc = GenericDocumentToPlatformConverter.toPlatformGenericDocument(jetpackDoc)
        val result =
            GenericDocument.Builder<GenericDocument.Builder<*>>("", "", "")
                .setPropertyDocument(ExecuteAppFunctionResponse.PROPERTY_RETURN_VALUE, platformDoc)
                .build()
        return ExecuteAppFunctionResponse(result)
    }

    protected fun localizedString(resId: Int): LocalizedString {
        return LocalizedString(
            english = englishContext.resources.getString(resId),
            localized = delegate.getContext().resources.getString(resId),
        )
    }

    protected fun getString(resId: Int): String {
        return delegate.getContext().resources.getString(resId)
    }

    protected fun deviceStateItem(
        keyResId: Int,
        purposeResId: Int,
        nameResId: Int,
        hintResId: Int,
        jsonValue: String,
    ): DeviceStateItem {
        val resources = delegate.getContext().resources
        return DeviceStateItem(
            key = resources.getString(keyResId),
            purpose = resources.getString(purposeResId),
            name = localizedString(nameResId),
            jsonValue = jsonValue,
            hintText = resources.getString(hintResId),
        )
    }
}
