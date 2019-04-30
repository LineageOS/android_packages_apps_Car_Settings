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

package com.android.car.settings.applications.managedomainurls;

import android.app.Application;
import android.car.drivingstate.CarUxRestrictions;
import android.car.userlib.CarUserManagerHelper;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.IntentFilterVerificationInfo;
import android.content.pm.PackageManager;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.IconDrawableFactory;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.Lifecycle;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;

import com.android.car.settings.R;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceController;
import com.android.settingslib.applications.ApplicationsState;

import java.util.ArrayList;
import java.util.List;

/** Business logic to populate the list of apps that deal with domain urls. */
public class DomainAppPreferenceController extends PreferenceController<PreferenceGroup> {

    private final ApplicationsState mApplicationsState;
    private final PackageManager mPm;
    private final CarUserManagerHelper mCarUserManagerHelper;

    @VisibleForTesting
    final ApplicationsState.Callbacks mApplicationStateCallbacks =
            new ApplicationsState.Callbacks() {
                @Override
                public void onRunningStateChanged(boolean running) {
                }

                @Override
                public void onPackageListChanged() {
                }

                @Override
                public void onRebuildComplete(ArrayList<ApplicationsState.AppEntry> apps) {
                    rebuildAppList(apps);
                }

                @Override
                public void onPackageIconChanged() {
                }

                @Override
                public void onPackageSizeChanged(String packageName) {
                }

                @Override
                public void onAllSizesComputed() {
                }

                @Override
                public void onLauncherInfoChanged() {
                }

                @Override
                public void onLoadEntriesCompleted() {
                    mSession.rebuild(ApplicationsState.FILTER_WITH_DOMAIN_URLS,
                            ApplicationsState.ALPHA_COMPARATOR);
                }
            };

    private ApplicationsState.Session mSession;
    private ArrayMap<String, Preference> mPreferenceCache;

    public DomainAppPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
        mApplicationsState = ApplicationsState.getInstance(
                (Application) context.getApplicationContext());
        mPm = context.getPackageManager();
        mCarUserManagerHelper = new CarUserManagerHelper(context);
    }

    @Override
    protected Class<PreferenceGroup> getPreferenceType() {
        return PreferenceGroup.class;
    }

    @Override
    protected void checkInitialized() {
        if (mSession == null) {
            throw new IllegalStateException("session should be non null by this point");
        }
    }

    /** Sets the lifecycle to create a new session. */
    public void setLifecycle(Lifecycle lifecycle) {
        mSession = mApplicationsState.newSession(mApplicationStateCallbacks, lifecycle);
    }

    @Override
    protected void onStartInternal() {
        // Resume the session earlier than the lifecycle so that cached information is updated
        // even if settings is not resumed (for example in multi-display).
        mSession.onResume();
    }

    @Override
    protected void onStopInternal() {
        // Since we resume early in onStart, make sure we clean up even if we don't receive onPause.
        mSession.onPause();
    }

    private void rebuildAppList(ArrayList<ApplicationsState.AppEntry> apps) {
        PreferenceGroup preferenceGroup = getPreference();
        preferenceGroup.removeAll();
        for (int i = 0; i < apps.size(); i++) {
            ApplicationsState.AppEntry entry = apps.get(i);
            preferenceGroup.addPreference(createPreference(entry));
        }
    }

    private Preference createPreference(ApplicationsState.AppEntry entry) {
        String key = entry.info.packageName + "|" + entry.info.uid;
        IconDrawableFactory iconDrawableFactory = IconDrawableFactory.newInstance(getContext());
        Preference preference = new Preference(getContext());
        preference.setKey(key);
        preference.setTitle(entry.label);
        preference.setSummary(getDomainsSummary(entry.info.packageName));
        preference.setIcon(iconDrawableFactory.getBadgedIcon(entry.info));
        preference.setOnPreferenceClickListener(pref -> {
            getFragmentController().launchFragment(
                    ApplicationLaunchSettingsFragment.newInstance(entry.info.packageName));
            return true;
        });
        return preference;
    }

    private CharSequence getDomainsSummary(String packageName) {
        // If the user has explicitly said "no" for this package, that's the
        // string we should show.
        int domainStatus = mPm.getIntentVerificationStatusAsUser(packageName,
                mCarUserManagerHelper.getCurrentProcessUserId());
        if (domainStatus == PackageManager.INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_NEVER) {
            return getContext().getText(R.string.domain_urls_summary_none);
        }
        // Otherwise, ask package manager for the domains for this package,
        // and show the first one (or none if there aren't any).
        ArraySet<String> result = getHandledDomains(mPm, packageName);
        if (result.isEmpty()) {
            return getContext().getText(R.string.domain_urls_summary_none);
        } else if (result.size() == 1) {
            return getContext().getString(R.string.domain_urls_summary_one, result.valueAt(0));
        } else {
            return getContext().getString(R.string.domain_urls_summary_some, result.valueAt(0));
        }
    }

    private ArraySet<String> getHandledDomains(PackageManager pm, String packageName) {
        List<IntentFilterVerificationInfo> iviList = pm.getIntentFilterVerifications(packageName);
        List<IntentFilter> filters = pm.getAllIntentFilters(packageName);

        ArraySet<String> result = new ArraySet<>();
        if (iviList != null && iviList.size() > 0) {
            for (IntentFilterVerificationInfo ivi : iviList) {
                for (String host : ivi.getDomains()) {
                    result.add(host);
                }
            }
        }
        if (filters != null && filters.size() > 0) {
            for (IntentFilter filter : filters) {
                if (filter.hasCategory(Intent.CATEGORY_BROWSABLE)
                        && (filter.hasDataScheme(IntentFilter.SCHEME_HTTP)
                        || filter.hasDataScheme(IntentFilter.SCHEME_HTTPS))) {
                    result.addAll(filter.getHostsList());
                }
            }
        }
        return result;
    }
}
