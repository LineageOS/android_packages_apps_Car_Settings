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

import androidx.annotation.XmlRes;

import com.android.car.settings.R;
import com.android.car.settings.common.SettingsFragment;
import com.android.settingslib.applications.ApplicationsState;

/**
 * Fragment for the verified links settings page.
 */
public class VerifiedLinksFragment extends SettingsFragment {
    private static final String ARG_PACKAGE_NAME = "arg_package_name";
    private ApplicationsState mState;
    private ApplicationsState.AppEntry mAppEntry;

    /** Creates a new instance of this fragment for the package specified in the arguments. */
    public static VerifiedLinksFragment newInstance(String pkg) {
        VerifiedLinksFragment fragment = new VerifiedLinksFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PACKAGE_NAME, pkg);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    @XmlRes
    protected int getPreferenceScreenResId() {
        return R.xml.verified_links_fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mState = ApplicationsState.getInstance(requireActivity().getApplication());
        String pkgName = getArguments().getString(ARG_PACKAGE_NAME);
        mAppEntry = mState.getEntry(pkgName, UserHandle.myUserId());

        use(VerifiedLinkGroupPreferenceController.class,
                R.string.pk_opening_links_verified_links_list)
                .setAppEntry(mAppEntry);
    }
}
