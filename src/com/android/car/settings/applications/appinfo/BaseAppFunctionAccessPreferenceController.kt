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
package com.android.car.settings.applications.appinfo

import android.app.appfunctions.AppFunctionManager
import android.car.drivingstate.CarUxRestrictions
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.preference.Preference
import com.android.car.settings.Flags
import com.android.car.settings.common.FragmentController
import com.android.car.settings.common.Logger
import com.android.car.settings.common.PreferenceController

/**
 * An abstract base controller for preferences that link to app function access screens.
 *
 * This class handles the shared logic for checking feature flag availability and launching the
 * appropriate settings screen when the preference is clicked. Subclasses are responsible for
 * providing the specific Intent action and package validation logic for their use case
 * (e.g., for AGENT or TARGET apps).
 *
 * @param context The context for accessing system services and resources.
 * @param preferenceKey The key of the preference this controller manages.
 * @param fragmentController The controller for the parent fragment.
 * @param uxRestrictions Determines if the preference is actionable based on driving state.
 * @param appFunctionManager The system service used for package validation.
 */
abstract class BaseAppFunctionAccessPreferenceController(
    context: Context,
    preferenceKey: String,
    fragmentController: FragmentController,
    uxRestrictions: CarUxRestrictions,
    protected val appFunctionManager: AppFunctionManager? =
        context.getSystemService(AppFunctionManager::class.java)
) : PreferenceController<Preference>(context, preferenceKey, fragmentController, uxRestrictions) {

    private lateinit var appInfo: ApplicationInfo

    /**
     * The public-facing, final, read-only Intent action for this controller.
     * Its value is supplied by the subclass via [action].
     */
    val intentAction: String
        get() = action

    /**
     * The Intent action specific to the child class (e.g., AGENT or TARGET).
     */
    protected abstract val action: String

    /** The validation logic specific to the child class. */
    abstract fun isValidPackage(packageName: String): Boolean

    /** Sets the required associate AppInfo of this controller. To be called by the
     * PreferenceFragment during set-up. */
    fun setApplicationInfo(
        applicationInfo: ApplicationInfo
    ): BaseAppFunctionAccessPreferenceController {
        this.appInfo = applicationInfo
        return this
    }

    override fun handlePreferenceClicked(preference: Preference?): Boolean {
        val intent = Intent(intentAction)
            .putExtra(Intent.EXTRA_PACKAGE_NAME, appInfo.packageName)
        LOG.d("Starting app function permission page for: ${appInfo.packageName}")
        context.startActivity(intent)
        return true
    }

    override fun getPreferenceType(): Class<Preference> = Preference::class.java

    override fun getDefaultAvailabilityStatus(): Int {
        if (featureFlagsEnabled() && isValidPackage(appInfo.packageName)) {
            return AVAILABLE
        }
        return CONDITIONALLY_UNAVAILABLE
    }

    private fun featureFlagsEnabled(): Boolean {
        return android.permission.flags.Flags.appFunctionAccessUiEnabled() &&
                Flags.appFunctionAccessPermissionsUi()
    }

    private companion object {
        val LOG = Logger(BaseAppFunctionAccessPreferenceController::class.java)
    }
}
