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

import android.annotation.NonNull;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.verify.domain.DomainVerificationManager;
import android.content.pm.verify.domain.DomainVerificationUserState;

import androidx.annotation.Nullable;

import com.android.car.settings.common.Logger;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Manages domain verifications.
 */
public class CarDomainVerificationManager {
    private static final Logger LOG = new Logger(CarDomainVerificationManager.class);
    private DomainVerificationManager mDomainVerificationManager;
    private DomainVerificationStateChangeListener mStateChangeListener;

    /**
     * A listener for the {@link DomainVerificationUserState} change.
     */
    public interface DomainVerificationStateChangeListener {
        /**
         * Called when {@link DomainVerificationUserState} changes.
         */
        void onDomainVerificationStateChanged();
    }

    /**
     * The constructor for this class.
     */
    public CarDomainVerificationManager(Context context) {
        mDomainVerificationManager = context.getSystemService(DomainVerificationManager.class);
    }

    /**
     * Returns {@link DomainVerificationManager}
     */
    public DomainVerificationManager getDomainVerificationManager() {
        return mDomainVerificationManager;
    }

    /**
     * Registers a listener.
     */
    public void addListener(DomainVerificationStateChangeListener listener) {
        mStateChangeListener = listener;
    }

    /**
     * Returns the {@link DomainVerificationUserState} for a package.
     */
    @Nullable
    public DomainVerificationUserState getDomainVerificationUserState(String pkg) {
        if (mDomainVerificationManager == null) {
            return null;
        }
        DomainVerificationUserState domainVerificationUserState = null;
        if (pkg != null) {
            try {
                domainVerificationUserState =
                        getDomainVerificationManager().getDomainVerificationUserState(pkg);
            } catch (PackageManager.NameNotFoundException e) {
                LOG.e("Fail to get DomainVerificationUserState. The package name is not found: "
                        + e);
            }
        }
        return domainVerificationUserState;
    }

    /**
     * Set whether link handling is enabled.
     */
    public void setDomainVerificationLinkHandlingAllowed(@NonNull String pkg, boolean allowed) {
        if (mDomainVerificationManager == null) {
            return;
        }
        try {
            mDomainVerificationManager.setDomainVerificationLinkHandlingAllowed(pkg, allowed);
            mStateChangeListener.onDomainVerificationStateChanged();
        } catch (PackageManager.NameNotFoundException e) {
            LOG.e("There is an exception that the package name cannot be found: " + e);
        }
    }

    /**
     * Gets the links list by {@link DomainVerificationUserState.DomainState}
     *
     * @return A links list.
     */
    public List<String> getLinksList(String pkgName,
            @DomainVerificationUserState.DomainState int state) {
        if (mDomainVerificationManager == null) {
            return Collections.emptyList();
        }
        DomainVerificationUserState userState = getDomainVerificationUserState(pkgName);
        if (userState == null) {
            return Collections.emptyList();
        }
        return userState.getHostToStateMap()
                .entrySet()
                .stream()
                .filter(it -> it.getValue() == state)
                .map(it -> it.getKey())
                .collect(Collectors.toList());
    }

    /**
     * Update the recorded user selection for the given domains for the given domainSet
     */
    public void setDomainVerificationUserSelection(String pkg, Set<String> domainSet,
            boolean isEnabled) {
        if (mDomainVerificationManager == null) {
            return;
        }
        DomainVerificationUserState userState = getDomainVerificationUserState(pkg);
        if (userState == null) {
            return;
        }
        try {
            mDomainVerificationManager.setDomainVerificationUserSelection(userState.getIdentifier(),
                    domainSet, isEnabled);
        } catch (PackageManager.NameNotFoundException e) {
            LOG.e("There is an exception that the package name cannot be found: " + e);
        }
    }
}
