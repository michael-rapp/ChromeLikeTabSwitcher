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
package de.mrapp.android.tabswitcher.layout.tablet;

import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.support.v7.widget.Toolbar;
import android.view.MotionEvent;
import android.view.ViewGroup;

import de.mrapp.android.tabswitcher.Animation;
import de.mrapp.android.tabswitcher.Tab;
import de.mrapp.android.tabswitcher.TabSwitcher;
import de.mrapp.android.tabswitcher.layout.AbstractDragHandler;
import de.mrapp.android.tabswitcher.layout.AbstractDragHandler.DragState;
import de.mrapp.android.tabswitcher.layout.AbstractTabSwitcherLayout;
import de.mrapp.android.tabswitcher.layout.Arithmetics;
import de.mrapp.android.tabswitcher.model.TabItem;
import de.mrapp.android.tabswitcher.model.TabSwitcherModel;
import de.mrapp.android.util.logging.LogLevel;

/**
 * A layout, which implements the functionality of a {@link TabSwitcher} on tablets.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public class TabletTabSwitcherLayout extends AbstractTabSwitcherLayout {

    /**
     * Creates a new layout, which implements the functionality of a {@link TabSwitcher} on tablets.
     *
     * @param tabSwitcher
     *         The tab switcher, the layout belongs to, as an instance of the class {@link
     *         TabSwitcher}. The tab switcher may not be null
     * @param model
     *         The model of the tab switcher, the layout belongs to, as an instance of the class
     *         {@link TabSwitcherModel}. The model may not be null
     * @param arithmetics
     *         The arithmetics, which should be used by the layout, as an instance of the type
     *         {@link Arithmetics}. The arithmetics may not be null
     */
    public TabletTabSwitcherLayout(@NonNull final TabSwitcher tabSwitcher,
                                   @NonNull final TabSwitcherModel model,
                                   @NonNull final Arithmetics arithmetics) {
        super(tabSwitcher, model, arithmetics);
    }

    @Nullable
    @Override
    protected final AbstractDragHandler<?> onInflateLayout(final boolean tabsOnly) {
        return null;
    }

    @Nullable
    @Override
    protected final Pair<Integer, Float> onDetachLayout(final boolean tabsOnly) {
        return null;
    }

    @Override
    public final boolean handleTouchEvent(@NonNull final MotionEvent event) {
        return false;
    }

    @Override
    public final void onGlobalLayout() {

    }

    @Nullable
    @Override
    public final ViewGroup getTabContainer() {
        return null;
    }

    @Nullable
    @Override
    public final Toolbar[] getToolbars() {
        return new Toolbar[0];
    }

    @Override
    public final void onLogLevelChanged(@NonNull final LogLevel logLevel) {

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
                                 final boolean switcherVisibilityChanged,
                                 @NonNull final Animation animation) {

    }

    @Override
    public final void onAllTabsAdded(final int index, @NonNull final Tab[] tabs,
                                     final int previousSelectedTabIndex, final int selectedTabIndex,
                                     @NonNull final Animation animation) {

    }

    @Override
    public final void onTabRemoved(final int index, @NonNull final Tab tab,
                                   final int previousSelectedTabIndex, final int selectedTabIndex,
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
    public final void onTabIconChanged(@Nullable final Drawable icon) {

    }

    @Override
    public final void onTabBackgroundColorChanged(@Nullable final ColorStateList colorStateList) {

    }

    @Override
    public final void onTabTitleColorChanged(@Nullable final ColorStateList colorStateList) {

    }

    @Override
    public final void onTabCloseButtonIconChanged(@Nullable final Drawable icon) {

    }

    @Nullable
    @Override
    public final DragState onDrag(@NonNull final DragState dragState, final float dragDistance) {
        return null;
    }

    @Override
    public final void onClick(@NonNull final TabItem tabItem) {

    }

}