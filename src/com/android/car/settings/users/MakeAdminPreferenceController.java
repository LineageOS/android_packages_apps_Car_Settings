/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.car.settings.users;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.PreferenceScreen;

import com.android.car.settings.common.ButtonPreference;
import com.android.car.settings.common.FragmentController;

/** Business Logic for preference which promotes a regular user to an admin user. */
public class MakeAdminPreferenceController extends BaseUserDetailsPreferenceController implements
        LifecycleObserver {

    private final ConfirmGrantAdminPermissionsDialog.ConfirmGrantAdminListener mListener = () -> {
        getCarUserManagerHelper().grantAdminPermissions(getUserInfo());
        getFragmentController().goBack();
    };

    public MakeAdminPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController) {
        super(context, preferenceKey, fragmentController);
    }

    /** Ensure that the listener is reset if the dialog was open during a configuration change. */
    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    public void onCreate() {
        ConfirmGrantAdminPermissionsDialog dialog =
                (ConfirmGrantAdminPermissionsDialog) getFragmentController().findDialogByTag(
                        ConfirmGrantAdminPermissionsDialog.TAG);
        if (dialog != null) {
            dialog.setConfirmGrantAdminListener(mListener);
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        ButtonPreference preference = (ButtonPreference) screen.findPreference(getPreferenceKey());
        preference.setOnButtonClickListener(pref -> {
            ConfirmGrantAdminPermissionsDialog dialog = new ConfirmGrantAdminPermissionsDialog();
            dialog.setConfirmGrantAdminListener(mListener);
            getFragmentController().showDialog(dialog, ConfirmGrantAdminPermissionsDialog.TAG);
        });

        Drawable icon = new UserIconProvider(getCarUserManagerHelper()).getUserIcon(getUserInfo(),
                mContext);
        preference.setIcon(icon);
    }

}
