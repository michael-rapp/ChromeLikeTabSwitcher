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
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.FrameLayout;

import java.util.Collections;

import de.mrapp.android.tabswitcher.Animation;
import de.mrapp.android.tabswitcher.R;
import de.mrapp.android.tabswitcher.Tab;
import de.mrapp.android.tabswitcher.TabSwitcher;
import de.mrapp.android.tabswitcher.iterator.AbstractInitialTabItemIterator;
import de.mrapp.android.tabswitcher.iterator.AbstractTabItemIterator;
import de.mrapp.android.tabswitcher.layout.AbstractDragHandler;
import de.mrapp.android.tabswitcher.layout.AbstractDragHandler.DragState;
import de.mrapp.android.tabswitcher.layout.AbstractTabSwitcherLayout;
import de.mrapp.android.tabswitcher.layout.Arithmetics;
import de.mrapp.android.tabswitcher.layout.Arithmetics.Axis;
import de.mrapp.android.tabswitcher.model.State;
import de.mrapp.android.tabswitcher.model.TabItem;
import de.mrapp.android.tabswitcher.model.TabSwitcherModel;
import de.mrapp.android.util.logging.LogLevel;
import de.mrapp.android.util.view.AttachedViewRecycler;

/**
 * A layout, which implements the functionality of a {@link TabSwitcher} on tablets.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public class TabletTabSwitcherLayout extends AbstractTabSwitcherLayout {

    /**
     * An iterator, which allows to iterate the tab items, which correspond to the tabs of a {@link
     * TabSwitcher}. When a tab item is referenced for the first time, its initial position and
     * state is calculated and the tab item is stored in a backing array. When the tab item is
     * iterated again, it is retrieved from the backing array.
     */
    private class InitialTabItemIterator extends AbstractInitialTabItemIterator {

        /**
         * Calculates the initial position and state of a specific tab item.
         *
         * @param tabItem
         *         The tab item, whose position and state should be calculated, as an instance of
         *         the class {@link TabItem}. The tab item may not be null
         * @param predecessor
         *         The predecessor of the given tab item as an instance of the class {@link TabItem}
         *         or null, if the tab item does not have a predecessor
         */
        private void calculateAndClipStartPosition(@NonNull final TabItem tabItem,
                                                   @Nullable final TabItem predecessor) {
            float position = calculateStartPosition(tabItem);
            Pair<Float, State> pair =
                    clipTabPosition(getModel().getCount(), tabItem.getIndex(), position,
                            predecessor);
            tabItem.getTag().setPosition(pair.first);
            tabItem.getTag().setState(pair.second);
        }

        /**
         * Calculates and returns the initial position of a specific tab item.
         *
         * @param tabItem
         *         The tab item, whose position should be calculated, as an instance of the class
         *         {@link TabItem}. The tab item may not be null
         * @return The position, which has been calculated, as a {@link Float} value
         */
        private float calculateStartPosition(@NonNull final TabItem tabItem) {
            // TODO: Calculate position
            return 0;
        }

        /**
         * Creates a new iterator, which allows to iterate the tab items, which corresponds to the
         * tabs of a {@link TabSwitcher}. When a tab item is referenced for the first time, its
         * initial position and state is calculated and the tab item is stored in a backing array.
         * When the tab item is iterated again, it is retrieved from the backing array.
         *
         * @param backingArray
         *         The backing array, which should be used to store tab items, once their initial
         *         position and state has been calculated, as an array of the type {@link TabItem}.
         *         The array may not be null and the array's length must be equal to the number of
         *         tabs, which are contained by the given tab switcher
         * @param reverse
         *         True, if the tabs should be iterated in reverse order, false otherwise
         * @param start
         *         The index of the first tab, which should be iterated, as an {@link Integer} value
         *         or -1, if all tabs should be iterated
         */
        private InitialTabItemIterator(@NonNull final TabItem[] backingArray, final boolean reverse,
                                       final int start) {
            super(backingArray, reverse, start);
        }

        @NonNull
        @Override
        protected TabItem createInitialTabItem(final int index) {
            TabItem tabItem = TabItem.create(getModel(), viewRecycler, index);
            calculateAndClipStartPosition(tabItem, index > 0 ? getItem(index - 1) : null);
            return tabItem;
        }

    }

    /**
     * The drag handler, which is used by the layout.
     */
    private TabletDragHandler dragHandler;

    /**
     * The adapter, which allows to inflate the views, which are used to visualize tabs.
     */
    private TabletRecyclerAdapter recyclerAdapter;

    /**
     * The view recycler, which allows to recycle the views, which are used to visualize tabs.
     */
    private AttachedViewRecycler<TabItem, Void> viewRecycler;

    /**
     * The layout's primary toolbar.
     */
    private Toolbar primaryToolbar;

    /**
     * The layout's secondary toolbar.
     */
    private Toolbar secondaryToolbar;

    /**
     * The view group, which contains the tab switcher's tabs.
     */
    private ViewGroup tabContainer;

    /**
     * The view group, which contains the children of the tab switcher's tabs.
     */
    private ViewGroup contentContainer;

    /**
     * Adapts the margin of the tab container.
     */
    private void adaptTabContainerMargin() {
        FrameLayout.LayoutParams layoutParams =
                (FrameLayout.LayoutParams) tabContainer.getLayoutParams();
        layoutParams.setMargins(getModel().getPaddingLeft(), getModel().getPaddingTop(),
                getModel().getPaddingRight(), 0);
    }

    /**
     * Clips the position of a specific tab.
     *
     * @param count
     *         The total number of tabs, which are currently contained by the tab switcher, as an
     *         {@link Integer} value
     * @param index
     *         The index of the tab, whose position should be clipped, as an {@link Integer} value
     * @param position
     *         The position, which should be clipped, in pixels as a {@link Float} value
     * @param predecessor
     *         The predecessor of the given tab item as an instance of the class {@link TabItem} or
     *         null, if the tab item does not have a predecessor
     * @return A pair, which contains the position and state of the tab item, as an instance of the
     * class {@link Pair}. The pair may not be null
     */
    @NonNull
    private Pair<Float, State> clipTabPosition(final int count, final int index,
                                               final float position,
                                               @Nullable final TabItem predecessor) {
        // TODO: Implement
        return Pair.create(position, State.FLOATING);
    }

    /**
     * Calculates and returns the tab items, which correspond to the tabs, when the tab switcher is
     * shown initially.
     *
     * @param firstVisibleTabIndex
     *         The index of the first visible tab as an {@link Integer} value or -1, if the index is
     *         unknown
     * @param firstVisibleTabPosition
     *         The position of the first visible tab in pixels as a {@link Float} value or -1, if
     *         the position is unknown
     * @return An array, which contains the tab items, as an array of the type {@link TabItem}. The
     * array may not be null
     */
    @NonNull
    private TabItem[] calculateInitialTabItems(final int firstVisibleTabIndex,
                                               final float firstVisibleTabPosition) {
        dragHandler.reset(getDragThreshold());
        TabItem[] tabItems = new TabItem[getModel().getCount()];

        if (!getModel().isEmpty()) {
            AbstractTabItemIterator iterator = new InitialTabItemIterator(tabItems, false, 0);
            TabItem tabItem;

            while ((tabItem = iterator.next()) != null) {

            }
        }

        dragHandler.setCallback(this);
        return tabItems;
    }

    /**
     * Inflates and updates the view, which is used to visualize a specific tab.
     *
     * @param tabItem
     *         The tab item, which corresponds to the tab, whose view should be inflated, as an
     *         instance of the class {@link TabItem}. The tab item may not be null
     * @param listener
     *         The layout listener, which should be notified, when the view has been inflated, as an
     *         instance of the type {@link OnGlobalLayoutListener} or null, if no listener should be
     *         notified
     */
    private void inflateAndUpdateView(@NonNull final TabItem tabItem,
                                      @Nullable final OnGlobalLayoutListener listener) {
        inflateView(tabItem, createInflateViewLayoutListener(tabItem, listener));
    }

    /**
     * Inflates the view, which is used to visualize a specific tab.
     *
     * @param tabItem
     *         The tab item, which corresponds to the tab, whose view should be inflated, as an
     *         instance of the class {@link TabItem}. The tab item may not be null
     * @param listener
     *         The layout listener, which should be notified, when the view has been inflated, as an
     *         instance of the type {@link OnGlobalLayoutListener} or null, if no listener should be
     *         notified
     */
    private void inflateView(@NonNull final TabItem tabItem,
                             @Nullable final OnGlobalLayoutListener listener) {
        Pair<View, Boolean> pair = viewRecycler.inflate(tabItem);

        if (listener != null) {
            boolean inflated = pair.second;

            if (inflated) {
                View view = pair.first;
                view.getViewTreeObserver()
                        .addOnGlobalLayoutListener(new LayoutListenerWrapper(view, listener));
            } else {
                listener.onGlobalLayout();
            }
        }
    }

    /**
     * Updates the view, which is used to visualize a specific tab.
     *
     * @param tabItem
     *         The tab item, which corresponds to the tab, whose view should be updated, as an
     *         instance of the class {@link TabItem}. The tab item may not be null
     */
    private void updateView(@NonNull final TabItem tabItem) {
        float position = tabItem.getTag().getPosition();
        View view = tabItem.getView();
        getArithmetics().setPosition(Axis.DRAGGING_AXIS, view, position);
        getArithmetics().setPosition(Axis.ORTHOGONAL_AXIS, view, 0);
    }

    /**
     * Creates and returns a layout listener, which allows to adapt the size and position of a tab,
     * once its view has been inflated.
     *
     * @param tabItem
     *         The tab item, which corresponds to the tab, whose view should be adapted, as an
     *         instance of the class {@link TabItem}. The tab item may not be null
     * @param layoutListener
     *         The layout lister, which should be notified, when the created listener is invoked, as
     *         an instance of the type {@link OnGlobalLayoutListener} or null, if no listener should
     *         be notified
     * @return The layout listener, which has been created, as an instance of the type {@link
     * OnGlobalLayoutListener}. The layout listener may not be null
     */
    @NonNull
    private OnGlobalLayoutListener createInflateViewLayoutListener(@NonNull final TabItem tabItem,
                                                                   @Nullable final OnGlobalLayoutListener layoutListener) {
        return new OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                updateView(tabItem);

                if (layoutListener != null) {
                    layoutListener.onGlobalLayout();
                }
            }

        };
    }

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
        LayoutInflater inflater = LayoutInflater.from(getContext());

        if (!tabsOnly) {
            inflater.inflate(R.layout.tablet_layout, getTabSwitcher(), true);
        }

        primaryToolbar = (Toolbar) getTabSwitcher().findViewById(R.id.primary_toolbar);
        secondaryToolbar = (Toolbar) getTabSwitcher().findViewById(R.id.secondary_toolbar);
        tabContainer = (ViewGroup) getTabSwitcher().findViewById(R.id.tab_container);
        contentContainer = (ViewGroup) getTabSwitcher().findViewById(R.id.content_container);
        recyclerAdapter = new TabletRecyclerAdapter(getTabSwitcher(), getModel());
        getModel().addListener(recyclerAdapter);
        viewRecycler = new AttachedViewRecycler<>(tabContainer, inflater,
                Collections.reverseOrder(new TabItem.Comparator(getTabSwitcher())));
        viewRecycler.setAdapter(recyclerAdapter);
        recyclerAdapter.setViewRecycler(viewRecycler);
        dragHandler = new TabletDragHandler(getTabSwitcher(), getArithmetics());
        adaptTabContainerMargin();
        return dragHandler;
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
        TabItem[] tabItems = calculateInitialTabItems(getModel().getFirstVisibleTabIndex(),
                getModel().getFirstVisibleTabPosition());
        AbstractTabItemIterator iterator = new InitialTabItemIterator(tabItems, false, 0);
        TabItem tabItem;

        while ((tabItem = iterator.next()) != null) {
            if (tabItem.isVisible()) {
                inflateAndUpdateView(tabItem, null);
            }
        }
    }

    @Nullable
    @Override
    public final ViewGroup getTabContainer() {
        return tabContainer;
    }

    @Nullable
    @Override
    public final Toolbar[] getToolbars() {
        return primaryToolbar != null && secondaryToolbar != null ?
                new Toolbar[]{primaryToolbar, secondaryToolbar} : null;
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
        adaptTabContainerMargin();
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