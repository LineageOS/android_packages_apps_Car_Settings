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
 * limitations under the License.
 */

package com.android.car.settings.common;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.LayoutRes;
import androidx.annotation.StringRes;
import androidx.car.widget.ListItemAdapter;
import androidx.car.widget.ListItemProvider;
import androidx.car.widget.PagedListView;
import androidx.car.widget.TextListItem;

import com.android.car.settings.R;

/**
 * Settings page that only contain a list of items.
 * <p>
 * Uses support library ListItemAdapter.
 */
public abstract class ListItemSettingsFragment extends BaseFragment implements ListController {
    private ListItemAdapter mListAdapter;

    @Override
    @LayoutRes
    protected int getLayoutId() {
        return R.layout.list_fragment;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mListAdapter = new ListItemAdapter(getContext(), getItemProvider());
        mListAdapter.registerListItemViewType(CustomListItemTypes.CHECK_BOX_VIEW_TYPE,
                CheckBoxListItem.getViewLayoutId(), CheckBoxListItem::createViewHolder);
        mListAdapter.registerListItemViewType(CustomListItemTypes.EDIT_TEXT_VIEW_TYPE,
                EditTextListItem.getViewLayoutId(), EditTextListItem::createViewHolder);
        mListAdapter.registerListItemViewType(CustomListItemTypes.PASSWORD_VIEW_TYPE,
                PasswordListItem.getViewLayoutId(), PasswordListItem::createViewHolder);
        mListAdapter.registerListItemViewType(CustomListItemTypes.SPINNER_VIEW_TYPE,
                SpinnerListItem.getViewLayoutId(), SpinnerListItem::createViewHolder);

        PagedListView listView = getView().findViewById(R.id.list);
        listView.setAdapter(mListAdapter);
        listView.setDividerVisibilityManager(mListAdapter);
        listView.setMaxPages(PagedListView.UNLIMITED_PAGES);
    }

    @Override
    public void refreshList() {
        mListAdapter.notifyDataSetChanged();
    }

    /**
     * Called in onActivityCreated.
     * Gets ListItemProvider that should provide items to show up in the list.
     */
    public abstract ListItemProvider getItemProvider();

    protected TextListItem createSimpleListItem(@StringRes int titleResId,
            View.OnClickListener onClickListener) {
        Context context = requireContext();
        TextListItem item = new TextListItem(context);
        item.setTitle(context.getString(titleResId));
        item.setSupplementalIcon(R.drawable.ic_chevron_right, /* showDivider= */ false);
        item.setOnClickListener(onClickListener);
        return item;
    }
}
