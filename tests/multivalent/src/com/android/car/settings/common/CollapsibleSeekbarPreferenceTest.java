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

package com.android.car.settings.common;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.FrameLayout;

import androidx.preference.PreferenceViewHolder;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.car.settings.R;
import com.android.car.ui.uxr.DrawableStateToggleButton;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class CollapsibleSeekbarPreferenceTest {

    private Context mContext;
    private PreferenceViewHolder mViewHolder;
    private CollapsibleSeekbarPreference mPreference;
    private CollapsibleSeekbarPreference.CollapsibleSeekbarUpdateListener mMockListener;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mMockListener = mock(CollapsibleSeekbarPreference.CollapsibleSeekbarUpdateListener.class);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            View rootView = View.inflate(mContext, R.layout.collapsible_seekbar_preference,
                    /* root= */ null);
            mViewHolder = PreferenceViewHolder.createInstanceForTests(rootView);
            mPreference = new CollapsibleSeekbarPreference(mContext);
            mPreference.setListener(mMockListener);
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
    public void updateState_selected_setsSelectedIcon() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            mPreference.updateState(CollapsibleSeekbarPreference.STATE_SELECTED, true);
            mPreference.onBindViewHolder(mViewHolder);
            assertThat(toBitmap(mPreference.getIcon()).sameAs(
                    toBitmap(mContext.getDrawable(R.drawable.ic_radio_btn_checked)))).isTrue();
        });
    }

    @Test
    public void updateState_unselected_setsUnselectedIcon() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            mPreference.updateState(CollapsibleSeekbarPreference.STATE_UNSELECTED, true);
            mPreference.onBindViewHolder(mViewHolder);
            assertThat(toBitmap(mPreference.getIcon()).sameAs(
                    toBitmap(mContext.getDrawable(R.drawable.ic_radio_btn_unchecked)))).isTrue();
        });
    }

    @Test
    public void updateState_multiSelected_setsMultiSelectedIcon() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            mPreference.updateState(CollapsibleSeekbarPreference.STATE_MULTI_SELECTED, true);
            mPreference.onBindViewHolder(mViewHolder);
            assertThat(toBitmap(mPreference.getIcon()).sameAs(
                    toBitmap(mContext.getDrawable(R.drawable.ic_audio_sharing_primary)))).isTrue();
        });
    }

    @Test
    public void onBindViewHolder_shouldHideSeekbar() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            mPreference.setShowSeekerBar(false);
            mPreference.onBindViewHolder(mViewHolder);
            View seekbarContainer = mViewHolder.findViewById(R.id.seekbar_container);
            assertThat(seekbarContainer.getVisibility()).isEqualTo(View.GONE);
        });
    }

    @Test
    public void onBindViewHolder_shouldShowSeekbar() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            mPreference.setShowSeekerBar(true);
            mPreference.onBindViewHolder(mViewHolder);
            View seekbarContainer = mViewHolder.findViewById(R.id.seekbar_container);
            assertThat(seekbarContainer.getVisibility()).isEqualTo(View.VISIBLE);
        });
    }

    @Test
    public void onBindViewHolder_shouldHideSecondaryAction() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            mPreference.onBindViewHolder(mViewHolder);
            View secondaryAction = mViewHolder.findViewById(
                    R.id.collapsible_seekbar_preference_second_action_container);
            assertThat(secondaryAction.getVisibility()).isEqualTo(View.GONE);
        });
    }

    @Test
    public void onBindViewHolder_primaryActionClicked_notifiesListener() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            mPreference.onBindViewHolder(mViewHolder);
            View primaryAction = mViewHolder.findViewById(android.R.id.icon);
            primaryAction.performClick();
            verify(mMockListener).onSelected(any(), anyInt());
        });
    }

    @Test
    public void onBindViewHolder_secondaryActionClicked_notifiesListener() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            mPreference.setMultiSelectAvailable(true);
            mPreference.setShowActionButton(true);
            mPreference.onBindViewHolder(mViewHolder);
            FrameLayout frameLayout = mViewHolder.itemView.findViewById(
                    R.id.collapsible_seekbar_preference_second_action_item);
            DrawableStateToggleButton button = frameLayout.findViewById(
                    R.id.multi_action_preference_toggle_button);
            button.performClick();
            verify(mMockListener).onMultiSelected(any(), anyInt());
        });
    }
}
