/*
 * Copyright (C) 2017 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.car.settings.common;

import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.android.car.settings.R;

/**
 * Contains logic for a line item represents text only view of a title and a EditText as input.
 */
public class EditTextLineItem extends TypedPagedListAdapter.LineItem<EditTextLineItem.ViewHolder> {
    private final CharSequence mTitle;
    private final CharSequence mInitialInputTest;

    public interface TextChangeListener {
        void textChanged(Editable s);
    }

    private TextChangeListener mTextChangeListener;

    public EditTextLineItem(CharSequence title, CharSequence initialInputText) {
        mTitle = title;
        mInitialInputTest = initialInputText;
    }

    public void setTextChangeListener(TextChangeListener listener) {
        mTextChangeListener = listener;
    }

    @Override
    public int getType() {
        return EDIT_TEXT_TYPE;
    }

    @Override
    public void bindViewHolder(ViewHolder viewHolder) {
        viewHolder.titleView.setText(mTitle);
        viewHolder.editText.setText(mInitialInputTest);
        viewHolder.editText.addTextChangedListener(new TextWatcher() {
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
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView titleView;
        public final EditText editText;

        public ViewHolder(View view) {
            super(view);
            titleView = view.findViewById(R.id.title);
            editText = view.findViewById(R.id.input);
        }
    }

    public static RecyclerView.ViewHolder createViewHolder(ViewGroup parent) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.edit_text_line_item, parent, false));
    }

    @Override
    public CharSequence getDesc() {
        return null;
    }

    @Override
    public boolean isExpandable() {
        return false;
    }
}
