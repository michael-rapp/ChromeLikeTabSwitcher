/*
 * Copyright 2016 Michael Rapp
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package de.mrapp.android.tabswitcher.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.util.AttributeSet;
import android.widget.ImageButton;

import de.mrapp.android.tabswitcher.R;
import de.mrapp.android.tabswitcher.Tab;
import de.mrapp.android.tabswitcher.TabSwitcher;
import de.mrapp.android.tabswitcher.TabSwitcherListener;
import de.mrapp.android.tabswitcher.drawable.TabSwitcherDrawable;
import de.mrapp.android.util.ThemeUtil;
import de.mrapp.android.util.ViewUtil;

/**
 * A drawable, which allows to show the number of tabs, which are currently contained by a {@link
 * TabSwitcher} by using a {@link TabSwitcherDrawable}. It must be registered at a {@link
 * TabSwitcher} instance in order to keep the displayed count up to date.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public class TabSwitcherButton extends ImageButton implements TabSwitcherListener {

    private TabSwitcherDrawable drawable;

    private void initialize() {
        drawable = new TabSwitcherDrawable(getContext());
        setImageDrawable(drawable);
        ViewUtil.setBackground(this,
                ThemeUtil.getDrawable(getContext(), R.attr.selectableItemBackgroundBorderless));
        setContentDescription(null);
        setClickable(true);
        setFocusable(true);
    }

    public TabSwitcherButton(@NonNull final Context context) {
        this(context, null);
    }

    public TabSwitcherButton(@NonNull final Context context,
                             @Nullable final AttributeSet attributeSet) {
        super(context, attributeSet);
        initialize();
    }

    public TabSwitcherButton(@NonNull final Context context,
                             @Nullable final AttributeSet attributeSet,
                             @AttrRes final int defaultStyleAttribute) {
        super(context, attributeSet, defaultStyleAttribute);
        initialize();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public TabSwitcherButton(@NonNull final Context context,
                             @Nullable final AttributeSet attributeSet,
                             @AttrRes final int defaultStyleAttribute,
                             @StyleRes final int defaultStyleResource) {
        super(context, attributeSet, defaultStyleAttribute, defaultStyleResource);
        initialize();
    }

    public final void setCount(final int count) {
        drawable.setCount(count);
    }

    @Override
    public final void onSwitcherShown(@NonNull final TabSwitcher tabSwitcher) {
        drawable.onSwitcherShown(tabSwitcher);
    }

    @Override
    public final void onSwitcherHidden(@NonNull final TabSwitcher tabSwitcher) {
        drawable.onSwitcherHidden(tabSwitcher);
    }

    @Override
    public final void onSelectionChanged(@NonNull final TabSwitcher tabSwitcher,
                                         final int selectedTabIndex,
                                         @Nullable final Tab selectedTab) {
        drawable.onSelectionChanged(tabSwitcher, selectedTabIndex, selectedTab);
    }

    @Override
    public final void onTabAdded(@NonNull final TabSwitcher tabSwitcher, final int index,
                                 @NonNull final Tab tab) {
        drawable.onTabAdded(tabSwitcher, index, tab);
    }

    @Override
    public final void onTabRemoved(@NonNull final TabSwitcher tabSwitcher, final int index,
                                   @NonNull final Tab tab) {
        drawable.onTabRemoved(tabSwitcher, index, tab);
    }

    @Override
    public final void onAllTabsRemoved(@NonNull final TabSwitcher tabSwitcher) {
        drawable.onAllTabsRemoved(tabSwitcher);
    }

}