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

import android.view.View;
import android.widget.CheckBox;

import androidx.annotation.LayoutRes;

import com.android.car.settings.R;

/**
 * Contains logic for a list item that represents a text only view of a title and an EditText
 * password input followed by a checkbox to toggle between show/hide password.
 */
public class PasswordListItem extends EditTextListItem<PasswordListItem.ViewHolder> {
    private boolean mShowPassword;

    public PasswordListItem(String title) {
        super(title, /* initialInputText= */ null);
    }

    @LayoutRes
    public static final int getViewLayoutId() {
        return R.layout.password_list_item;
    }

    /**
     * Creates a {@link PasswordListItem.ViewHolder}.
     */
    public static ViewHolder createViewHolder(View itemView) {
        return new ViewHolder(itemView);
    }

    @Override
    public void setTextType(TextType textType) {
        throw new UnsupportedOperationException("checkbox will automatically set TextType.");
    }

    @Override
    public final int getViewType() {
        return CustomListItemTypes.PASSWORD_VIEW_TYPE;
    }


    @Override
    protected void onBind(ViewHolder viewHolder) {
        // setTextType is public but with PasswordListItem it should only be
        // set to be one of the two types as follows so we use super and
        // throw exception on our setTextType.
        super.setTextType(mShowPassword ? TextType.VISIBLE_PASSWORD : TextType.HIDDEN_PASSWORD);
        super.onBind(viewHolder);
        viewHolder.checkbox.setChecked(mShowPassword);
        viewHolder.checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mShowPassword = isChecked;
            super.setTextType(mShowPassword
                    ? TextType.VISIBLE_PASSWORD : TextType.HIDDEN_PASSWORD);
            viewHolder.editText.setInputType(getTextType().getValue());
        });
        viewHolder.checkbox.setEnabled(isEnabled());
    }

    static class ViewHolder extends EditTextListItem.ViewHolder {
        public final CheckBox checkbox;

        ViewHolder(View view) {
            super(view);
            checkbox = view.findViewById(R.id.checkbox);
        }
    }
}
