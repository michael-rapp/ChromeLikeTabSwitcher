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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.FrameLayout;

import java.util.Collections;

import de.mrapp.android.tabswitcher.Animation;
import de.mrapp.android.tabswitcher.R;
import de.mrapp.android.tabswitcher.Tab;
import de.mrapp.android.tabswitcher.TabSwitcher;
import de.mrapp.android.tabswitcher.iterator.AbstractTabItemIterator;
import de.mrapp.android.tabswitcher.layout.AbstractDragHandler;
import de.mrapp.android.tabswitcher.layout.AbstractTabSwitcherLayout;
import de.mrapp.android.tabswitcher.layout.Arithmetics;
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
public class TabletTabSwitcherLayout extends AbstractTabSwitcherLayout<Void> {

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

    @Override
    protected final void onInflateLayout(final boolean tabsOnly) {
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
    }

    @Override
    protected final void onDetachLayout(final boolean tabsOnly) {
        // TODO: childViewRecycler.removeAll();
        // TODO: childViewRecycler.clearCache();
        if (!tabsOnly) {
            getModel().removeListener(recyclerAdapter);
        }
    }

    @Override
    protected final AbstractDragHandler<?> getDragHandler() {
        return dragHandler;
    }

    @Override
    protected final AttachedViewRecycler<TabItem, Void> getViewRecycler() {
        return viewRecycler;
    }

    @Override
    protected final void inflateAndUpdateView(@NonNull final TabItem tabItem,
                                              @Nullable final OnGlobalLayoutListener listener) {
        inflateView(tabItem, createInflateViewLayoutListener(tabItem, listener));
    }

    @NonNull
    @Override
    protected final Pair<Float, State> calculatePositionAndStateWhenStackedAtEnd(final int index) {
        float size = 1000; // TODO: Use correct width of the tab container

        if (index < getStackedTabCount()) {
            float position = size - (getStackedTabSpacing() * (index + 1));
            return Pair.create(position, State.STACKED_END);
        } else {
            float position = size - (getStackedTabSpacing() * getStackedTabCount());
            return Pair.create(position, State.HIDDEN);
        }
    }

    @Override
    protected final boolean isOvershootingAtStart() {
        // TODO: Implement
        return false;
    }

    @Override
    protected final boolean isOvershootingAtEnd(@NonNull final AbstractTabItemIterator iterator) {
        // TODO: Implement
        return false;
    }

    @Override
    protected final float calculateEndPosition(final int index) {
        // TODO: Use correct distance
        return (getModel().getCount() - index - 1) * 200;
    }

    @Override
    protected final float calculateSuccessorPosition(@NonNull final TabItem tabItem,
                                                     @NonNull final TabItem predecessor) {
        float predecessorPosition = predecessor.getTag().getPosition();
        // TODO: Use correct distance
        return predecessorPosition - 200;
    }

    @Override
    protected final float calculatePredecessorPosition(@NonNull final TabItem tabItem,
                                                       @NonNull final TabItem successor) {
        float successorPosition = successor.getTag().getPosition();
        // TODO: Use correct distance
        return successorPosition + 200;
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
        // TODO: Implement
    }

    @Override
    public final void onSelectionChanged(final int previousIndex, final int index,
                                         @Nullable final Tab selectedTab,
                                         final boolean switcherHidden) {
        // TODO: Implement
    }

    @Override
    public final void onTabAdded(final int index, @NonNull final Tab tab,
                                 final int previousSelectedTabIndex, final int selectedTabIndex,
                                 final boolean switcherVisibilityChanged,
                                 @NonNull final Animation animation) {
        // TODO: Implement
    }

    @Override
    public final void onAllTabsAdded(final int index, @NonNull final Tab[] tabs,
                                     final int previousSelectedTabIndex, final int selectedTabIndex,
                                     @NonNull final Animation animation) {
        // TODO: Implement
    }

    @Override
    public final void onTabRemoved(final int index, @NonNull final Tab tab,
                                   final int previousSelectedTabIndex, final int selectedTabIndex,
                                   @NonNull final Animation animation) {
        // TODO: Implement
    }

    @Override
    public final void onAllTabsRemoved(@NonNull final Tab[] tabs,
                                       @NonNull final Animation animation) {
        // TODO: Implement
    }

    @Override
    public final void onPaddingChanged(final int left, final int top, final int right,
                                       final int bottom) {
        adaptTabContainerMargin();
    }

}