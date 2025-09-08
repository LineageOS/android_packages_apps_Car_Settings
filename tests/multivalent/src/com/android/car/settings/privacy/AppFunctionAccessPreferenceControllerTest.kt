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
import android.content.pm.PackageManager
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.preference.Preference
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.car.settings.common.FragmentController
import com.android.car.settings.common.PreferenceController
import com.android.car.settings.common.PreferenceControllerTestUtil
import com.google.common.truth.Truth.assertThat
import java.lang.reflect.Method
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class AppFunctionAccessPreferenceControllerTest {
    // Test the real, final class directly.
    private lateinit var preferenceController: AppFunctionAccessPreferenceController
    private val context = spy(ApplicationProvider.getApplicationContext<Context>())
    private val mockFragmentController = mock<FragmentController>()
    private val mockPreference = mock<Preference>()
    private val mockPackageManager = mock<PackageManager>()
    private val mockAppFunctionManager = mock<AppFunctionManager>()
    private val testPermissionControllerPackage = "com.android.permissioncontroller"

    @get:Rule
    val setFlagRules = SetFlagsRule()

    @Before
    fun setUp() {
        val carUxRestrictions = CarUxRestrictions.Builder(
            true,
            CarUxRestrictions.UX_RESTRICTIONS_BASELINE,
            0
        ).build()

        whenever(context.packageManager).thenReturn(mockPackageManager)
        whenever(mockPackageManager.permissionControllerPackageName)
            .thenReturn(testPermissionControllerPackage)

        preferenceController = AppFunctionAccessPreferenceController(
            context,
            "preferenceKey",
            mockFragmentController,
            carUxRestrictions,
            mockAppFunctionManager
        )
        PreferenceControllerTestUtil.assignPreference(preferenceController, mockPreference)
        doNothing().whenever(context).startActivity(any())
    }

    @Test
    @EnableFlags(
        android.permission.flags.Flags.FLAG_APP_FUNCTION_ACCESS_UI_ENABLED,
        com.android.car.settings.Flags.FLAG_APP_FUNCTION_ACCESS_PERMISSIONS_UI
    )
    fun getAvailabilityStatus_isAvailable() {
        assertThat(preferenceController.availabilityStatus)
            .isEqualTo(PreferenceController.AVAILABLE)
    }

    @Test
    @EnableFlags(
        android.permission.flags.Flags.FLAG_APP_FUNCTION_ACCESS_UI_ENABLED,
        com.android.car.settings.Flags.FLAG_APP_FUNCTION_ACCESS_PERMISSIONS_UI
    )
    fun getAvailabilityStatus_isConditionallyUnavailable_whenManagerIsNull() {
        val controllerWithNullManager = AppFunctionAccessPreferenceController(
            context,
            "preferenceKey",
            mockFragmentController,
            CarUxRestrictions.Builder(true, CarUxRestrictions.UX_RESTRICTIONS_BASELINE, 0).build(),
            /* appFunctionManager= */
            null
        )
        assertThat(controllerWithNullManager.availabilityStatus)
            .isEqualTo(PreferenceController.CONDITIONALLY_UNAVAILABLE)
    }

    @Test
    @DisableFlags(android.permission.flags.Flags.FLAG_APP_FUNCTION_ACCESS_UI_ENABLED)
    fun getAvailabilityStatus_isConditionallyUnavailable_whenMissingCoreFlag() {
        assertThat(preferenceController.availabilityStatus)
            .isEqualTo(PreferenceController.CONDITIONALLY_UNAVAILABLE)
    }

    @Test
    @DisableFlags(com.android.car.settings.Flags.FLAG_APP_FUNCTION_ACCESS_PERMISSIONS_UI)
    fun getAvailabilityStatus_isConditionallyUnavailable_whenMissingAutomotiveFlag() {
        assertThat(preferenceController.availabilityStatus)
            .isEqualTo(PreferenceController.CONDITIONALLY_UNAVAILABLE)
    }

    /**
     * Tests the protected [PreferenceController.handlePreferenceClicked] method using reflection.
     *
     * This is a deliberate trade-off to keep the production class `final` (a Kotlin best
     * practice) at the cost of a slightly more brittle test. Making the class `open` solely for
     * testing is considered an anti-pattern.
     */
    @Test
    fun handlePreferenceClicked_startsActivityWithCorrectIntent() {
        // Use reflection to access the protected method on the final class.
        val method: Method = preferenceController::class.java.getDeclaredMethod(
            "handlePreferenceClicked",
            Preference::class.java
        )
        method.isAccessible = true
        method.invoke(preferenceController, mockPreference)

        val intentCaptor = ArgumentCaptor.forClass(Intent::class.java)
        verify(context).startActivity(intentCaptor.capture())

        val intent = intentCaptor.value
        assertThat(intent.action).isEqualTo(AppFunctionManager.ACTION_MANAGE_APP_FUNCTION_ACCESS)
        assertThat(intent.getPackage()).isEqualTo(testPermissionControllerPackage)
    }
}
