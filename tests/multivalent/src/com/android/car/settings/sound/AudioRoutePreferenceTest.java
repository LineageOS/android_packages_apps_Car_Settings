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

import static android.media.AudioDeviceInfo.TYPE_BUILTIN_SPEAKER;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.media.AudioDeviceAttributes;
import android.view.View;

import androidx.preference.PreferenceViewHolder;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.car.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class AudioRoutePreferenceTest {
    private Context mContext;
    private PreferenceViewHolder mViewHolder;
    private AudioRoutePreference mPreference;
    @Mock
    private AudioDeviceAttributes mAudioDeviceAttributes;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mAudioDeviceAttributes.getName()).thenReturn("Test device name");
        when(mAudioDeviceAttributes.getAddress()).thenReturn("Test address");
        when(mAudioDeviceAttributes.getType()).thenReturn(TYPE_BUILTIN_SPEAKER);

        mContext = ApplicationProvider.getApplicationContext();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            View rootView = View.inflate(mContext, R.layout.collapsible_seekbar_preference,
                    /* root= */ null);
            mViewHolder = PreferenceViewHolder.createInstanceForTests(rootView);
            AudioRouteItem item = new AudioRouteItem.Builder(mAudioDeviceAttributes)
                    .setName("Test Device")
                    .setState(AudioRouteItem.State.UNICAST_READY)
                    .build();
            mPreference = new AudioRoutePreference(mContext, item);
        });
    }

    @Test
    public void updateUi_unicastReady_setsCorrectState() {
        AudioRouteItem item = new AudioRouteItem.Builder(mAudioDeviceAttributes)
                .setName("Test Device")
                .setState(AudioRouteItem.State.UNICAST_READY)
                .build();
        mPreference = new AudioRoutePreference(mContext, item);

        mPreference.onBindViewHolder(mViewHolder);

        assertThat(mPreference.getTitle().toString()).isEqualTo("Test Device");
        assertThat(mPreference.getSummary()).isNull();
        assertThat(toBitmap(mPreference.getIcon()).sameAs(
                toBitmap(mContext.getDrawable(R.drawable.ic_radio_btn_unchecked)))).isTrue();
    }

    @Test
    public void updateUi_unicastActive_setsCorrectState() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            AudioRouteItem item = new AudioRouteItem.Builder(mAudioDeviceAttributes)
                    .setName("Test Device")
                    .setState(AudioRouteItem.State.UNICAST_ACTIVE)
                    .build();
            mPreference = new AudioRoutePreference(mContext, item);

            mPreference.onBindViewHolder(mViewHolder);

            assertThat(mPreference.getTitle().toString()).isEqualTo("Test Device");
            assertThat(mPreference.getSummary().toString()).isEqualTo(
                    mContext.getString(R.string.audio_route_preference_listening));
            assertThat(toBitmap(mPreference.getIcon()).sameAs(
                    toBitmap(mContext.getDrawable(R.drawable.ic_radio_btn_checked)))).isTrue();
        });
    }

    @Test
    public void updateUi_multicastReadyUnicastReady_setsCorrectState() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            AudioRouteItem item = new AudioRouteItem.Builder(mAudioDeviceAttributes)
                    .setName("Test Device")
                    .setState(AudioRouteItem.State.MULTICAST_READY_UNICAST_READY)
                    .build();
            mPreference = new AudioRoutePreference(mContext, item);

            mPreference.onBindViewHolder(mViewHolder);

            assertThat(mPreference.getTitle().toString()).isEqualTo("Test Device");
            assertThat(mPreference.getSummary().toString()).isEqualTo(
                    mContext.getString(R.string.audio_route_preference_available_to_join));
            assertThat(toBitmap(mPreference.getIcon()).sameAs(
                    toBitmap(mContext.getDrawable(R.drawable.ic_radio_btn_unchecked)))).isTrue();
        });
    }

    @Test
    public void updateUi_multicastReadyUnicastActive_setsCorrectState() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            AudioRouteItem item = new AudioRouteItem.Builder(mAudioDeviceAttributes)
                    .setName("Test Device")
                    .setState(AudioRouteItem.State.MULTICAST_READY_UNICAST_ACTIVE)
                    .build();
            mPreference = new AudioRoutePreference(mContext, item);

            mPreference.onBindViewHolder(mViewHolder);

            assertThat(mPreference.getTitle().toString()).isEqualTo("Test Device");
            assertThat(mPreference.getSummary()).isNull();
            assertThat(toBitmap(mPreference.getIcon()).sameAs(
                    toBitmap(mContext.getDrawable(R.drawable.ic_radio_btn_checked)))).isTrue();
        });
    }

    @Test
    public void updateUi_multicastActive_setsCorrectState() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            AudioRouteItem item = new AudioRouteItem.Builder(mAudioDeviceAttributes)
                    .setName("Test Device")
                    .setState(AudioRouteItem.State.MULTICAST_ACTIVE)
                    .build();
            mPreference = new AudioRoutePreference(mContext, item);
            mPreference.onBindViewHolder(mViewHolder);

            assertThat(mPreference.getTitle().toString()).isEqualTo("Test Device");
            assertThat(mPreference.getSummary().toString()).isEqualTo(
                    mContext.getString(R.string.audio_route_preference_listening));
            assertThat(toBitmap(mPreference.getIcon()).sameAs(
                    toBitmap(mContext.getDrawable(R.drawable.ic_audio_sharing_primary)))).isTrue();
        });
    }

    @Test
    public void updateUi_startingUnicast_setsCorrectState() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            AudioRouteItem item = new AudioRouteItem.Builder(mAudioDeviceAttributes)
                    .setName("Test Device")
                    .setState(AudioRouteItem.State.STARTING_UNICAST)
                    .build();
            mPreference = new AudioRoutePreference(mContext, item);
            mPreference.onBindViewHolder(mViewHolder);

            assertThat(mPreference.getTitle().toString()).isEqualTo("Test Device");
            assertThat(mPreference.getSummary().toString()).isEqualTo(
                    mContext.getString(R.string.audio_route_preference_connecting_device));
            assertThat(toBitmap(mPreference.getIcon()).sameAs(
                    toBitmap(mContext.getDrawable(R.drawable.ic_radio_btn_unchecked)))).isTrue();
        });
    }

    @Test
    public void updateUi_startingBroadcast_setsCorrectState() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            AudioRouteItem item = new AudioRouteItem.Builder(mAudioDeviceAttributes)
                    .setName("Test Device")
                    .setState(AudioRouteItem.State.STARTING_BROADCAST)
                    .build();
            mPreference = new AudioRoutePreference(mContext, item);
            mPreference.onBindViewHolder(mViewHolder);

            assertThat(mPreference.getTitle().toString()).isEqualTo("Test Device");
            assertThat(mPreference.getSummary().toString()).isEqualTo(
                    mContext.getString(R.string.audio_route_preference_starting_broadcast));
        });
    }

    @Test
    public void updateUi_joiningBroadcast_setsCorrectState() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            AudioRouteItem item = new AudioRouteItem.Builder(mAudioDeviceAttributes)
                    .setName("Test Device")
                    .setState(AudioRouteItem.State.JOINING_BROADCAST)
                    .build();
            mPreference = new AudioRoutePreference(mContext, item);
            mPreference.onBindViewHolder(mViewHolder);

            assertThat(mPreference.getTitle().toString()).isEqualTo("Test Device");
            assertThat(mPreference.getSummary().toString()).isEqualTo(
                    mContext.getString(R.string.audio_route_preference_connecting_broadcast));
        });
    }

    @Test
    public void updateUi_leavingBroadcast_setsCorrectState() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            AudioRouteItem item = new AudioRouteItem.Builder(mAudioDeviceAttributes)
                    .setName("Test Device")
                    .setState(AudioRouteItem.State.LEAVING_BROADCAST)
                    .build();
            mPreference = new AudioRoutePreference(mContext, item);
            mPreference.onBindViewHolder(mViewHolder);

            assertThat(mPreference.getTitle().toString()).isEqualTo("Test Device");
            assertThat(mPreference.getSummary().toString()).isEqualTo(
                    mContext.getString(R.string.audio_route_preference_leaving_broadcast));
        });
    }

    @Test
    public void updateUi_broadcastActive_setsCorrectState() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            AudioRouteItem item = new AudioRouteItem.Builder(mAudioDeviceAttributes)
                    .setName("Test Device")
                    .setState(AudioRouteItem.State.BROADCAST_ACTIVE)
                    .build();
            mPreference = new AudioRoutePreference(mContext, item);
            mPreference.onBindViewHolder(mViewHolder);

            assertThat(mPreference.getTitle().toString()).isEqualTo("Test Device");
            assertThat(mPreference.getSummary()).isNull();
            assertThat(toBitmap(mPreference.getIcon()).sameAs(
                    toBitmap(mContext.getDrawable(R.drawable.ic_radio_btn_checked)))).isTrue();
        });
    }

    @Test
    public void updateUi_broadcastReady_setsCorrectState() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            AudioRouteItem item = new AudioRouteItem.Builder(mAudioDeviceAttributes)
                    .setName("Test Device")
                    .setState(AudioRouteItem.State.BROADCAST_READY)
                    .build();
            mPreference = new AudioRoutePreference(mContext, item);
            mPreference.onBindViewHolder(mViewHolder);

            assertThat(mPreference.getTitle().toString()).isEqualTo("Test Device");
            assertThat(mPreference.getSummary()).isNull();
            assertThat(toBitmap(mPreference.getIcon()).sameAs(
                    toBitmap(mContext.getDrawable(R.drawable.ic_radio_btn_unchecked)))).isTrue();
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
}
