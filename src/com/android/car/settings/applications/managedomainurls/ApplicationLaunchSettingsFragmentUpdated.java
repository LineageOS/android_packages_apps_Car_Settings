/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.car.settings.applications.managedomainurls;

import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;

import androidx.annotation.VisibleForTesting;
import androidx.annotation.XmlRes;

import com.android.car.settings.R;
import com.android.car.settings.applications.ApplicationPreferenceController;
import com.android.car.settings.common.PreferenceController;
import com.android.car.settings.common.SettingsFragment;
import com.android.settingslib.applications.ApplicationsState;

import java.util.ArrayList;
import java.util.List;

/** Settings screen to show details about launching a specific app. */
public class ApplicationLaunchSettingsFragmentUpdated extends SettingsFragment implements
        CarDomainVerificationManager.DomainVerificationStateChangeListener {

    @VisibleForTesting
    static final String ARG_PACKAGE_NAME = "arg_package_name";

    private ApplicationsState mState;
    private ApplicationsState.AppEntry mAppEntry;
    private List<PreferenceController> mLinksListControllers = new ArrayList<>();

    /** Creates a new instance of this fragment for the package specified in the arguments. */
    public static ApplicationLaunchSettingsFragmentUpdated newInstance(String pkg) {
        ApplicationLaunchSettingsFragmentUpdated fragment =
                new ApplicationLaunchSettingsFragmentUpdated();
        Bundle args = new Bundle();
        args.putString(ARG_PACKAGE_NAME, pkg);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    @XmlRes
    protected int getPreferenceScreenResId() {
        return R.xml.application_launch_settings_fragment_updated;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mState = ApplicationsState.getInstance(requireActivity().getApplication());

        String pkgName = getArguments().getString(ARG_PACKAGE_NAME);
        mAppEntry = mState.getEntry(pkgName, UserHandle.myUserId());

        use(ApplicationPreferenceController.class,
                R.string.pk_opening_links_app_details)
                .setAppEntry(mAppEntry).setAppState(mState);

        OpenLinksSwitchPreferenceController openLinksSwitchPreferenceController =
                use(OpenLinksSwitchPreferenceController.class,
                        R.string.pk_opening_links_app_toggle);
        openLinksSwitchPreferenceController.setAppEntry(mAppEntry);
        openLinksSwitchPreferenceController.setDomainVerificationStateListener(this);

        VerifiedLinksPreferenceController verifiedLinksPreferenceController =
                use(VerifiedLinksPreferenceController.class,
                        R.string.pk_opening_links_verified_links);
        verifiedLinksPreferenceController.setAppEntry(mAppEntry);
        mLinksListControllers.add(verifiedLinksPreferenceController);

        SelectedLinksListPreferenceController selectedLinksListPreferenceController =
                use(SelectedLinksListPreferenceController.class,
                        R.string.pk_opening_links_supported_links);
        selectedLinksListPreferenceController.setAppEntry(mAppEntry);
        mLinksListControllers.add(selectedLinksListPreferenceController);

        AddLinksListPreferenceController addLinksListPreferenceController =
                use(AddLinksListPreferenceController.class,
                        R.string.pk_opening_links_add_links);
        addLinksListPreferenceController.setAppEntry(mAppEntry);
        mLinksListControllers.add(addLinksListPreferenceController);
    }

    @Override
    public void onDomainVerificationStateChanged() {
        for (PreferenceController controller : mLinksListControllers) {
            controller.refreshUi();
        }
    }
}
