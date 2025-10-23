/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.car.settings.sound;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.preference.PreferenceViewHolder;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.car.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AudioRoutePreferenceTest {

    private Context mContext;
    private PreferenceViewHolder mViewHolder;
    private AudioRoutePreference mPreference;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            View rootView = View.inflate(mContext, R.layout.collapsible_seekbar_preference,
                    /* root= */ null);
            mViewHolder = PreferenceViewHolder.createInstanceForTests(rootView);
            mPreference = new AudioRoutePreference(mContext);
        });
    }

    private Bitmap toBitmap(Drawable drawable) {
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    @Test
    public void setUnicastActive_leCapable_setsCorrectState() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            mPreference.setUnicastActive(true);
            mPreference.onBindViewHolder(mViewHolder);

            assertThat(mPreference.getSummary().toString()).isEqualTo(
                    mContext.getString(R.string.audio_route_preference_listening));
            assertThat(toBitmap(mPreference.getIcon()).sameAs(
                    toBitmap(mContext.getDrawable(R.drawable.ic_radio_btn_checked)))).isTrue();
        });
    }

    @Test
    public void setUnicastActive_notLeCapable_setsCorrectState() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            mPreference.setUnicastActive(false);
            mPreference.onBindViewHolder(mViewHolder);

            assertThat(mPreference.getSummary().toString()).isEqualTo(
                    mContext.getString(R.string.audio_route_preference_listening));
            assertThat(toBitmap(mPreference.getIcon()).sameAs(
                    toBitmap(mContext.getDrawable(R.drawable.ic_radio_btn_checked)))).isTrue();
        });
    }

    @Test
    public void setUnicastInactive_setsCorrectState() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            mPreference.setUnicastInactive();
            mPreference.onBindViewHolder(mViewHolder);

            assertThat(mPreference.getSummary()).isNull();
            assertThat(toBitmap(mPreference.getIcon()).sameAs(
                    toBitmap(mContext.getDrawable(R.drawable.ic_radio_btn_unchecked)))).isTrue();
        });
    }

    @Test
    public void setMulticastActive_setsCorrectState() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            mPreference.setMulticastActive();
            mPreference.onBindViewHolder(mViewHolder);

            assertThat(mPreference.getSummary().toString()).isEqualTo(
                    mContext.getString(R.string.audio_route_preference_listening));
            assertThat(toBitmap(mPreference.getIcon()).sameAs(
                    toBitmap(mContext.getDrawable(R.drawable.ic_audio_sharing_primary)))).isTrue();
        });
    }

    @Test
    public void setMulticastInactive_setsCorrectState() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            mPreference.setMulticastInactive();
            mPreference.onBindViewHolder(mViewHolder);

            assertThat(mPreference.getSummary().toString()).isEqualTo(
                    mContext.getString(R.string.audio_route_preference_available_to_join));
            assertThat(toBitmap(mPreference.getIcon()).sameAs(
                    toBitmap(mContext.getDrawable(R.drawable.ic_radio_btn_unchecked)))).isTrue();
        });
    }
}
