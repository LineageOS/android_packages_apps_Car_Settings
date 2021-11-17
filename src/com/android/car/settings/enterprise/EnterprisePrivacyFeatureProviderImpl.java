/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.car.settings.enterprise;

import android.app.admin.DevicePolicyManager;

import java.util.Date;

/**
 * TODO(b/190867059): copied from phone (but stripped what's not used), should be moved to
 * SettingsLib
 */
final class EnterprisePrivacyFeatureProviderImpl implements EnterprisePrivacyFeatureProvider {

    private final DevicePolicyManager mDpm;

    EnterprisePrivacyFeatureProviderImpl(DevicePolicyManager dpm) {
        mDpm = dpm;
    }

    @Override
    public Date getLastBugReportRequestTime() {
        long timestamp = mDpm.getLastBugReportRequestTime();
        return timestamp < 0 ? null : new Date(timestamp);
    }
}
