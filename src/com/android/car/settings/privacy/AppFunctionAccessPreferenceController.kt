/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.car.settings.privacy

import android.app.appfunctions.AppFunctionManager
import android.car.drivingstate.CarUxRestrictions
import android.content.Context
import android.content.Intent
import androidx.preference.Preference
import com.android.car.settings.Flags
import com.android.car.settings.common.FragmentController
import com.android.car.settings.common.Logger
import com.android.car.settings.common.PreferenceController

/**
 * Controller for a preference that links to the top-level App Function Access settings screen.
 *
 * This screen is intended to display a list of all available agent applications, allowing users
 * to manage their permissions from a central location.
 *
 * This preference is only visible if the necessary feature flags are enabled and the
 * [AppFunctionManager] service is available on the system.
 *
 * @param context The context for accessing system services and launching activities.
 * @param preferenceKey The key of the preference this controller manages.
 * @param fragmentController The controller for the parent fragment.
 * @param uxRestrictions Determines if the preference is actionable based on driving state.
 * @param appFunctionManager The system service used to determine availability.
 */
open class AppFunctionAccessPreferenceController @JvmOverloads constructor(
    context: Context,
    preferenceKey: String,
    fragmentController: FragmentController,
    uxRestrictions: CarUxRestrictions,
    private val appFunctionManager: AppFunctionManager? =
        context.getSystemService(AppFunctionManager::class.java)
) : PreferenceController<Preference>(context, preferenceKey, fragmentController, uxRestrictions) {

    override fun getPreferenceType(): Class<Preference> = Preference::class.java

    override fun getDefaultAvailabilityStatus(): Int {
        return if (appFunctionManager != null && featureFlagsEnabled()) {
            AVAILABLE
        } else {
            CONDITIONALLY_UNAVAILABLE
        }
    }

    override fun handlePreferenceClicked(preference: Preference?): Boolean {
        val targetPackage = context.packageManager.permissionControllerPackageName
        val intent = Intent(AppFunctionManager.ACTION_MANAGE_APP_FUNCTION_ACCESS)
            .setPackage(targetPackage)
        LOG.d("Starting manage app function page in: $targetPackage")
        context.startActivity(intent)
        return true
    }

    private fun featureFlagsEnabled(): Boolean {
        return android.permission.flags.Flags.appFunctionAccessUiEnabled() &&
                Flags.appFunctionAccessPermissionsUi()
    }

    private companion object {
        val LOG = Logger(AppFunctionAccessPreferenceController::class.java)
    }
}
