/*
 * Copyright 2016 - 2019 Michael Rapp
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
package de.mrapp.android.tabswitcher.layout.tablet;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import de.mrapp.android.tabswitcher.Animation;
import de.mrapp.android.tabswitcher.Tab;
import de.mrapp.android.tabswitcher.TabSwitcher;
import de.mrapp.android.tabswitcher.TabSwitcherDecorator;
import de.mrapp.android.tabswitcher.layout.ContentRecyclerAdapter;
import de.mrapp.android.tabswitcher.model.Model;
import de.mrapp.android.tabswitcher.model.Restorable;
import de.mrapp.android.tabswitcher.model.TabSwitcherStyle;
import de.mrapp.android.util.logging.LogLevel;
import de.mrapp.android.util.view.AbstractViewRecycler;
import de.mrapp.android.util.view.AttachedViewRecycler;
import de.mrapp.util.Condition;

/**
 * A wrapper, which encapsulates a {@link ContentRecyclerAdapter}, which allows to inflate the
 * views, which are used to visualize the content views of the tabs of a {@link TabSwitcher}. The
 * wrapper enables to adapt the views for use with the tablet layout.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public class TabletContentRecyclerAdapterWrapper extends AbstractViewRecycler.Adapter<Tab, Void>
        implements Restorable, Tab.Callback, Model.Listener {

    /**
     * The tab switcher, the recycler adapter belongs to.
     */
    private final TabSwitcher tabSwitcher;

    /**
     * The style, which allows to retrieve style attributes of the tab switcher.
     */
    private final TabSwitcherStyle style;

    /**
     * The view recycler, the recycler adapter is attached to.
     */
    private final AttachedViewRecycler<Tab, ?> viewRecycler;

    /**
     * The encapsulated view recycler adapter.
     */
    private final ContentRecyclerAdapter encapsulatedAdapter;

    /**
     * Adapts the background color of a specific tab's content.
     *
     * @param view
     *         The view, which is used to visualize the tab's content, as an instance of the class
     *         {@link View}. The view may not be null
     * @param tab
     *         The tab as an instance of the class {@link Tab}. The tab may not be null
     */
    private void adaptContentBackgroundColor(@NonNull final View view, @NonNull final Tab tab) {
        int color = style.getTabContentBackgroundColor(tab);
        view.setBackgroundColor(color);
    }

    /**
     * Creates a new wrapper, which encapsulates a {@link ContentRecyclerAdapter}, which allows to
     * inflate the views, which are used to visualize the content views of the tabs of a {@link
     * TabSwitcher}.
     *
     * @param tabSwitcher
     *         The tab switcher, the recycler adapter belongs to, as an instance of the class {@link
     *         TabSwitcher}. The tab switcher may not be null
     * @param style
     *         The style, which allows to retrieve style attributes of the tab switcher, as an
     *         instance of the class {@link TabSwitcherStyle}. The style may not be null
     * @param viewRecycler
     *         The view recycler, the adapter is attached to, as an instance of the class
     *         AttachedViewRecycler. The view recycler may not be null
     * @param encapsulatedAdapter
     *         The view recycler adapter, which should be encapsulated, as an instance of the class
     *         {@link ContentRecyclerAdapter}. The recycler adapter may not be null
     */
    public TabletContentRecyclerAdapterWrapper(@NonNull final TabSwitcher tabSwitcher,
                                               @NonNull final TabSwitcherStyle style,
                                               @NonNull final AttachedViewRecycler<Tab, ?> viewRecycler,
                                               @NonNull final ContentRecyclerAdapter encapsulatedAdapter) {
        Condition.INSTANCE.ensureNotNull(tabSwitcher, "The tab switcher may not be null");
        Condition.INSTANCE.ensureNotNull(style, "The style may not be null");
        Condition.INSTANCE.ensureNotNull(viewRecycler, "The view recycler may not be null");
        Condition.INSTANCE
                .ensureNotNull(encapsulatedAdapter, "The recycler adapter may not be null");
        this.tabSwitcher = tabSwitcher;
        this.style = style;
        this.viewRecycler = viewRecycler;
        this.encapsulatedAdapter = encapsulatedAdapter;
    }

    @NonNull
    @Override
    public final View onInflateView(@NonNull final LayoutInflater inflater,
                                    @Nullable final ViewGroup parent, @NonNull final Tab item,
                                    final int viewType, @NonNull final Void... params) {
        View view = encapsulatedAdapter.onInflateView(inflater, parent, item, viewType, params);
        FrameLayout container = new FrameLayout(tabSwitcher.getContext());
        container.setPadding(tabSwitcher.getPaddingLeft(), 0, tabSwitcher.getPaddingRight(),
                tabSwitcher.getPaddingBottom());
        container.setLayoutParams(
                new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));
        container.addView(view);
        return container;
    }

    @Override
    public final void onShowView(@NonNull final Context context, @NonNull final View view,
                                 @NonNull final Tab item, final boolean inflated,
                                 @NonNull final Void... params) {
        encapsulatedAdapter.onShowView(context, view, item, inflated, params);
        item.addCallback(this);
        adaptContentBackgroundColor(view, item);
    }

    @Override
    public final void onRemoveView(@NonNull final View view, @NonNull final Tab item) {
        encapsulatedAdapter.onRemoveView(view, item);
        item.removeCallback(this);
    }

    @Override
    public final int getViewTypeCount() {
        return encapsulatedAdapter.getViewTypeCount();
    }

    @Override
    public final int getViewType(@NonNull final Tab item) {
        return encapsulatedAdapter.getViewType(item);
    }

    @Override
    public final void saveInstanceState(@NonNull final Bundle outState) {
        encapsulatedAdapter.saveInstanceState(outState);
    }

    @Override
    public final void restoreInstanceState(@Nullable final Bundle savedInstanceState) {
        encapsulatedAdapter.restoreInstanceState(savedInstanceState);
    }

    @Override
    public final void onTitleChanged(@NonNull final Tab tab) {

    }

    @Override
    public final void onIconChanged(@NonNull final Tab tab) {

    }

    @Override
    public final void onCloseableChanged(@NonNull final Tab tab) {

    }

    @Override
    public final void onCloseButtonIconChanged(@NonNull final Tab tab) {

    }

    @Override
    public final void onBackgroundColorChanged(@NonNull final Tab tab) {

    }

    @Override
    public final void onContentBackgroundColorChanged(@NonNull final Tab tab) {
        View view = viewRecycler.getView(tab);

        if (view != null) {
            adaptContentBackgroundColor(view, tab);
        }
    }

    @Override
    public final void onTitleTextColorChanged(@NonNull final Tab tab) {

    }

    @Override
    public final void onProgressBarVisibilityChanged(@NonNull final Tab tab) {

    }

    @Override
    public final void onProgressBarColorChanged(@NonNull final Tab tab) {

    }

    @Override
    public final void onLogLevelChanged(@NonNull final LogLevel logLevel) {

    }

    @Override
    public final void onDecoratorChanged(@NonNull final TabSwitcherDecorator decorator) {

    }

    @Override
    public final void onSwitcherShown() {

    }

    @Override
    public final void onSwitcherHidden() {

    }

    @Override
    public final void onSelectionChanged(final int previousIndex, final int index,
                                         @Nullable final Tab selectedTab,
                                         final boolean switcherHidden) {

    }

    @Override
    public final void onTabAdded(final int index, @NonNull final Tab tab,
                                 final int previousSelectedTabIndex, final int selectedTabIndex,
                                 final boolean selectionChanged,
                                 final boolean switcherVisibilityChanged,
                                 @NonNull final Animation animation) {

    }

    @Override
    public final void onAllTabsAdded(final int index, @NonNull final Tab[] tabs,
                                     final int previousSelectedTabIndex, final int selectedTabIndex,
                                     final boolean selectionChanged,
                                     @NonNull final Animation animation) {

    }

    @Override
    public final void onTabRemoved(final int index, @NonNull final Tab tab,
                                   final int previousSelectedTabIndex, final int selectedTabIndex,
                                   final boolean selectionChanged,
                                   @NonNull final Animation animation) {

    }

    @Override
    public final void onAllTabsRemoved(@NonNull final Tab[] tabs,
                                       @NonNull final Animation animation) {

    }

    @Override
    public final void onPaddingChanged(final int left, final int top, final int right,
                                       final int bottom) {

    }

    @Override
    public final void onApplyPaddingToTabsChanged(final boolean applyPaddingToTabs) {

    }

    @Override
    public final void onTabIconChanged(@Nullable final Drawable icon) {

    }

    @Override
    public final void onTabBackgroundColorChanged(@Nullable final ColorStateList colorStateList) {

    }

    @Override
    public final void onTabContentBackgroundColorChanged(@ColorInt final int color) {
        for (Tab tab : tabSwitcher) {
            View view = viewRecycler.getView(tab);

            if (view != null) {
                adaptContentBackgroundColor(view, tab);
            }
        }
    }

    @Override
    public final void onTabTitleColorChanged(@Nullable final ColorStateList colorStateList) {

    }

    @Override
    public final void onTabCloseButtonIconChanged(@Nullable final Drawable icon) {

    }

    @Override
    public final void onTabProgressBarColorChanged(@ColorInt final int color) {

    }

    @Override
    public final void onAddTabButtonVisibilityChanged(final boolean visible) {

    }

    @Override
    public final void onAddTabButtonColorChanged(@Nullable final ColorStateList colorStateList) {

    }

    @Override
    public final void onToolbarVisibilityChanged(final boolean visible) {

    }

    @Override
    public final void onToolbarTitleChanged(@Nullable final CharSequence title) {

    }

    @Override
    public final void onToolbarNavigationIconChanged(@Nullable final Drawable icon,
                                                     @Nullable final View.OnClickListener listener) {

    }

    @Override
    public final void onToolbarMenuInflated(@MenuRes final int resourceId,
                                            @Nullable final Toolbar.OnMenuItemClickListener listener) {

    }

    @Override
    public final void onEmptyViewChanged(@Nullable final View view, final long animationDuration) {

    }

}