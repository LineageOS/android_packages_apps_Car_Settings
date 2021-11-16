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

import static com.google.common.truth.Truth.assertWithMessage;

import org.junit.Before;
import org.junit.Test;

import java.util.Date;

public final class EnterprisePrivacyFeatureProviderImplTest extends BaseEnterpriseTestCase {

    private EnterprisePrivacyFeatureProviderImpl mProvider;

    @Before
    public void setProvider() {
        mProvider = new EnterprisePrivacyFeatureProviderImpl(mDpm);
    }

    @Test
    public void testGetLastBugReportRequestTime_none() {
        mockGetLastBugreportTime(-1);

        assertWithMessage("getLastBugReportRequestTime()")
                .that(mProvider.getLastBugReportRequestTime()).isNull();
    }

    @Test
    public void testGetLastBugReportRequestTime_ok() {
        long now = System.currentTimeMillis();
        mockGetLastBugreportTime(now);

        Date last = mProvider.getLastBugReportRequestTime();
        assertWithMessage("getLastBugReportRequestTime()").that(last).isNotNull();
        assertWithMessage("getLastBugReportRequestTime().getTime()").that(last.getTime())
                .isEqualTo(now);
    }

    @Test
    public void testGetLastNetworkLogRetrievalTime_none() {
        mockGetLastNetworkLogRetrievalTime(-1);

        assertWithMessage("getLastNetworkLogRetrievalTime()")
                .that(mProvider.getLastNetworkLogRetrievalTime()).isNull();
    }

    @Test
    public void testGetLastNetworkLogRetrievalTime_ok() {
        long now = System.currentTimeMillis();
        mockGetLastNetworkLogRetrievalTime(now);

        Date last = mProvider.getLastNetworkLogRetrievalTime();
        assertWithMessage("getLastNetworkLogRetrievalTime()").that(last).isNotNull();
        assertWithMessage("getLastNetworkLogRetrievalTime().getTime()").that(last.getTime())
                .isEqualTo(now);
    }

    @Test
    public void testGetLastSecurityLogRetrievalTime_none() {
        mockGetLastSecurityLogRetrievalTime(-1);

        assertWithMessage("getLastSecurityLogRetrievalTime()")
                .that(mProvider.getLastSecurityLogRetrievalTime()).isNull();
    }

    @Test
    public void testGetLastSecurityLogRetrievalTime_ok() {
        long now = System.currentTimeMillis();
        mockGetLastSecurityLogRetrievalTime(now);

        Date last = mProvider.getLastSecurityLogRetrievalTime();
        assertWithMessage("getLastSecurityLogRetrievalTime()").that(last).isNotNull();
        assertWithMessage("getLastSecurityLogRetrievalTime().getTime()").that(last.getTime())
                .isEqualTo(now);
    }
}
