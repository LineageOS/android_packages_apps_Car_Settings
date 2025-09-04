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
class BaseAppFunctionAccessPreferenceControllerTest {
    private lateinit var preferenceController: TestAppFunctionAccessPreferenceController
    private val context = spy(ApplicationProvider.getApplicationContext<Context>())
    private val mockFragmentController = mock<FragmentController>()
    private val mockAppFunctionManager = mock<AppFunctionManager>()
    private val mockPreference = mock<Preference>()
    private val testPackageName = "com.android.car.test.package"
    private val testIntentAction = "com.android.car.test.ACTION"

    @get:Rule
    val setFlagRules = SetFlagsRule()

    @Before
    fun setUp() {
        val carUxRestrictions = CarUxRestrictions.Builder(
            true,
            CarUxRestrictions.UX_RESTRICTIONS_BASELINE,
            0
        ).build()
        preferenceController = TestAppFunctionAccessPreferenceController(
            context, "preferenceKey", mockFragmentController, carUxRestrictions,
            mockAppFunctionManager
        ).apply {
            setApplicationInfo(ApplicationInfo().apply {
                packageName = testPackageName
            })
            // Set the value for the protected 'action' property for this test class
            action = testIntentAction
        }
        PreferenceControllerTestUtil.assignPreference(preferenceController, mockPreference)
        doNothing().whenever(context).startActivity(any())
    }

    @Test
    @EnableFlags(
        android.permission.flags.Flags.FLAG_APP_FUNCTION_ACCESS_UI_ENABLED,
        com.android.car.settings.Flags.FLAG_APP_FUNCTION_ACCESS_PERMISSIONS_UI
    )
    fun getAvailabilityStatus_isAvailable() {
        preferenceController.isValidPackageResult = true
        assertThat(preferenceController.availabilityStatus)
            .isEqualTo(PreferenceController.AVAILABLE)
    }

    @Test
    @EnableFlags(
        android.permission.flags.Flags.FLAG_APP_FUNCTION_ACCESS_UI_ENABLED,
        com.android.car.settings.Flags.FLAG_APP_FUNCTION_ACCESS_PERMISSIONS_UI
    )
    fun getAvailabilityStatus_isConditionallyUnavailable_whenPackageIsInvalid() {
        preferenceController.isValidPackageResult = false
        assertThat(preferenceController.availabilityStatus)
            .isEqualTo(PreferenceController.CONDITIONALLY_UNAVAILABLE)
    }

    @Test
    @DisableFlags(android.permission.flags.Flags.FLAG_APP_FUNCTION_ACCESS_UI_ENABLED)
    fun getAvailabilityStatus_isConditionallyUnavailable_whenMissingCoreFlag() {
        preferenceController.isValidPackageResult = true
        assertThat(preferenceController.availabilityStatus)
            .isEqualTo(PreferenceController.CONDITIONALLY_UNAVAILABLE)
    }

    @Test
    @DisableFlags(com.android.car.settings.Flags.FLAG_APP_FUNCTION_ACCESS_PERMISSIONS_UI)
    fun getAvailabilityStatus_isConditionallyUnavailable_whenMissingAutomotiveFlag() {
        preferenceController.isValidPackageResult = true
        assertThat(preferenceController.availabilityStatus)
            .isEqualTo(PreferenceController.CONDITIONALLY_UNAVAILABLE)
    }

    @Test
    fun handlePreferenceClicked_startsActivityWithCorrectIntent() {
        preferenceController.handlePreferenceClicked(mockPreference)

        val intentCaptor = ArgumentCaptor.forClass(Intent::class.java)
        verify(context).startActivity(intentCaptor.capture())

        val intent = intentCaptor.value
        // Verify against the public `intentAction` property
        assertThat(intent.action).isEqualTo(testIntentAction)
        assertThat(intent.getStringExtra(Intent.EXTRA_PACKAGE_NAME)).isEqualTo(testPackageName)
    }

    /**
     * A concrete implementation of the abstract controller for testing purposes.
     * It provides configurable implementations for the abstract members.
     */
    private class TestAppFunctionAccessPreferenceController(
        context: Context,
        preferenceKey: String,
        fragmentController: FragmentController,
        uxRestrictions: CarUxRestrictions,
        appFunctionManager: AppFunctionManager?
    ) : BaseAppFunctionAccessPreferenceController(
        context,
        preferenceKey,
        fragmentController,
        uxRestrictions,
        appFunctionManager
    ) {
        // make protected variable public for testing
        public override var action: String = ""

        var isValidPackageResult: Boolean = false

        override fun isValidPackage(packageName: String): Boolean {
            return isValidPackageResult
        }

        // make protected method public for testing
        public override fun handlePreferenceClicked(preference: Preference?): Boolean {
            return super.handlePreferenceClicked(preference)
        }
    }
}
