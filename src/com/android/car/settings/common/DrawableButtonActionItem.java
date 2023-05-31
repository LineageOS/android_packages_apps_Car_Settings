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

package com.android.car.settings.common;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import com.android.car.settings.R;
import com.android.car.ui.uxr.DrawableStateButton;

/**
 * Handles DrawableButton action item logic
 */
public final class DrawableButtonActionItem extends BaseActionItem {
    private boolean mIsChecked = true;
    @Nullable
    private Runnable mOnClickListener;

    @Nullable
    private Drawable mDrawable;

    @Nullable
    private CharSequence mText;

    private boolean mShowLoadingAnimation = false;

    public DrawableButtonActionItem(ActionItemInfoChangeListener actionItemInfoChangeListener) {
        super(actionItemInfoChangeListener);
    }

    /**
     * Create and setup views.
     *
     * @param frameLayout ViewGroup to attach views to
     */
    @Override
    public void bindViewHolder(FrameLayout frameLayout) {
        // Required to be effectively final for inner class access
        final FrameLayout buttonContainer = getOptionalButtonContainer(frameLayout);

        final View progressIndicator = buttonContainer.findViewById(
                R.id.multi_action_preference_drawable_button_loading_indicator);
        progressIndicator.setVisibility(mShowLoadingAnimation ? View.VISIBLE : View.GONE);

        final DrawableStateButton button = (DrawableStateButton) buttonContainer.findViewById(
                R.id.multi_action_preference_drawable_button);
        button.setOnClickListener(v -> onClick());
        button.setText(mText);
        button.setBackground(mDrawable);
        button.setVisibility(isVisible() ? View.VISIBLE : View.GONE);
        button.setEnabled(isEnabled());
        button.setAllowClickWhenDisabled(true);
    }

    private FrameLayout getOptionalButtonContainer(FrameLayout frameLayout) {
        FrameLayout buttonContainer =
                frameLayout.findViewById(R.id.multi_action_preference_drawable_button_container);

        if (buttonContainer == null) {
            buttonContainer = createView(frameLayout.getContext(), frameLayout);
            frameLayout.addView(buttonContainer);
        }
        return buttonContainer;
    }

    @Override
    public int getLayoutResource() {
        return R.layout.multi_action_preference_drawable_button;
    }

    /**
     * Shows loading wheel animation on the button. Make sure to call it in
     * conjunction with {@link #setText(CharSequence)} otherwise text and
     * animation will overlap.
     *
     * @param show whether to show or hide loading animation wheel
     */
    public void showLoadingAnimation(boolean show) {
        this.mShowLoadingAnimation = show;
        update();
    }

    /**
     * Set the text that is shown on the button.
     */
    public void setText(CharSequence text) {
        mText = text;
        update();
    }

    /**
     * Get the text that is shown on the button.
     */
    public CharSequence getText() {
        return mText;
    }

    /**
     * Get the Runnable that should run when button is clicked.
     */
    public Runnable getOnClickListener() {
        return mOnClickListener;
    }

    /**
     * Set the Runnable that should run when button is clicked.
     */
    public void setOnClickListener(Runnable onClickListener) {
        if (onClickListener != mOnClickListener) {
            mOnClickListener = onClickListener;
            update();
        }
    }

    /**
     * Get the button drawable.
     */
    public Drawable getDrawable() {
        return mDrawable;
    }

    /**
     * Set the button drawable.
     */
    public void setDrawable(Drawable drawable) {
        if (drawable != mDrawable) {
            mDrawable = drawable;
            update();
        }
    }

    /**
     * Set the button drawable.
     */
    public void setDrawable(Context context, @DrawableRes int iconResId) {
        Drawable drawable = context.getDrawable(iconResId);

        if (drawable != mDrawable) {
            mDrawable = drawable;
            update();
        }
    }

    /**
     * Executes when button is clicked.
     */
    public void onClick() {
        if (mIsRestricted && mPreference != null
                && mRestrictedOnClickListener != null) {
            mRestrictedOnClickListener.accept(mPreference);
        } else if (isEnabled() && !mIsRestricted && mOnClickListener != null) {
            mIsChecked = !mIsChecked;
            mOnClickListener.run();
        }
    }

    private FrameLayout createView(Context context, ViewGroup viewGroup) {
        LayoutInflater layoutInflater = LayoutInflater.from(context);

        return (FrameLayout) layoutInflater
                .inflate(getLayoutResource(), viewGroup, false);
    }
}
