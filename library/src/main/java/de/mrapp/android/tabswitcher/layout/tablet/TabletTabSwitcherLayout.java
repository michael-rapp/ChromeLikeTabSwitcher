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

import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
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
import de.mrapp.android.tabswitcher.layout.Arithmetics.Axis;
import de.mrapp.android.tabswitcher.model.State;
import de.mrapp.android.tabswitcher.model.TabItem;
import de.mrapp.android.tabswitcher.model.TabSwitcherModel;
import de.mrapp.android.util.view.AttachedViewRecycler;
import de.mrapp.android.util.view.ViewRecycler;

/**
 * A layout, which implements the functionality of a {@link TabSwitcher} on tablets.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public class TabletTabSwitcherLayout extends AbstractTabSwitcherLayout<Void> {

    /**
     * A comparator, which allows to compare two instances of the class {@link TabItem}. The tab
     * item, which corresponds to the currently selected tab, is always sorted before all other tab
     * items.
     */
    private static class TabletTabItemComparator extends TabItem.Comparator {

        /**
         * Creates a new comparator, which allows to compare two instances of the class {@link
         * TabItem}. The tab item, which corresponds to the currently selected tab, is always sorted
         * before all other tab items.
         *
         * @param tabSwitcher
         *         The tab switcher, the tab items, which should be compared by the comparator,
         *         belong to, as a instance of the class {@link TabSwitcher}. The tab switcher may
         *         not be null
         */
        TabletTabItemComparator(@NonNull final TabSwitcher tabSwitcher) {
            super(tabSwitcher);
        }

        @Override
        public int compare(final TabItem o1, final TabItem o2) {
            if (o1.getTab() == getTabSwitcher().getSelectedTab()) {
                return -1;
            } else {
                return super.compare(o1, o2);
            }
        }

    }

    /**
     * The maximum width of a tab in pixels.
     */
    private final int maxTabWidth;

    /**
     * The minimum width of a tab in pixels.
     */
    private final int minTabWidth;

    /**
     * The offset between two neighboring tabs in pixels.
     */
    private final int tabOffset;

    /**
     * The drag handler, which is used by the layout.
     */
    private TabletDragHandler dragHandler;

    /**
     * The view recycler, which allows to recycler the views, which are associated with of tabs.
     */
    private ViewRecycler<Tab, Void> tabViewRecycler;

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
     * Adapts the margins of the tab container and the toolbars.
     */
    private void adaptTabContainerAndToolbarMargins() {
        FrameLayout.LayoutParams tabContainerLayoutParams =
                (FrameLayout.LayoutParams) tabContainer.getLayoutParams();
        tabContainerLayoutParams.setMargins(getModel().getPaddingLeft(), getModel().getPaddingTop(),
                getModel().getPaddingRight(), 0);
        FrameLayout.LayoutParams primaryToolbarLayoutParams =
                (FrameLayout.LayoutParams) primaryToolbar.getLayoutParams();
        primaryToolbarLayoutParams
                .setMargins(getModel().getPaddingLeft(), 0, getModel().getPaddingRight(), 0);
        FrameLayout.LayoutParams secondaryToolbarLayoutParams =
                (FrameLayout.LayoutParams) secondaryToolbar.getLayoutParams();
        secondaryToolbarLayoutParams
                .setMargins(getModel().getPaddingLeft(), 0, getModel().getPaddingRight(), 0);
    }

    /**
     * Calculates and returns the width of the tabs, depending on the total number of tabs, which
     * are currently contained by the tab switcher.
     *
     * @return The width, which has been calculated, in pixels as an {@link Integer} value
     */
    private int calculateTabWidth() {
        return maxTabWidth;
    }

    /**
     * Calculates and returns the space between two neighboring tabs.
     *
     * @return The space, which has been calculated, in pixels as an {@link Integer} value
     */
    private int calculateTabSpacing() {
        return calculateTabWidth() - tabOffset;
    }

    /**
     * Calculates and returns the width of the tab container.
     *
     * @return The width of the tab container in pixels as a {@link Float} value
     */
    private float calculateTabContainerWidth() {
        float size = getArithmetics().getSize(Axis.DRAGGING_AXIS, getTabSwitcher());
        int padding = getModel().getPaddingRight() + getModel().getPaddingLeft();
        Toolbar[] toolbars = getToolbars();
        float toolbarSize = getModel().areToolbarsShown() && toolbars != null ?
                Math.max(0, toolbars[0].getWidth() - tabOffset) +
                        Math.max(0, toolbars[1].getWidth() - tabOffset) : 0;
        return size - padding - toolbarSize;
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
        setFirstVisibleIndex(-1);
        TabItem[] tabItems = new TabItem[getModel().getCount()];

        if (!getModel().isEmpty()) {
            int tabSpacing = calculateTabSpacing();
            int referenceIndex = firstVisibleTabIndex != -1 && firstVisibleTabPosition != -1 ?
                    firstVisibleTabIndex : 0;
            float referencePosition = firstVisibleTabIndex != -1 && firstVisibleTabPosition != -1 ?
                    firstVisibleTabPosition : -1;
            TabItem referenceTabItem = null;
            AbstractTabItemIterator iterator =
                    new InitialTabItemIterator(tabItems, false, referenceIndex);
            TabItem tabItem;

            while ((tabItem = iterator.next()) != null) {
                TabItem predecessor = iterator.previous();
                float position;

                if (tabItem.getIndex() == referenceIndex && referencePosition != -1) {
                    referenceTabItem = tabItem;
                    position = referencePosition;
                } else {
                    position = (getModel().getCount() - tabItem.getIndex() - 1) * tabSpacing;
                }

                Pair<Float, State> pair =
                        clipTabPosition(getModel().getCount(), tabItem.getIndex(), position,
                                predecessor);
                tabItem.getTag().setPosition(pair.first);
                tabItem.getTag().setState(pair.second);

                if (getFirstVisibleIndex() == -1 && pair.second == State.FLOATING) {
                    setFirstVisibleIndex(tabItem.getIndex());
                }
            }

            if (referenceIndex > 0 && referenceTabItem != null) {
                iterator = new InitialTabItemIterator(tabItems, true, referenceIndex - 1);

                while ((tabItem = iterator.next()) != null && tabItem.getIndex() < referenceIndex) {
                    TabItem predecessor = iterator.peek();
                    float position = referenceTabItem.getTag().getPosition() +
                            ((tabItem.getIndex() - referenceIndex) * tabSpacing);
                    Pair<Float, State> pair =
                            clipTabPosition(getModel().getCount(), tabItem.getIndex(), position,
                                    predecessor);
                    tabItem.getTag().setPosition(pair.first);
                    tabItem.getTag().setState(pair.second);

                    if (pair.second == State.FLOATING) {
                        setFirstVisibleIndex(tabItem.getIndex());
                    }
                }
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
        Resources resources = tabSwitcher.getResources();
        maxTabWidth = resources.getDimensionPixelSize(R.dimen.tablet_tab_max_width);
        minTabWidth = resources.getDimensionPixelSize(R.dimen.tablet_tab_min_width);
        tabOffset = resources.getDimensionPixelSize(R.dimen.tablet_tab_offset);
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
        tabViewRecycler = new ViewRecycler<>(inflater);
        recyclerAdapter = new TabletRecyclerAdapter(getTabSwitcher(), getModel());
        getModel().addListener(recyclerAdapter);
        viewRecycler = new AttachedViewRecycler<>(tabContainer, inflater,
                Collections.reverseOrder(new TabletTabItemComparator(getTabSwitcher())));
        viewRecycler.setAdapter(recyclerAdapter);
        recyclerAdapter.setViewRecycler(viewRecycler);
        dragHandler = new TabletDragHandler(getTabSwitcher(), getArithmetics(), viewRecycler);
        adaptTabContainerAndToolbarMargins();
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
    protected final ViewRecycler<Tab, Void> getTabViewRecycler() {
        return tabViewRecycler;
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
        float tabContainerWidth = calculateTabContainerWidth();

        if (index < getStackedTabCount()) {
            float position = tabContainerWidth - calculateTabWidth() -
                    (getStackedTabSpacing() * (index + 1));
            return Pair.create(position, State.STACKED_END);
        } else {
            float position = tabContainerWidth - calculateTabWidth() -
                    (getStackedTabSpacing() * getStackedTabCount());
            return Pair.create(position, State.HIDDEN);
        }
    }

    @Override
    protected final float calculateMinStartPosition(final int index) {
        int thresholdPosition = (getModel().getCount() - 1) * calculateTabSpacing();
        float tabContainerWidth = calculateTabContainerWidth();

        if (thresholdPosition < tabContainerWidth) {
            return (getModel().getCount() - index - 1) * calculateTabSpacing();
        } else {
            return tabContainerWidth - calculateTabWidth() - (index * calculateTabSpacing());
        }
    }

    @Override
    protected final float calculateMaxEndPosition(final int index) {
        return (getModel().getCount() - index - 1) * calculateTabSpacing();
    }

    @Override
    protected final float calculateSuccessorPosition(@NonNull final TabItem tabItem,
                                                     @NonNull final TabItem predecessor) {
        float predecessorPosition = predecessor.getTag().getPosition();
        return predecessorPosition - calculateTabSpacing();
    }

    @Override
    protected final float calculatePredecessorPosition(@NonNull final TabItem tabItem,
                                                       @NonNull final TabItem successor) {
        float successorPosition = successor.getTag().getPosition();
        return successorPosition + calculateTabSpacing();
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

        Tab selectedTab = getModel().getSelectedTab();

        if (selectedTab != null) {
            Pair<View, ?> pair = tabViewRecycler.inflate(selectedTab, contentContainer);
            View view = pair.first;
            FrameLayout.LayoutParams layoutParams =
                    new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT);
            layoutParams.setMargins(getModel().getPaddingLeft(), 0, getModel().getPaddingRight(),
                    getModel().getPaddingBottom());
            contentContainer.addView(view, layoutParams);
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
    public final void onSelectionChanged(final int previousIndex, final int index,
                                         @Nullable final Tab selectedTab,
                                         final boolean switcherHidden) {
        if (previousIndex != index && selectedTab != null) {
            viewRecycler.setComparator(
                    Collections.reverseOrder(new TabletTabItemComparator(getTabSwitcher())));
        }
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
        adaptTabContainerAndToolbarMargins();
    }

}