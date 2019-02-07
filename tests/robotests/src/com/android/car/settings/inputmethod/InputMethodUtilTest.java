/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.car.settings.inputmethod;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ServiceInfo;
import android.graphics.drawable.Drawable;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.view.inputmethod.InputMethodSubtype.InputMethodSubtypeBuilder;

import com.android.car.settings.CarSettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(CarSettingsRobolectricTestRunner.class)
public class InputMethodUtilTest {
    private static final String DUMMY_PACKAGE_NAME = "dummy package name";
    private static final String DUMMY_LABEL = "dummy label";
    private static final String DUMMY_SETTINGS_ACTIVITY = "dummy settings activity";
    private static final String SUBTYPES_STRING =
            "English (United States), German (Belgium), and Occitan (France)";

    private Context mContext;

    @Mock
    private PackageManager mPackageManager;
    @Mock
    private InputMethodManager mInputMethodManager;
    @Mock
    private Drawable mIcon;

    private static InputMethodInfo createMockInputMethodInfoWithSubtypes(
            PackageManager packageManager, InputMethodManager inputMethodManager,
            String packageName) {
        InputMethodInfo mockInfo = createMockInputMethodInfo(packageManager, packageName);
        List<InputMethodSubtype> subtypes = createSubtypes();
        when(inputMethodManager.getEnabledInputMethodSubtypeList(
                eq(mockInfo), anyBoolean())).thenReturn(subtypes);

        return mockInfo;
    }

    private static InputMethodInfo createMockInputMethodInfo(
            PackageManager packageManager, String packageName) {
        InputMethodInfo mockInfo = mock(InputMethodInfo.class);
        when(mockInfo.getPackageName()).thenReturn(packageName);
        when(mockInfo.loadLabel(packageManager)).thenReturn(DUMMY_LABEL);
        when(mockInfo.getServiceInfo()).thenReturn(new ServiceInfo());
        when(mockInfo.getSettingsActivity()).thenReturn(DUMMY_SETTINGS_ACTIVITY);
        return mockInfo;
    }

    private static List<InputMethodSubtype> createSubtypes() {
        List<InputMethodSubtype> subtypes = new ArrayList<>();
        subtypes.add(createSubtype(1, "en_US"));
        subtypes.add(createSubtype(2, "de_BE"));
        subtypes.add(createSubtype(3, "oc-FR"));
        return subtypes;
    }

    private static InputMethodSubtype createSubtype(int id, String locale) {
        return new InputMethodSubtypeBuilder().setSubtypeId(id).setSubtypeLocale(locale)
                .setIsAuxiliary(false).setIsAsciiCapable(true).build();
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
    }

    @Test
    public void testGetPackageIcon_hasApplicationIcon() throws NameNotFoundException {
        InputMethodInfo info = createMockInputMethodInfoWithSubtypes(mPackageManager,
                mInputMethodManager, DUMMY_PACKAGE_NAME);
        when(mPackageManager.getApplicationIcon(eq(info.getPackageName()))).thenReturn(mIcon);
        assertThat(InputMethodUtil.getPackageIcon(mPackageManager, info)).isEqualTo(mIcon);
    }

    @Test
    public void testGetPackageIcon_noApplicationIcon() throws NameNotFoundException {
        InputMethodInfo info = createMockInputMethodInfoWithSubtypes(mPackageManager,
                mInputMethodManager, DUMMY_PACKAGE_NAME);
        when(mPackageManager.getApplicationIcon(DUMMY_PACKAGE_NAME)).thenThrow(
                new NameNotFoundException());
        assertThat(InputMethodUtil.getPackageIcon(mPackageManager, info)).isEqualTo(
                InputMethodUtil.NO_ICON);
    }

    @Test
    public void testGetPackageLabel() {
        InputMethodInfo info = createMockInputMethodInfoWithSubtypes(mPackageManager,
                mInputMethodManager, DUMMY_PACKAGE_NAME);
        assertThat(InputMethodUtil.getPackageLabel(mPackageManager, info)).isEqualTo(
                DUMMY_LABEL);
    }

    @Test
    public void testGetSummaryString() {
        InputMethodInfo info = createMockInputMethodInfoWithSubtypes(mPackageManager,
                mInputMethodManager, DUMMY_PACKAGE_NAME);
        assertThat(InputMethodUtil.getSummaryString(mContext, mInputMethodManager, info)).isEqualTo(
                SUBTYPES_STRING);
    }
}
