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

package com.android.car.settings.testutils;

import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import androidx.annotation.Nullable;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;

import java.util.List;

@Implements(value = InputMethodManager.class)
public class ShadowInputMethodManager extends org.robolectric.shadows.ShadowInputMethodManager {
    private static List<InputMethodInfo> sEnabledInputMethodList;
    private static List<InputMethodSubtype> sInputMethodSubtypes;

    public static void setEnabledInputMethodList(@Nullable List<InputMethodInfo> list) {
        sEnabledInputMethodList = list;
    }

    @Implementation
    protected List<InputMethodInfo> getEnabledInputMethodList() {
        return sEnabledInputMethodList;
    }

    public static void setEnabledInputMethodSubtypeList(List<InputMethodSubtype> list) {
        sInputMethodSubtypes = list;
    }

    @Implementation
    protected List<InputMethodSubtype> getEnabledInputMethodSubtypeList(InputMethodInfo imi,
            boolean allowsImplicitlySelectedSubtypes) {
        return sInputMethodSubtypes;
    }

    @Resetter
    public static void reset() {
        sInputMethodSubtypes = null;
        sEnabledInputMethodList = null;
        org.robolectric.shadows.ShadowInputMethodManager.reset();
    }
}
