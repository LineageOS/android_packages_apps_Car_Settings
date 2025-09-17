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
import android.content.pm.ApplicationInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.car.settings.common.FragmentController
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class TargetAppFunctionAccessPreferenceControllerTest {
    private lateinit var preferenceController: TargetAppFunctionAccessPreferenceController
    private val mockAppFunctionManager = mock<AppFunctionManager>()
    private val testPackageName = "com.android.car.test.package"

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val mockFragmentController = mock<FragmentController>()
        val carUxRestrictions = CarUxRestrictions.Builder(
            true,
            CarUxRestrictions.UX_RESTRICTIONS_BASELINE,
            0
        ).build()

        preferenceController = TargetAppFunctionAccessPreferenceController(
            context, "preferenceKey", mockFragmentController, carUxRestrictions,
            mockAppFunctionManager
        ).apply {
            setApplicationInfo(ApplicationInfo().apply {
                packageName = testPackageName
            })
        }
    }

    @Test
    fun intentAction_isCorrect() {
        assertThat(preferenceController.intentAction)
            .isEqualTo(AppFunctionManager.ACTION_MANAGE_TARGET_APP_FUNCTION_ACCESS)
    }

    @Test
    fun isValidPackage_returnsTrue_whenPackageIsInValidTargets() {
        whenever(mockAppFunctionManager.validTargets).thenReturn(listOf(testPackageName))
        assertThat(preferenceController.isValidPackage(testPackageName)).isTrue()
    }

    @Test
    fun isValidPackage_returnsFalse_whenPackageIsNotInValidTargets() {
        whenever(mockAppFunctionManager.validTargets).thenReturn(emptyList())
        assertThat(preferenceController.isValidPackage(testPackageName)).isFalse()
    }

    @Test
    fun isValidPackage_returnsFalse_whenManagerIsNull() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val mockFragmentController = mock<FragmentController>()
        val carUxRestrictions = CarUxRestrictions.Builder(
            true,
            CarUxRestrictions.UX_RESTRICTIONS_BASELINE,
            0
        ).build()

        val controllerWithNullManager = TargetAppFunctionAccessPreferenceController(
            context,
            "preferenceKey",
            mockFragmentController,
            carUxRestrictions,
            null
        ).apply {
            setApplicationInfo(ApplicationInfo().apply {
                packageName = testPackageName
            })
        }

        assertThat(controllerWithNullManager.isValidPackage(testPackageName)).isFalse()
    }
}
