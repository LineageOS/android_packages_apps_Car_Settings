/*
 * Copyright (C) 2025 The Android Open Source Project
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
package com.android.car.settings.applications.appinfo

import android.app.appfunctions.AppFunctionManager
import android.car.drivingstate.CarUxRestrictions
import android.content.Context
import com.android.car.settings.common.FragmentController

/**
 * Manages a preference that links to the app function access screen for a specific target app.
 *
 * The preference's visibility is conditional. It will only be shown if the necessary feature
 * flags are enabled and the application is recognized as a valid target by the
 * [AppFunctionManager].
 *
 * @param context The context to use for system services and launching activities.
 * @param preferenceKey The key of the preference this controller manages.
 * @param fragmentController The controller for the parent fragment.
 * @param uxRestrictions Determines if the preference is actionable based on driving state.
 * @param appFunctionManager The system service used to validate target applications.
 */
class TargetAppFunctionAccessPreferenceController @JvmOverloads constructor(
    context: Context,
    preferenceKey: String,
    fragmentController: FragmentController,
    uxRestrictions: CarUxRestrictions,
    appFunctionManager: AppFunctionManager? =
        context.getSystemService(AppFunctionManager::class.java)
) : BaseAppFunctionAccessPreferenceController(
    context,
    preferenceKey,
    fragmentController,
    uxRestrictions,
    appFunctionManager
) {
    override val action: String = AppFunctionManager.ACTION_MANAGE_TARGET_APP_FUNCTION_ACCESS

    override fun isValidPackage(packageName: String): Boolean {
        return appFunctionManager?.validTargets?.contains(packageName) ?: false
    }
}
