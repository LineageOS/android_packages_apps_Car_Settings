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

package com.android.car.settings.testutils;

import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.car.widget.ActionListItem;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

/**
 * Allows access to the internals of an {@link ActionListItem} for testing.
 */
@Implements(ActionListItem.class)
public class ShadowActionListItem {
    private CharSequence mTitle;
    private Drawable mIcon;
    private CharSequence mPrimaryActionText;
    private View.OnClickListener mPrimaryActionOnClickListener;

    @Implementation
    protected void setTitle(CharSequence title) {
        mTitle = title;
    }

    @Implementation
    protected void setPrimaryActionIcon(Drawable icon, int size) {
        mIcon = icon;
    }

    @Implementation
    protected void setAction(String text, boolean showDivider, View.OnClickListener listener) {
        mPrimaryActionText = text;
        mPrimaryActionOnClickListener = listener;
    }

    /**
     * Returns the title set on this item.
     */
    public CharSequence getTitle() {
        return mTitle;
    }

    /**
     * Returns the icon set on this item.
     */
    public Drawable getPrimaryActionIcon() {
        return mIcon;
    }

    /**
     * Returns the primary action text set on this item.
     */
    public CharSequence getPrimaryActionText() {
        return mPrimaryActionText;
    }

    /**
     * Returns the onclick listener for the first action.
     */
    public View.OnClickListener getPrimaryActionOnClickListener() {
        return mPrimaryActionOnClickListener;
    }
}
