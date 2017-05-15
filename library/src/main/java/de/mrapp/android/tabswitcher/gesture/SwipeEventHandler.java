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
package de.mrapp.android.tabswitcher.gesture;

import android.content.res.Resources;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import de.mrapp.android.tabswitcher.R;
import de.mrapp.android.tabswitcher.TabSwitcher;
import de.mrapp.android.tabswitcher.model.AbstractItem;
import de.mrapp.android.tabswitcher.model.TabItem;
import de.mrapp.android.util.view.AttachedViewRecycler;

import static de.mrapp.android.util.Condition.ensureNotNull;

/**
 * An event handler, which allows to handle swipe gestures, which can be used to switch between
 * tabs.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public class SwipeEventHandler extends AbstractTouchEventHandler {

    /**
     * Defines the interface, a class, which should be notified about the events of a {@link
     * SwipeEventHandler}, must implement.
     */
    public interface Callback {

        /**
         * The method, which is invoked, when switching between neighboring tabs.
         *
         * @param tabItem
         *         The tab item, which corresponds to the swiped tab, as an instance of the class
         *         {@link TabItem}. The tab item may not be null
         * @param distance
         *         The distance, the tab is swiped by, in pixels as a {@link Float} value
         */
        void onSwitchingBetweenTabs(@NonNull TabItem tabItem, final float distance);

        /**
         * The method, which is invoked, when switching between neighboring tabs ended.
         *
         * @param tabItem
         *         The tab item, which corresponds to the swiped tab, as an instance of the class
         *         {@link TabItem}. The tab item may not be null
         * @param switchTabs
         *         True, if the selected tab should be switched, false otherwise
         * @param velocity
         *         The velocity of the swipe gesture in pixels per second as a {@link Float} value
         */
        void onSwitchingBetweenTabsEnded(@NonNull TabItem tabItem, boolean switchTabs,
                                         float velocity);

    }

    /**
     * The view recycler, which allows to inflate the views, which are used to visualize the tabs of
     * the tab switcher, the event handler belongs to.
     */
    private final AttachedViewRecycler<AbstractItem, ?> viewRecycler;

    /**
     * The velocity, which may be reached by a drag gesture at maximum to start a fling animation.
     */
    private final float maxFlingVelocity;

    /**
     * The velocity, which must be reached by a drag gesture in order to start a swipe animation.
     */
    private final float minSwipeVelocity;

    /**
     * The tab item, which corresponds to the tab, which is currently swiped.
     */
    private TabItem swipedTabItem;

    /**
     * The callback, which is notified about the event handler's events.
     */
    private Callback callback;

    /**
     * Returns, whether the threshold of a swiped tab item, which causes the previous or next tab to
     * be selected, has been reached, or not.
     *
     * @param swipedTabItem
     *         The swiped tab item as an instance of the class {@link TabItem}. The tab item may not
     *         be null
     * @return True, if the threshold has been reached, false otherwise
     */
    private boolean isSwipeThresholdReached(@NonNull final TabItem swipedTabItem) {
        View view = swipedTabItem.getView();
        return Math.abs(view.getX()) > getTabSwitcher().getWidth() / 6f;
    }

    /**
     * Notifies the callback about a swipe gesture, which is used to switch between neighboring
     * tabs.
     *
     * @param tabItem
     *         The tab item, which corresponds to the swiped tab, as an instance of the class {@link
     *         TabItem}. The tab item may not be null
     * @param distance
     *         The distance, the tab is swiped by, in pixels as a {@link Float} value
     */
    private void notifyOnSwitchingBetweenTabs(@NonNull final TabItem tabItem,
                                              final float distance) {
        if (callback != null) {
            callback.onSwitchingBetweenTabs(tabItem, distance);
        }
    }

    /**
     * Notifies the callback, that a swipe gesture, which was used to switch between neighboring
     * tabs, has ended.
     *
     * @param tabItem
     *         The tab item, which corresponds to the swiped tab, as an instance of the class {@link
     *         TabItem}. The tab item may not be null
     * @param switchTabs
     *         True, if the selected tab should be switched, false otherwise
     * @param velocity
     *         The velocity of the swipe gesture in pixels per second as a {@link Float} value
     */
    private void notifyOnSwitchingBetweenTabsEnded(@NonNull final TabItem tabItem,
                                                   final boolean switchTabs, final float velocity) {
        if (callback != null) {
            callback.onSwitchingBetweenTabsEnded(tabItem, switchTabs, velocity);
        }
    }

    /**
     * Creates a new event handler, which allows to handle swipe gestures, which can be used to
     * switch between tabs.
     *
     * @param tabSwitcher
     *         The tab switcher, the event handler belongs to, as an instance of the class {@link
     *         TabSwitcher}. The tab switcher may not be null
     * @param viewRecycler
     *         The view recycler, which allows to inflate the views, which are used to visualize the
     *         tabs of the tab switcher, the event handler belongs to, as an instance of the class
     *         {@link AttachedViewRecycler}. The view recycler may not be null
     * @param dragThreshold
     *         The drag threshold in pixels as an {@link Integer} value. The drag threshold must be
     *         at least 0
     * @param touchableArea
     *         The bounds of the onscreen area, the handler should take into consideration for
     *         handling touch events, as an instance of the class {@link RectF} or null, if the are
     *         should not be restricted
     */
    public SwipeEventHandler(@NonNull final TabSwitcher tabSwitcher,
                             @NonNull final AttachedViewRecycler<AbstractItem, ?> viewRecycler,
                             final int dragThreshold, @Nullable final RectF touchableArea) {
        super(MAX_PRIORITY, touchableArea, tabSwitcher, dragThreshold);
        ensureNotNull(viewRecycler, "The view recycler may not be null");
        this.viewRecycler = viewRecycler;
        ViewConfiguration configuration = ViewConfiguration.get(tabSwitcher.getContext());
        this.maxFlingVelocity = configuration.getScaledMaximumFlingVelocity();
        Resources resources = tabSwitcher.getResources();
        this.minSwipeVelocity = resources.getDimensionPixelSize(R.dimen.min_swipe_velocity);
        this.callback = null;
    }

    /**
     * Sets the callback, which should be notified about the event handler's events.
     *
     * @param callback
     *         The callback, which should be set, as an instance of the type {@link Callback} or
     *         null, if no callback should be notified
     */
    public final void setCallback(@Nullable final Callback callback) {
        this.callback = callback;
    }

    @Override
    protected final boolean isDraggingAllowed() {
        return !getTabSwitcher().isSwitcherShown() && getTabSwitcher().getSelectedTab() != null;
    }

    @Override
    protected final void onTouchEvent() {

    }

    @Override
    protected final void onDown(@NonNull final MotionEvent event) {

    }

    @Override
    protected final void onDrag(@NonNull final MotionEvent event) {
        getDragHelper().update(event.getX());

        if (getDragHelper().hasThresholdBeenReached()) {
            int selectedTabIndex = getTabSwitcher().getSelectedTabIndex();
            swipedTabItem = TabItem.create(getTabSwitcher(), viewRecycler, selectedTabIndex);
            notifyOnSwitchingBetweenTabs(swipedTabItem, getDragHelper().getDragDistance());
        }
    }

    @Override
    protected final void onUp(@Nullable final MotionEvent event) {
        if (swipedTabItem != null) {
            float swipeVelocity = 0;

            if (event != null && getVelocityTracker() != null) {
                int pointerId = event.getPointerId(0);
                getVelocityTracker().computeCurrentVelocity(1000, maxFlingVelocity);
                swipeVelocity = Math.abs(getVelocityTracker().getXVelocity(pointerId));
            }

            boolean remove =
                    (swipeVelocity >= minSwipeVelocity || isSwipeThresholdReached(swipedTabItem));
            notifyOnSwitchingBetweenTabsEnded(swipedTabItem, remove,
                    swipeVelocity >= minSwipeVelocity ? swipeVelocity : 0);
        }

        swipedTabItem = null;
        reset();
    }

}