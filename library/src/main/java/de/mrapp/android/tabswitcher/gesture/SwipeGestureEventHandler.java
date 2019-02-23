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
package de.mrapp.android.tabswitcher.gesture;

import android.content.res.Resources;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.mrapp.android.tabswitcher.Layout;
import de.mrapp.android.tabswitcher.R;
import de.mrapp.android.tabswitcher.TabSwitcher;

/**
 * An event handler, which allows to handle swipe gestures, which can be used to switch between
 * tabs.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public class SwipeGestureEventHandler extends AbstractDragGestureEventHandler {

    /**
     * Defines the interface, a class, which should be notified about the events of a {@link
     * SwipeGestureEventHandler}, must implement.
     */
    public interface Callback {

        /**
         * The method, which is invoked, when switching between neighboring tabs.
         *
         * @param selectedTabIndex
         *         The index of the currently selected tab as an {@link Integer} value
         * @param distance
         *         The distance, the currently selected tab is swiped by, in pixels as a {@link
         *         Float} value
         */
        void onSwitchingBetweenTabs(int selectedTabIndex, float distance);

        /**
         * The method, which is invoked, when switching between neighboring tabs ended.
         *
         * @param selectedTabIndex
         *         The index of the tab, which should become selected, as an {@link Integer} value
         * @param previousSelectedTabIndex
         *         The index of the previously selected tab as an {@link Integer} value
         * @param selectionChanged
         *         True, if the selection has changed, false otherwise
         * @param velocity
         *         The velocity of the swipe gesture in pixels per second as a {@link Float} value
         * @param animationDuration
         *         The duration of the swipe animation in milliseconds as a {@link Long} value
         */
        void onSwitchingBetweenTabsEnded(int selectedTabIndex, int previousSelectedTabIndex,
                                         boolean selectionChanged, float velocity,
                                         long animationDuration);

    }

    /**
     * The velocity, which may be reached by a drag gesture at maximum to start a fling animation.
     */
    private final float maxFlingVelocity;

    /**
     * The velocity, which must be reached by a drag gesture in order to start a swipe animation.
     */
    private final float minSwipeVelocity;

    /**
     * The duration of the swipe animation in milliseconds.
     */
    private final long swipeAnimationDuration;

    /**
     * The distance between two neighboring tabs when being swiped in pixels.
     */
    private final int swipedTabDistance;

    /**
     * The index of the currently selected tab.
     */
    private int selectedTabIndex;

    /**
     * The callback, which is notified about the event handler's events.
     */
    private Callback callback;

    /**
     * Returns, whether the threshold of a swiped tab item, which causes the previous or next tab to
     * be selected, has been reached, or not.
     *
     * @return True, if the threshold has been reached, false otherwise
     */
    private boolean isSwipeThresholdReached() {
        return Math.abs(getDragHelper().getDragDistance()) > 4 * swipedTabDistance;
    }

    /**
     * Notifies the callback about a swipe gesture, which is used to switch between neighboring
     * tabs.
     *
     * @param selectedTabIndex
     *         The index of the currently selected tab as an {@link Integer} value
     * @param distance
     *         The distance, the currently selected tab is swiped by, in pixels as a {@link Float}
     *         value
     */
    private void notifyOnSwitchingBetweenTabs(final int selectedTabIndex, final float distance) {
        if (callback != null) {
            callback.onSwitchingBetweenTabs(selectedTabIndex, distance);
        }
    }

    /**
     * Notifies the callback, that a swipe gesture, which was used to switch between neighboring
     * tabs, has ended.
     *
     * @param selectedTabIndex
     *         The index of the tab, which should become selected, as an {@link Integer} value
     * @param previousSelectedTabIndex
     *         The index of the previously selected tab as an {@link Integer} value
     * @param selectionChanged
     *         True, if the selection has changed, false otherwise
     * @param velocity
     *         The velocity of the swipe gesture in pixels per second as a {@link Float} value
     */
    private void notifyOnSwitchingBetweenTabsEnded(final int selectedTabIndex,
                                                   final int previousSelectedTabIndex,
                                                   final boolean selectionChanged,
                                                   final float velocity) {
        if (callback != null) {
            callback.onSwitchingBetweenTabsEnded(selectedTabIndex, previousSelectedTabIndex,
                    selectionChanged, velocity, swipeAnimationDuration);
        }
    }

    /**
     * Creates a new event handler, which allows to handle swipe gestures, which can be used to
     * switch between tabs.
     *
     * @param tabSwitcher
     *         The tab switcher, the event handler belongs to, as an instance of the class {@link
     *         TabSwitcher}. The tab switcher may not be null
     * @param dragThreshold
     *         The drag threshold in pixels as an {@link Integer} value. The drag threshold must be
     *         at least 0
     * @param touchableArea
     *         The bounds of the onscreen area, the handler should take into consideration for
     *         handling touch events, as an instance of the class {@link RectF} or null, if the are
     *         should not be restricted
     * @param animationDuration
     *         The duration of the swipe animation in milliseconds as a {@link Long} value or -1, if
     *         the default duration should be used
     */
    public SwipeGestureEventHandler(@NonNull final TabSwitcher tabSwitcher, final int dragThreshold,
                                    @Nullable final RectF touchableArea,
                                    final long animationDuration) {
        super(tabSwitcher, dragThreshold, touchableArea);
        ViewConfiguration configuration = ViewConfiguration.get(tabSwitcher.getContext());
        this.maxFlingVelocity = configuration.getScaledMaximumFlingVelocity();
        Resources resources = tabSwitcher.getResources();
        this.minSwipeVelocity = resources.getDimensionPixelSize(R.dimen.min_swipe_velocity);
        this.swipeAnimationDuration = animationDuration != -1 ? animationDuration :
                resources.getInteger(R.integer.swipe_animation_duration);
        this.swipedTabDistance = resources.getDimensionPixelSize(R.dimen.swiped_tab_distance);
        this.callback = null;
        this.selectedTabIndex = -1;
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
        return (getTabSwitcher().getLayout() == Layout.TABLET ||
                !getTabSwitcher().isSwitcherShown()) && getTabSwitcher().getSelectedTab() != null;
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
            if (selectedTabIndex == -1) {
                selectedTabIndex = getTabSwitcher().getSelectedTabIndex();
            }

            notifyOnSwitchingBetweenTabs(selectedTabIndex, getDragHelper().getDragDistance());
        }
    }

    @Override
    protected final void onUp(@Nullable final MotionEvent event) {
        if (selectedTabIndex != -1) {
            float swipeVelocity = 0;

            if (event != null && getVelocityTracker() != null) {
                int pointerId = event.getPointerId(0);
                getVelocityTracker().computeCurrentVelocity(1000, maxFlingVelocity);
                swipeVelocity = Math.abs(getVelocityTracker().getXVelocity(pointerId));
            }

            int index = selectedTabIndex;
            boolean selectionChanged = false;

            if (swipeVelocity >= minSwipeVelocity || isSwipeThresholdReached()) {
                index = getDragHelper().getDragDistance() > 0 ? selectedTabIndex + 1 :
                        selectedTabIndex - 1;
                selectionChanged = true;
                index = Math.max(Math.min(index, getTabSwitcher().getCount() - 1), 0);
            }

            notifyOnSwitchingBetweenTabsEnded(index, selectedTabIndex, selectionChanged,
                    swipeVelocity >= minSwipeVelocity ? swipeVelocity : 0);
        }

        selectedTabIndex = -1;
        reset();
    }

}