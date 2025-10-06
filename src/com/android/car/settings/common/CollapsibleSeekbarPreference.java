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

import static com.android.car.ui.utils.CarUiUtils.requireViewByRefId;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.CallSuper;
import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceViewHolder;

import com.android.car.settings.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

/**
 * A class for preferences that have a main click, secondary action, and a slider seekbar.
 * For now, this class is only used for audio routing page. If this UI component is reused later,
 * then we can look to generalize it TODO(b/450023259).
 */
public class CollapsibleSeekbarPreference extends SeekBarPreference {
    public static final int STATE_SELECTED = 0;
    public static final int STATE_UNSELECTED = 1;
    public static final int STATE_MULTI_SELECTED = 2;

    protected View mSeekbarContainer;
    final protected ToggleButtonActionItem mActionItem =
            new ToggleButtonActionItem(/* changeListener=*/ null);

    private final List<CollapsibleSeekbarUpdateListener> mListeners = new ArrayList<>();
    private @MultiSelectState int mSelectionState = STATE_UNSELECTED;
    private boolean mSecondaryActionAvailable;
    private boolean mShowActionButton;

    @IntDef(flag = false, open = false, value = {
            STATE_SELECTED,
            STATE_UNSELECTED,
            STATE_MULTI_SELECTED
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface MultiSelectState {}

    public CollapsibleSeekbarPreference(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    public CollapsibleSeekbarPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public CollapsibleSeekbarPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CollapsibleSeekbarPreference(Context context) {
        this(context, null);
    }

    /**
     * Initialize styles and attributes, as well as what types of action items should be created/
     * @param attrs Attribute set from which to read values
     */
    @CallSuper
    protected void init(@Nullable AttributeSet attrs) {
        setLayoutResource(R.layout.collapsible_seekbar_preference);
        updateStateInternal(STATE_UNSELECTED, /* notifyChanged= */ false);
        setSelectable(false);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        // set-up preference icon for primary action
        View radioButton = requireViewByRefId(holder.itemView, android.R.id.icon);
        radioButton.setOnClickListener(
                v -> mListeners.forEach(
                        listener -> listener.onSelected(getKey(), mSelectionState)));

        // set-up preference icon for secondary action
        if (mSecondaryActionAvailable) {
            mActionItem.setDrawable(getContext().getDrawable(mActionItem.isChecked()
                    ? R.drawable.ic_audio_sharing_exit : R.drawable.ic_audio_sharing_join));
            mActionItem.setPreference(this)
                    .setRestrictedOnClickListener(getOnClickWhileRestrictedListener());
            mActionItem.setOnClickListener(isChecked -> {
                mListeners.forEach(listener -> listener.onMultiSelected(getKey(), mSelectionState));
            });
        }

        boolean showActionItem = mSecondaryActionAvailable && mShowActionButton;
        requireViewByRefId(holder.itemView,
                R.id.collapsible_seekbar_preference_second_action_container)
                .setVisibility(showActionItem ? View.VISIBLE : View.GONE);
        mActionItem.bindViewHolder(requireViewByRefId(holder.itemView,
                R.id.collapsible_seekbar_preference_second_action_item));

        // set-up seekbar
        mSeekbarContainer = requireViewByRefId(holder.itemView, R.id.seekbar_container);
        mSeekbarContainer.setVisibility(shouldShowSeekbar() ? View.VISIBLE : View.GONE);
    }

    /**
     * Update selection state, and dispatch animation only when is necessary.
     *
     * @param state new state to update the preference into.
     * @param showActionButton whether or not to show the action button.
     */
    public void updateState(@MultiSelectState int state, boolean showActionButton) {
        boolean changed = showActionButton != mShowActionButton || state != mSelectionState;
        mShowActionButton = showActionButton;
        updateStateInternal(state, /* notifyChanged=*/ changed);
    }

    /**
     * Setting {@code notifyChanged} to {@code true} will set the preference to call parent method
     * {@link androidx.preference.Preference#notifyChanged()} immediately and update the view layout
     * and update its relative location within the {@link androidx.recyclerview.widget.RecyclerView}
     *
     * TODO(b/450023259) icons are not imported using stylables and are not made to be
     * updatable via method because it is only used for multicasting.
     *
     * @param selectionState sets the targeted UI state to render at the next convenient time.
     * @param notifyChanged whether or not the UI state should be immediately updated.
     */
    private void updateStateInternal(@MultiSelectState int selectionState, boolean notifyChanged) {
        mSelectionState = selectionState;

        switch (mSelectionState) {
            case STATE_SELECTED:
                setIcon(R.drawable.ic_radio_btn_checked);
                mActionItem.setChecked(false);
                break;
            case STATE_UNSELECTED:
                setIcon(R.drawable.ic_radio_btn_unchecked);
                mActionItem.setChecked(false);
                mActionItem.setDrawable(getContext().getDrawable(R.drawable.ic_audio_sharing_join));
                break;
            case STATE_MULTI_SELECTED:
                setIcon(R.drawable.ic_audio_sharing_primary);
                mActionItem.setChecked(true);
                mActionItem.setDrawable(getContext().getDrawable(R.drawable.ic_audio_sharing_exit));
                break;
        }

        if (mSeekbarContainer != null) {
            mSeekbarContainer.setVisibility(shouldShowSeekbar() ? View.VISIBLE : View.GONE);
        }

        if (notifyChanged) {
            notifyChanged();
        }
    }

    /**
     * @return true if seekbar should be shown in the UI, false otherwise.
     */
    public boolean shouldShowSeekbar() {
        // TODO: impl this method after supporting volume control profile
        return false;
    }

    /**
     * This button sets the overall enabled state of the multi-select button. If set to {@code true}
     * then the {@link CollapsibleSeekbarUpdateListener} should be notified.
     * @param multiSelectable {@code true} if the action item should be enabled.
     */
    public void setMultiSelectAvailable(boolean multiSelectable) {
        mSecondaryActionAvailable = multiSelectable;
    }

    /**
     * Show the UI for Action button. Rather than setting whether or not the secondary action is
     * available, the variable only controls whether that action is shown.
     * This is useful for when a secondary action is temporarily unavailable and may need to be
     * turned on/off later.
     * @param showActionButton {@code true} if the action button should be shown in the UI.
     */
    public void setShowActionButton(boolean showActionButton) {
        mShowActionButton = showActionButton;
    }

    /**
     * @param listener listener for primary and secondary button interactions.
     */
    public void setListener(CollapsibleSeekbarUpdateListener listener) {
        mListeners.add(listener);
    }

    /**
     * Listener interface to be implemented by consumers of UI state change.
     */
    public interface CollapsibleSeekbarUpdateListener {
        void onSelected(String key, @MultiSelectState int currentState);
        void onMultiSelected(String key, @MultiSelectState int currentState);
    }
}
