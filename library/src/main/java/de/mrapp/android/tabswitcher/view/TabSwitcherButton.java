/*
 * Copyright 2016 - 2017 Michael Rapp
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

import android.content.Context;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageButton;
import android.util.AttributeSet;

import de.mrapp.android.tabswitcher.Animation;
import de.mrapp.android.tabswitcher.R;
import de.mrapp.android.tabswitcher.Tab;
import de.mrapp.android.tabswitcher.TabSwitcher;
import de.mrapp.android.tabswitcher.TabSwitcherListener;
import de.mrapp.android.tabswitcher.drawable.TabSwitcherDrawable;
import de.mrapp.android.util.ThemeUtil;
import de.mrapp.android.util.ViewUtil;

/**
 * An image button, which allows to display the number of tabs, which are currently contained by a
 * {@link TabSwitcher} by using a {@link TabSwitcherDrawable}. It must be registered at a {@link
 * TabSwitcher} instance in order to keep the displayed count up to date. It therefore implements
 * the interface {@link TabSwitcherListener}.
 *
 * @author Michael Rapp
 * @since 0.1.0
 */
public class TabSwitcherButton extends AppCompatImageButton implements TabSwitcherListener {

    /**
     * The drawable, which is used by the image button.
     */
    private TabSwitcherDrawable drawable;

    /**
     * Initializes the view.
     */
    private void initialize() {
        drawable = new TabSwitcherDrawable(getContext());
        setImageDrawable(drawable);
        ViewUtil.setBackground(this,
                ThemeUtil.getDrawable(getContext(), R.attr.selectableItemBackgroundBorderless));
        setContentDescription(null);
        setClickable(true);
        setFocusable(true);
    }

    /**
     * Creates a new image button, which allows to display the number of tabs, which are currently
     * contained by a {@link TabSwitcher}.
     *
     * @param context
     *         The context, which should be used by the view, as an instance of the class {@link
     *         Context}. The context may not be null
     */
    public TabSwitcherButton(@NonNull final Context context) {
        this(context, null);
    }

    /**
     * Creates a new image button, which allows to display the number of tabs, which are currently
     * contained by a {@link TabSwitcher}.
     *
     * @param context
     *         The context, which should be used by the view, as an instance of the class {@link
     *         Context}. The context may not be null
     * @param attributeSet
     *         The attribute set, the view's attributes should be obtained from, as an instance of
     *         the type {@link AttributeSet} or null, if no attributes should be obtained
     */
    public TabSwitcherButton(@NonNull final Context context,
                             @Nullable final AttributeSet attributeSet) {
        super(context, attributeSet);
        initialize();
    }

    /**
     * Creates a new image button, which allows to display the number of tabs, which are currently
     * contained by a {@link TabSwitcher}.
     *
     * @param context
     *         The context, which should be used by the view, as an instance of the class {@link
     *         Context}. The context may not be null
     * @param attributeSet
     *         The attribute set, the view's attributes should be obtained from, as an instance of
     *         the type {@link AttributeSet} or null, if no attributes should be obtained
     * @param defaultStyle
     *         The default style to apply to this view. If 0, no style will be applied (beyond what
     *         is included in the theme). This may either be an attribute resource, whose value will
     *         be retrieved from the current theme, or an explicit style resource
     */
    public TabSwitcherButton(@NonNull final Context context,
                             @Nullable final AttributeSet attributeSet,
                             @AttrRes final int defaultStyle) {
        super(context, attributeSet, defaultStyle);
        initialize();
    }

    /**
     * Updates the image button to display a specific value.
     *
     * @param count
     *         The value, which should be displayed, as an {@link Integer} value. The value must be
     *         at least 0
     */
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
                                 @NonNull final Tab tab, @NonNull final Animation animation) {
        drawable.onTabAdded(tabSwitcher, index, tab, animation);
    }

    @Override
    public final void onTabRemoved(@NonNull final TabSwitcher tabSwitcher, final int index,
                                   @NonNull final Tab tab, @NonNull final Animation animation) {
        drawable.onTabRemoved(tabSwitcher, index, tab, animation);
    }

    @Override
    public final void onAllTabsRemoved(@NonNull final TabSwitcher tabSwitcher,
                                       @NonNull final Tab[] tabs,
                                       @NonNull final Animation animation) {
        drawable.onAllTabsRemoved(tabSwitcher, tabs, animation);
    }

}