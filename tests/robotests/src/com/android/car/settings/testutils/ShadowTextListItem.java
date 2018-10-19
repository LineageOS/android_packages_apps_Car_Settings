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

import androidx.car.widget.TextListItem;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

/**
 * Allows access to the internals of a text list item for testing.
 */
@Implements(TextListItem.class)
public class ShadowTextListItem {
    private Drawable mDrawable;
    private int mSupplementalIconDrawableId;
    private String mTitle;
    private String mBody;
    private View.OnClickListener mOnClickListener;

    @Implementation
    public void setPrimaryActionIcon(Drawable drawable, boolean useLargeIcon) {
        mDrawable = drawable;
    }

    @Implementation
    public void setSupplementalIcon(int iconResId, boolean showDivider) {
        mSupplementalIconDrawableId = iconResId;
    }

    @Implementation
    public void setTitle(String title) {
        mTitle = title;
    }

    @Implementation
    public void setBody(String body) {
        mBody = body;
    }

    @Implementation
    public void setBody(String body, boolean asPrimary) {
        mBody = body;
    }

    @Implementation
    public void setOnClickListener(View.OnClickListener listener) {
        mOnClickListener = listener;
    }

    /**
     * Returns the drawable set on this item.
     */
    public Drawable getDrawable() {
        return mDrawable;
    }

    /**
     * Returns the supplemental drawable id set on this item.
     */
    public int getSupplementalIconDrawableId() {
        return mSupplementalIconDrawableId;
    }

    /**
     * Returns the title set on this item.
     */
    public String getTitle() {
        return mTitle;
    }

    /**
     * Returns the body set on this item.
     */
    public String getBody() {
        return mBody;
    }

    /**
     * Returns the onclick listener for this item.
     */
    public View.OnClickListener getOnClickListener() {
        return mOnClickListener;
    }
}
