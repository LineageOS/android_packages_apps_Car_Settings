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

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.android.settingslib.inputmethod.InputMethodAndSubtypeUtil;

import java.util.List;

/** Keyboard utility class. */
public final class InputMethodUtil {
    @VisibleForTesting
    static final Drawable NO_ICON = new ColorDrawable(Color.TRANSPARENT);

    /** Returns package icon. */
    public static Drawable getPackageIcon(@NonNull PackageManager packageManager,
            @NonNull InputMethodInfo inputMethodInfo) {
        Drawable icon;
        try {
            icon = packageManager.getApplicationIcon(inputMethodInfo.getPackageName());
        } catch (NameNotFoundException e) {
            icon = NO_ICON;
        }

        return icon;
    }

    /** Returns package label. */
    public static String getPackageLabel(@NonNull PackageManager packageManager,
            @NonNull InputMethodInfo inputMethodInfo) {
        return inputMethodInfo.loadLabel(packageManager).toString();
    }

    /** Returns input method summary. */
    public static String getSummaryString(@NonNull Context context,
            @NonNull InputMethodManager inputMethodManager,
            @NonNull InputMethodInfo inputMethodInfo) {
        List<InputMethodSubtype> subtypes =
                inputMethodManager.getEnabledInputMethodSubtypeList(
                        inputMethodInfo, /* allowsImplicitlySelectedSubtypes= */ true);
        return InputMethodAndSubtypeUtil.getSubtypeLocaleNameListAsSentence(
                subtypes, context, inputMethodInfo);
    }

    private InputMethodUtil() {}
}
