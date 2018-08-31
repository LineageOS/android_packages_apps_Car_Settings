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

package com.android.car.settings.common;

import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.uxrestrictions.CarUxRestrictions;
import androidx.car.widget.ListItem;

import com.android.car.settings.R;

/**
 * Represents text only view of a title and a EditText as input.
 * @param <VH> class that extends {@link ListItem.ViewHolder}.
 */
public class EditTextListItem<VH extends EditTextListItem.ViewHolder>
        extends ListItem<VH> {
    private final String mTitle;
    private final CharSequence mInitialInputText;

    /**
     * Interface that can be implemented to control behavior
     * when the text changes in the EditText.
     */
    public interface TextChangeListener {
        /**
         * Called when the EditText is changed.
         *
         * @param s Contents of EditText
         */
        void textChanged(Editable s);
    }

    private TextChangeListener mTextChangeListener;
    private EditText mEditText;
    protected TextType mTextType = TextType.NONE;
    protected boolean mIsEnabled = true;

    /**
     * Input flags that determine the way the EditText takes input.
     */
    public enum TextType {
        // None editable text
        NONE(0),
        // text input
        TEXT(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL),
        // password, input is replaced by dot
        HIDDEN_PASSWORD(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD),
        // password, visible.
        VISIBLE_PASSWORD(
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);

        private int mValue;

        TextType(int value) {
            mValue = value;
        }

        public int getValue() {
            return mValue;
        }
    }

    public EditTextListItem(String title) {
        this(title, null);
    }

    public EditTextListItem(String title, String initialInputText) {
        mTitle = title;
        mInitialInputText = initialInputText;
    }

    @LayoutRes
    public static int getViewLayoutId() {
        return R.layout.edit_text_list_item;
    }

    /**
     * Creates a {@link EditTextListItem.ViewHolder}.
     */
    public static ViewHolder createViewHolder(View itemView) {
        return new ViewHolder(itemView);
    }

    /**
     * Change the text type for the EditText
     */
    public void setTextType(TextType textType) {
        mTextType = textType;
    }

    protected TextType getTextType() {
        return mTextType;
    }

    /**
     * Change which object receives the TextChange calls
     *
     * @param listener listener to be called on text change
     */
    public void setTextChangeListener(TextChangeListener listener) {
        mTextChangeListener = listener;
    }

    /**
     * Returns the text contained in the EditText
     */
    @Nullable
    public String getInput() {
        return mEditText == null ? null : mEditText.getText().toString();
    }

    @Override
    public int getViewType() {
        return CustomListItemTypes.EDIT_TEXT_VIEW_TYPE;
    }

    @Override
    public void setEnabled(boolean enabled) {
        mIsEnabled = enabled;
    }

    protected boolean isEnabled() {
        return mIsEnabled;
    }

    @Override
    protected void resolveDirtyState() {
        // no-op
    }

    @Override
    protected void onBind(VH viewHolder) {
        viewHolder.titleView.setText(mTitle);
        viewHolder.titleView.setEnabled(mIsEnabled);
        mEditText = viewHolder.editText;
        mEditText.setEnabled(mIsEnabled);
        mEditText.setInputType(mTextType.getValue());
        if (!TextUtils.isEmpty(mInitialInputText)) {
            mEditText.setText(mInitialInputText);
        }
        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // don't care
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // dont' care
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (mTextChangeListener != null) {
                    mTextChangeListener.textChanged(s);
                }
            }
        });
        viewHolder.itemView.setEnabled(mIsEnabled);
    }

    static class ViewHolder extends ListItem.ViewHolder {
        public final TextView titleView;
        public final EditText editText;

        ViewHolder(View view) {
            super(view);
            titleView = view.findViewById(R.id.title);
            editText = view.findViewById(R.id.input);
        }

        @Override
        public void onUxRestrictionsChanged(@NonNull CarUxRestrictions restrictions) {
            // no-op
        }
    }
}
