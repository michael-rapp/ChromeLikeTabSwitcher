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
package de.mrapp.android.tabswitcher;

import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;

import de.mrapp.android.tabswitcher.arithmetic.Arithmetics;
import de.mrapp.android.tabswitcher.iterator.AbstractTabItemIterator;
import de.mrapp.android.tabswitcher.model.Axis;
import de.mrapp.android.tabswitcher.model.DragState;
import de.mrapp.android.tabswitcher.model.Layout;
import de.mrapp.android.tabswitcher.model.State;
import de.mrapp.android.tabswitcher.model.TabItem;
import de.mrapp.android.tabswitcher.util.DragHelper;

import static de.mrapp.android.util.Condition.ensureGreater;
import static de.mrapp.android.util.Condition.ensureNotEqual;
import static de.mrapp.android.util.Condition.ensureNotNull;

/**
 * A drag handler, which allows to calculate the position and state of tabs on touch events.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public class DragHandler {

    /**
     * Defines the interface, a class, which should be notified about the events of a drag handler,
     * must implement.
     */
    public interface Callback {

        /**
         * The method, which is invoked, when a tab has been clicked.
         *
         * @param tabItem
         *         The tab item, which corresponds to the tab, which has been clicked, as an
         *         instance of the class {@link TabItem}. The tab item may not be null
         */
        void onClick(@NonNull TabItem tabItem);

        /**
         * The method, which is invoked, when a fling has been triggered.
         *
         * @param distance
         *         The distance of the fling in pixels as a {@link Float} value
         * @param duration
         *         The duration of the fling in milliseconds as a {@link Long} value
         */
        void onFling(float distance, long duration);

        /**
         * The method, which is invoked, when a fling has been cancelled.
         */
        void onCancelFling();

        /**
         * The method, which is invoked, when an overshoot at the start should be reverted.
         *
         * @param maxAngle
         *         The angle, the tabs may be tilted by at maximum, in degrees as a {@link Float}
         *         value
         */
        void onRevertStartOvershoot(float maxAngle);

        /**
         * The method, which is invoked, when an overshoot at the end should be reverted.
         *
         * @param maxAngle
         *         The angle, the tabs may be tilted by at maximum, in degrees as a {@link Float}
         *         value
         */
        void onRevertEndOvershoot(float maxAngle);

        /**
         * The method, which is invoked, when tabs are overshooting at the start.
         *
         * @param position
         *         The position of the first tab in pixels as a {@link Float} value
         */
        void onStartOvershoot(float position);

        /**
         * The method, which is invoked, when the tabs should be tilted when overshooting at the
         * start.
         *
         * @param angle
         *         The angle, the tabs should be tilted by, in degrees as a {@link Float} value
         */
        void onTiltOnStartOvershoot(float angle);

        /**
         * The method, which is invoked, when the tabs should be tilted when overshooting at the
         * end.
         *
         * @param angle
         *         The angle, the tabs should be tilted by, in degrees as a {@link Float} value
         */
        void onTiltOnEndOvershoot(float angle);

        /**
         * The method, which is invoked, when a tab is swiped.
         *
         * @param tabItem
         *         The tab item, which corresponds to the swiped tab, as an instance of the class
         *         {@link TabItem}. The tab item may not be null
         * @param distance
         *         The distance, the tab is swiped by, in pixels as a {@link Float} value
         */
        void onSwipe(@NonNull TabItem tabItem, float distance);

        /**
         * The method, which is invoked, when swiping a tab ended.
         *
         * @param tabItem
         *         The tab item, which corresponds to the swiped tab, as an instance of the class
         *         {@link TabItem}. The tab item may not be null
         * @param remove
         *         True, if the tab should be removed, false otherwise
         * @param velocity
         *         The velocity of the swipe gesture as a {@link Float} value
         */
        void onSwipeEnded(@NonNull TabItem tabItem, boolean remove, float velocity);

        /**
         * The method, which is invoked, when the position or state of a tab has been changed.
         *
         * @param tabItem
         *         The tab item, which corresponds to the tab, whose position or state has been
         *         changed, as an instance of the class {@link TabItem}. The tab item may not be
         *         null
         */
        void onViewStateChanged(@NonNull TabItem tabItem);

    }

    /**
     * The ratio, which specifies the maximum space between the currently selected tab and its
     * predecessor in relation to the default space.
     */
    private static final float SELECTED_TAB_SPACING_RATIO = 1.5f;

    /**
     * The ratio, which specifies the minimum space between two neighboring tabs in relation to the
     * maximum space.
     */
    private static final float MIN_TAB_SPACING_RATIO = 0.375f;

    /**
     * The tab switcher, whose tabs' positions and states are calculated by the drag handler.
     */
    private final TabSwitcher tabSwitcher;

    /**
     * The arithmetics, which are used to calculate the positions, size and rotation of tabs.
     */
    private final Arithmetics arithmetics;

    /**
     * The drag helper, which is used to recognize drag gestures on the dragging axis.
     */
    private final DragHelper dragHelper;

    /**
     * The drag helper, which is used to recognize drag gestures when overshooting.
     */
    private final DragHelper overshootDragHelper;

    /**
     * The drag helper, which is used to recognize swipe gestures on the orthogonal axis.
     */
    private final DragHelper swipeDragHelper;

    /**
     * The maximum overshoot distance in pixels.
     */
    private final int maxOvershootDistance;

    /**
     * The maximum angle, tabs can be rotated by, when overshooting at the start, in degrees.
     */
    private final float maxStartOvershootAngle;

    /**
     * The maximum angle, tabs can be rotated by, when overshooting at the end, in degrees.
     */
    private final float maxEndOvershootAngle;

    /**
     * The number of tabs, which are contained by a stack.
     */
    private final int stackedTabCount;

    /**
     * The space between tabs, which are part of a stack, in pixels.
     */
    private final int stackedTabSpacing;

    /**
     * The minimum velocity, which must be reached by a drag gesture to start a fling animation.
     */
    private final float minFlingVelocity;

    /**
     * The velocity, which may be reached by a drag gesture at maximum to start a fling animation.
     */
    private final float maxFlingVelocity;

    /**
     * The velocity, which must be reached by a drag gesture in order to start a swipe animation.
     */
    private final float minSwipeVelocity;

    /**
     * The inset of tabs in pixels.
     */
    private final int tabInset;

    /**
     * The threshold, which must be reached until tabs are dragged, in pixels.
     */
    private int dragThreshold;

    /**
     * The velocity tracker, which is used to measure the velocity of dragging gestures.
     */
    private VelocityTracker velocityTracker;

    /**
     * The id of the pointer, which has been used to start the current drag gesture.
     */
    private int pointerId;

    /**
     * The currently swiped tab item.
     */
    private TabItem swipedTabItem;

    /**
     * The state of the currently performed drag gesture.
     */
    private DragState dragState;

    /**
     * The distance of the current drag gesture in pixels.
     */
    private float dragDistance;

    /**
     * The drag distance at which the start overshoot begins.
     */
    private float startOvershootThreshold;

    /**
     * The drag distance at which the end overshoot begins.
     */
    private float endOvershootThreshold;

    /**
     * The index of the first visible tab.
     */
    private int firstVisibleIndex;

    /**
     * The position on the dragging axis, where the distance between a tab and its predecessor
     * should have reached the maximum, in pixels.
     */
    private float attachedPosition;

    /**
     * The maximum space between neighboring tabs in pixels.
     */
    private float maxTabSpacing;

    /**
     * The callback, which is notified about the drag handler's events.
     */
    private Callback callback;

    /**
     * Resets the drag handler to its previous state, when a drag gesture has ended.
     *
     * @param dragThreshold
     *         The drag threshold, which should be used to recognize drag gestures, in pixels as an
     *         {@link Integer} value
     */
    private void resetDragging(final int dragThreshold) {
        if (this.velocityTracker != null) {
            this.velocityTracker.recycle();
            this.velocityTracker = null;
        }

        this.pointerId = -1;
        this.dragState = DragState.NONE;
        this.swipedTabItem = null;
        this.dragDistance = 0;
        this.startOvershootThreshold = -Float.MAX_VALUE;
        this.endOvershootThreshold = Float.MAX_VALUE;
        this.dragThreshold = dragThreshold;
        this.dragHelper.reset(dragThreshold);
        this.overshootDragHelper.reset();
        this.swipeDragHelper.reset();
    }

    /**
     * Handles, when a drag gesture has been started.
     *
     * @param event
     *         The motion event, which started the drag gesture, as an instance of the class {@link
     *         MotionEvent}. The motion event may not be null
     */
    private void handleDown(@NonNull final MotionEvent event) {
        pointerId = event.getPointerId(0);

        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        } else {
            velocityTracker.clear();
        }

        velocityTracker.addMovement(event);
    }

    /**
     * Handles a click.
     *
     * @param factory
     *         The factory, which allows to create builders, which allow to create iterators for
     *         iterating the tabs, as an instance of the type {@link AbstractTabItemIterator.Factory}.
     *         The factory may not be null
     * @param event
     *         The motion event, which triggered the click, as an instance of the class {@link
     *         MotionEvent}. The motion event may not be null
     */
    private void handleClick(@NonNull final AbstractTabItemIterator.Factory factory,
                             @NonNull final MotionEvent event) {
        TabItem tabItem =
                getFocusedTabView(factory, arithmetics.getPosition(Axis.DRAGGING_AXIS, event));

        if (tabItem != null) {
            notifyOnClick(tabItem);
        }
    }

    /**
     * Handles a fling gesture.
     *
     * @param event
     *         The motion event, which triggered the fling gesture, as an instance of the class
     *         {@link MotionEvent}. The motion event may not be null
     * @param dragState
     *         The current drag state, which determines the fling direction, as a value of the enum
     *         {@link DragState}. The drag state may not be null
     */
    private void handleFling(@NonNull final MotionEvent event, @NonNull final DragState dragState) {
        int pointerId = event.getPointerId(0);
        velocityTracker.computeCurrentVelocity(1000, maxFlingVelocity);
        float flingVelocity = Math.abs(velocityTracker.getYVelocity(pointerId));

        if (flingVelocity > minFlingVelocity) {
            float flingDistance = 0.25f * flingVelocity;

            if (dragState == DragState.DRAG_TO_START) {
                flingDistance = -1 * flingDistance;
            }

            long duration = Math.round(Math.abs(flingDistance) / flingVelocity * 1000);
            notifyOnFling(flingDistance, duration);
        }
    }

    /**
     * Calculates the positions of all tabs, depending on the current drag distance.
     *
     * @param factory
     *         The factory, which allows to create builders, which allow to create iterators for
     *         iterating the tabs, as an instance of the type {@link AbstractTabItemIterator.Factory}.
     *         The factory may not be null
     */
    private void calculatePositions(@NonNull final AbstractTabItemIterator.Factory factory) {
        float currentDragDistance = dragHelper.getDragDistance();
        float distance = currentDragDistance - dragDistance;
        dragDistance = currentDragDistance;

        if (distance != 0) {
            if (dragState == DragState.DRAG_TO_END) {
                calculatePositionsWhenDraggingToEnd(factory, distance);
            } else {
                calculatePositionsWhenDraggingToStart(factory, distance);
            }
        }
    }

    /**
     * Calculates the positions of all tabs, when dragging towards the start.
     *
     * @param factory
     *         The factory, which allows to create builders, which allow to create iterators for
     *         iterating the tabs, as an instance of the type {@link AbstractTabItemIterator.Factory}.
     *         The factory may not be null
     * @param dragDistance
     *         The current drag distance in pixels as a {@link Float} value
     */
    private void calculatePositionsWhenDraggingToEnd(
            @NonNull final AbstractTabItemIterator.Factory factory, final float dragDistance) {
        firstVisibleIndex = -1;
        AbstractTabItemIterator.AbstractBuilder builder = factory.create();
        AbstractTabItemIterator iterator = builder.start(Math.max(0, firstVisibleIndex)).create();
        TabItem tabItem;
        boolean abort = false;

        while ((tabItem = iterator.next()) != null && !abort) {
            if (tabSwitcher.getCount() - tabItem.getIndex() > 1) {
                abort = calculatePositionWhenDraggingToEnd(factory, dragDistance, tabItem,
                        iterator.previous());

                if (firstVisibleIndex == -1 && tabItem.getTag().getState() == State.FLOATING) {
                    firstVisibleIndex = tabItem.getIndex();
                }
            } else {
                Pair<Float, State> pair =
                        clipTabPosition(tabSwitcher.getCount(), tabItem.getIndex(),
                                tabItem.getTag().getPosition(), iterator.previous());
                tabItem.getTag().setPosition(pair.first);
                tabItem.getTag().setState(pair.second);
            }

            notifyOnViewStateChanged(tabItem);
        }
    }

    /**
     * Calculates the position of a specific tab, when dragging towards the end.
     *
     * @param factory
     *         The factory, which allows to create builders, which allow to create iterators for
     *         iterating the tabs, as an instance of the type {@link AbstractTabItemIterator.Factory}.
     *         The factory may not be null
     * @param dragDistance
     *         The current drag distance in pixels as a {@link Float} value
     * @param tabItem
     *         The tab item, which corresponds to the tab, whose position should be calculated, as
     *         an instance of the class {@link TabItem}. The tab item may not be null
     * @param predecessor
     *         The predecessor of the given tab item as an instance of the class {@link TabItem} or
     *         null, if the tab item does not have a predecessor
     * @return True, if calculating the position of subsequent tabs can be omitted, false otherwise
     */
    private boolean calculatePositionWhenDraggingToEnd(
            @NonNull final AbstractTabItemIterator.Factory factory, final float dragDistance,
            @NonNull final TabItem tabItem, @Nullable final TabItem predecessor) {
        if (predecessor == null || predecessor.getTag().getState() != State.FLOATING) {
            if ((tabItem.getTag().getState() == State.STACKED_START_ATOP &&
                    tabItem.getIndex() == 0) || tabItem.getTag().getState() == State.FLOATING) {
                float currentPosition = tabItem.getTag().getPosition();
                float thresholdPosition = calculateEndPosition(factory, tabItem);
                float newPosition = Math.min(currentPosition + dragDistance, thresholdPosition);
                Pair<Float, State> pair =
                        clipTabPosition(tabSwitcher.getCount(), tabItem.getIndex(), newPosition,
                                predecessor);
                tabItem.getTag().setPosition(pair.first);
                tabItem.getTag().setState(pair.second);
            } else if (tabItem.getTag().getState() == State.STACKED_START_ATOP) {
                return true;
            }
        } else {
            float thresholdPosition = calculateEndPosition(factory, tabItem);
            float newPosition =
                    Math.min(calculateNonLinearPosition(tabItem, predecessor), thresholdPosition);
            Pair<Float, State> pair =
                    clipTabPosition(tabSwitcher.getCount(), tabItem.getIndex(), newPosition,
                            predecessor);
            tabItem.getTag().setPosition(pair.first);
            tabItem.getTag().setState(pair.second);
        }

        return false;
    }

    /**
     * Calculates the positions of all tabs, when dragging towards the end.
     *
     * @param factory
     *         The factory, which allows to create builders, which allow to create iterators for
     *         iterating the tabs, as an instance of the type {@link AbstractTabItemIterator.Factory}.
     *         The factory may not be null
     * @param dragDistance
     *         The current drag distance in pixels as a {@link Float} value
     */
    private void calculatePositionsWhenDraggingToStart(
            @NonNull final AbstractTabItemIterator.Factory factory, final float dragDistance) {
        AbstractTabItemIterator.AbstractBuilder builder = factory.create();
        AbstractTabItemIterator iterator = builder.start(Math.max(0, firstVisibleIndex)).create();
        TabItem tabItem;
        boolean abort = false;

        while ((tabItem = iterator.next()) != null && !abort) {
            if (tabSwitcher.getCount() - tabItem.getIndex() > 1) {
                abort = calculatePositionWhenDraggingToStart(dragDistance, tabItem,
                        iterator.previous());
            } else {
                Pair<Float, State> pair =
                        clipTabPosition(tabSwitcher.getCount(), tabItem.getIndex(),
                                tabItem.getTag().getPosition(), iterator.previous());
                tabItem.getTag().setPosition(pair.first);
                tabItem.getTag().setState(pair.second);
            }

            notifyOnViewStateChanged(tabItem);
        }

        if (firstVisibleIndex > 0) {
            int start = firstVisibleIndex - 1;
            iterator = builder.reverse(true).start(start).create();
            abort = false;

            while ((tabItem = iterator.next()) != null && !abort) {
                TabItem predecessor = iterator.previous();
                float predecessorPosition = predecessor.getTag().getPosition();
                float newPosition = predecessorPosition +
                        calculateMaxTabSpacing(tabSwitcher.getCount(), predecessor);
                tabItem.getTag().setPosition(newPosition);

                if (tabItem.getIndex() < start) {
                    Pair<Float, State> pair =
                            clipTabPosition(tabSwitcher.getCount(), predecessor.getIndex(),
                                    predecessor.getTag().getPosition(), tabItem);
                    tabItem.getTag().setPosition(pair.first);
                    tabItem.getTag().setState(pair.second);
                    notifyOnViewStateChanged(predecessor);

                    if (predecessor.getTag().getState() == State.FLOATING) {
                        firstVisibleIndex = predecessor.getIndex();
                    } else {
                        abort = true;
                    }
                }

                if (!iterator.hasNext()) {
                    Pair<Float, State> pair =
                            clipTabPosition(tabSwitcher.getCount(), tabItem.getIndex(), newPosition,
                                    null);
                    tabItem.getTag().setPosition(pair.first);
                    tabItem.getTag().setState(pair.second);
                    notifyOnViewStateChanged(tabItem);

                    if (tabItem.getTag().getState() == State.FLOATING) {
                        firstVisibleIndex = tabItem.getIndex();
                    }
                }
            }
        }
    }

    /**
     * Calculates the position of a specific tab, when dragging towards the start.
     *
     * @param dragDistance
     *         The current drag distance in pixels as a {@link Float} value
     * @param tabItem
     *         The tab item, which corresponds to the tab, whose position should be calculated, as
     *         an instance of the class {@link TabItem}. The tab item may not be null
     * @param predecessor
     *         The predecessor of the given tab item as an instance of the class {@link TabItem} or
     *         null, if the tab item does not have a predecessor
     * @return True, if calculating the position of subsequent tabs can be omitted, false otherwise
     */
    private boolean calculatePositionWhenDraggingToStart(final float dragDistance,
                                                         @NonNull final TabItem tabItem,
                                                         @Nullable final TabItem predecessor) {
        if (predecessor == null || predecessor.getTag().getState() != State.FLOATING ||
                predecessor.getTag().getPosition() >
                        getAttachedPosition(false, tabSwitcher.getCount())) {
            if (tabItem.getTag().getState() == State.FLOATING) {
                float currentPosition = tabItem.getTag().getPosition();
                float newPosition = currentPosition + dragDistance;
                Pair<Float, State> pair =
                        clipTabPosition(tabSwitcher.getCount(), tabItem.getIndex(), newPosition,
                                predecessor);
                tabItem.getTag().setPosition(pair.first);
                tabItem.getTag().setState(pair.second);
            } else if (tabItem.getTag().getState() == State.STACKED_START_ATOP) {
                float currentPosition = tabItem.getTag().getPosition();
                Pair<Float, State> pair =
                        clipTabPosition(tabSwitcher.getCount(), tabItem.getIndex(), currentPosition,
                                predecessor);
                tabItem.getTag().setPosition(pair.first);
                tabItem.getTag().setState(pair.second);
                return true;
            } else if (tabItem.getTag().getState() == State.HIDDEN ||
                    tabItem.getTag().getState() == State.STACKED_START) {
                return true;
            }
        } else {
            float newPosition = calculateNonLinearPosition(tabItem, predecessor);
            Pair<Float, State> pair =
                    clipTabPosition(tabSwitcher.getCount(), tabItem.getIndex(), newPosition,
                            predecessor);
            tabItem.getTag().setPosition(pair.first);
            tabItem.getTag().setState(pair.second);
        }

        return false;
    }

    /**
     * Calculates the non-linear position of a tab in relation to position of its predecessor.
     *
     * @param tabItem
     *         The tab item, which corresponds to the tab, whose non-linear position should be
     *         calculated, as an instance of the class {@link TabItem}. The tab item may not be
     *         null
     * @param predecessor
     *         The predecessor as an instance of the class {@link TabItem}. The predecessor may not
     *         be null
     * @return The position, which has been calculated, as a {@link Float} value
     */
    public final float calculateNonLinearPosition(@NonNull final TabItem tabItem,
                                                  @NonNull final TabItem predecessor) {
        float predecessorPosition = predecessor.getTag().getPosition();
        float maxTabSpacing = calculateMaxTabSpacing(tabSwitcher.getCount(), tabItem);
        return calculateNonLinearPosition(predecessorPosition, maxTabSpacing);
    }

    public final float calculateNonLinearPosition(final float predecessorPosition,
                                                  final float maxTabSpacing) {
        float ratio = Math.min(1,
                predecessorPosition / getAttachedPosition(false, tabSwitcher.getCount()));
        float minTabSpacing = calculateMinTabSpacing(tabSwitcher.getCount());
        return predecessorPosition - minTabSpacing - (ratio * (maxTabSpacing - minTabSpacing));
    }

    /**
     * Calculates and returns the position of a specific tab item, when located at the end.
     *
     * @param factory
     *         The factory, which allows to create builders, which allow to create iterators for
     *         iterating the tabs, as an instance of the type {@link AbstractTabItemIterator.Factory}.
     *         The factory may not be null
     * @param tabItem
     *         The tab item, whose position should be calculated, as an instance of the class {@link
     *         TabItem}. The tab item may not be null
     * @return The position, which has been calculated, as a {@link Float} value
     */
    private float calculateEndPosition(@NonNull final AbstractTabItemIterator.Factory factory,
                                       @NonNull final TabItem tabItem) {
        float defaultMaxTabSpacing = calculateMaxTabSpacing(tabSwitcher.getCount(), null);

        if (tabSwitcher.getSelectedTabIndex() > tabItem.getIndex()) {
            AbstractTabItemIterator.AbstractBuilder builder = factory.create();
            AbstractTabItemIterator iterator = builder.create();
            TabItem selectedTabItem = iterator.getItem(tabSwitcher.getSelectedTabIndex());
            float selectedTabSpacing =
                    calculateMaxTabSpacing(tabSwitcher.getCount(), selectedTabItem);
            return (tabSwitcher.getCount() - 2 - tabItem.getIndex()) * defaultMaxTabSpacing +
                    selectedTabSpacing;
        }

        return (tabSwitcher.getCount() - 1 - tabItem.getIndex()) * defaultMaxTabSpacing;
    }

    /**
     * Checks if a drag gesture resulted in overshooting.
     *
     * @param factory
     *         The factory, which allows to create builders, which allow to create iterators for
     *         iterating the tabs, as an instance of the type {@link AbstractTabItemIterator.Factory}.
     *         The factory may not be null
     * @param dragPosition
     *         The position of the pointer on the dragging axis in pixels as a {@link Float} value
     * @return True, if the drag gesture resulted in overshooting, false otherwise
     */
    private boolean checkIfOvershooting(@NonNull final AbstractTabItemIterator.Factory factory,
                                        final float dragPosition) {
        if (isOvershootingAtEnd(factory) &&
                (dragState == DragState.DRAG_TO_END || dragState == DragState.OVERSHOOT_END)) {
            endOvershootThreshold = dragPosition;
            dragState = DragState.OVERSHOOT_END;
            return true;
        } else if (isOvershootingAtStart(factory) &&
                (dragState == DragState.DRAG_TO_START || dragState == DragState.OVERSHOOT_START)) {
            startOvershootThreshold = dragPosition;
            dragState = DragState.OVERSHOOT_START;
            return true;
        }

        return false;
    }

    /**
     * Returns, whether the tabs are overshooting at the start.
     *
     * @param factory
     *         The factory, which allows to create builders, which allow to create iterators for
     *         iterating the tabs, as an instance of the type {@link AbstractTabItemIterator.Factory}.
     *         The factory may not be null
     * @return True, if the tabs are overshooting at the start, false otherwise
     */
    private boolean isOvershootingAtStart(@NonNull final AbstractTabItemIterator.Factory factory) {
        if (tabSwitcher.getCount() <= 1) {
            return true;
        } else {
            AbstractTabItemIterator.AbstractBuilder builder = factory.create();
            AbstractTabItemIterator iterator = builder.create();
            TabItem tabItem = iterator.getItem(0);
            return tabItem.getTag().getState() == State.STACKED_START_ATOP;
        }
    }

    /**
     * Returns, whether the tabs are overshooting at the end.
     *
     * @param factory
     *         The factory, which allows to create builders, which allow to create iterators for
     *         iterating the tabs, as an instance of the type {@link AbstractTabItemIterator.Factory}.
     *         The factory may not be null
     * @return True, if the tabs are overshooting at the end, false otherwise
     */
    public final boolean isOvershootingAtEnd(
            @NonNull final AbstractTabItemIterator.Factory factory) {
        if (tabSwitcher.getCount() <= 1) {
            return true;
        } else {
            AbstractTabItemIterator.AbstractBuilder builder = factory.create();
            AbstractTabItemIterator iterator = builder.create();
            TabItem lastTabItem = iterator.getItem(tabSwitcher.getCount() - 1);
            TabItem predecessor = iterator.getItem(tabSwitcher.getCount() - 2);
            return predecessor.getTag().getPosition() >=
                    calculateMaxTabSpacing(tabSwitcher.getCount(), lastTabItem);
        }
    }

    /**
     * Returns the tab item, which corresponds to the tab, which is focused when clicking/dragging
     * at a specific position.
     *
     * @param factory
     *         The factory, which allows to create builders, which allow to create iterators for
     *         iterating the tabs, as an instance of the type {@link AbstractTabItemIterator.Factory}.
     *         The factory may not be null
     * @param position
     *         The position in pixels as a {@link Float} value
     * @return The tab item, which corresponds to the focused tab, as an instance of the class
     * {@link TabItem} or null, if no tab is focused
     */
    @Nullable
    private TabItem getFocusedTabView(@NonNull final AbstractTabItemIterator.Factory factory,
                                      final float position) {
        AbstractTabItemIterator.AbstractBuilder<?, ?> builder = factory.create();
        AbstractTabItemIterator iterator = builder.start(Math.max(0, firstVisibleIndex)).create();
        TabItem tabItem;

        while ((tabItem = iterator.next()) != null) {
            if (tabItem.getTag().getState() == State.FLOATING ||
                    tabItem.getTag().getState() == State.STACKED_START_ATOP) {
                View view = tabItem.getView();
                float toolbarHeight = tabSwitcher.isToolbarShown() &&
                        tabSwitcher.getLayout() != Layout.PHONE_LANDSCAPE ?
                        tabSwitcher.getToolbar().getHeight() - tabInset : 0;
                float viewPosition =
                        arithmetics.getPosition(Axis.DRAGGING_AXIS, view) + toolbarHeight +
                                arithmetics
                                        .getPadding(Axis.DRAGGING_AXIS, Gravity.START, tabSwitcher);

                if (viewPosition <= position) {
                    return tabItem;
                }
            }
        }

        return null;
    }

    /**
     * Calculates and returns the maximum space between a specific tab and its predecessor. The
     * maximum space is greater for the currently selected tab.
     *
     * @param count
     *         The total number of tabs, which are contained by the tabs switcher, as an {@link
     *         Integer} value
     * @param tabItem
     *         The tab item, which corresponds to the tab, the maximum space should be returned for,
     *         as an instance of the class {@link TabItem} or null, if the default maximum space
     *         should be returned
     * @return The maximum space between the given tab and its predecessor in pixels as a {@link
     * Float} value
     */
    public final float calculateMaxTabSpacing(final int count, @Nullable final TabItem tabItem) {
        ensureNotEqual(maxTabSpacing, -1, "No maximum tab spacing has been set",
                IllegalStateException.class);
        return count > 4 && tabItem != null &&
                tabItem.getIndex() == tabSwitcher.getSelectedTabIndex() ?
                maxTabSpacing * SELECTED_TAB_SPACING_RATIO : maxTabSpacing;
    }

    /**
     * Calculates and returns the minimum space between two neighboring tabs.
     *
     * @param count
     *         The total number of tabs, which are contained by the tabs switcher, as an {@link
     *         Integer} value
     * @return The minimum space between two neighboring tabs in pixels as a {@link Float} value
     */
    private float calculateMinTabSpacing(final int count) {
        return calculateMaxTabSpacing(count, null) * MIN_TAB_SPACING_RATIO;
    }

    /**
     * Notifies the callback, that a tab has been clicked.
     *
     * @param tabItem
     *         The tab item, which corresponds to the tab, which has been clicked, as an instance of
     *         the class {@link TabItem}. The tab item may not be null
     */
    private void notifyOnClick(@NonNull final TabItem tabItem) {
        if (callback != null) {
            callback.onClick(tabItem);
        }
    }

    /**
     * Notifies the callback, that a fling has been triggered.
     *
     * @param distance
     *         The distance of the fling in pixels as a {@link Float} value
     * @param duration
     *         The duration of the fling in milliseconds as a {@link Long} value
     */
    private void notifyOnFling(final float distance, final long duration) {
        if (callback != null) {
            callback.onFling(distance, duration);
        }
    }

    /**
     * Notifies the callback, that a fling has been cancelled.
     */
    private void notifyOnCancelFling() {
        if (callback != null) {
            callback.onCancelFling();
        }
    }

    /**
     * Notifies the callback, that an overshoot at the start should be reverted.
     */
    private void notifyOnRevertStartOvershoot() {
        if (callback != null) {
            callback.onRevertStartOvershoot(maxStartOvershootAngle);
        }
    }

    /**
     * Notifies the callback, that an overshoot at the end should be reverted.
     */
    private void notifyOnRevertEndOvershoot() {
        if (callback != null) {
            callback.onRevertEndOvershoot(maxEndOvershootAngle);
        }
    }

    /**
     * Notifies the callback, that tabs are overshooting at the start.
     *
     * @param position
     *         The position of the first tab in pixels as a {@link Float} value
     */
    private void notifyOnStartOvershoot(final float position) {
        if (callback != null) {
            callback.onStartOvershoot(position);
        }
    }

    /**
     * Notifies the callback, that the tabs should be tilted when overshooting at the start.
     *
     * @param angle
     *         The angle, the tabs should be tilted by, in degrees as a {@link Float} value
     */
    private void notifyOnTiltOnStartOvershoot(final float angle) {
        if (callback != null) {
            callback.onTiltOnStartOvershoot(angle);
        }
    }

    /**
     * Notifies the callback, that the tabs should be titled when overshooting at the end.
     *
     * @param angle
     *         The angle, the tabs should be tilted by, in degrees as a {@link Float} value
     */
    private void notifyOnTiltOnEndOvershoot(final float angle) {
        if (callback != null) {
            callback.onTiltOnEndOvershoot(angle);
        }
    }

    /**
     * Notifies the callback, that a tab is swiped.
     *
     * @param tabItem
     *         The tab item, which corresponds to the swiped tab, as an instance of the class {@link
     *         TabItem}. The tab item may not be null
     * @param distance
     *         The distance, the tab is swiped by, in pixels as a {@link Float} value
     */
    private void notifyOnSwipe(@NonNull final TabItem tabItem, final float distance) {
        if (callback != null) {
            callback.onSwipe(tabItem, distance);
        }
    }

    /**
     * Notifies the callback, that swiping a tab ended.
     *
     * @param tabItem
     *         The tab item, which corresponds to the swiped tab, as an instance of the class {@link
     *         TabItem}. The tab item may not be null
     * @param remove
     *         True, if the tab should be removed, false otherwise
     * @param velocity
     *         The velocity of the swipe gesture as a {@link Float} value
     */
    private void notifyOnSwipeEnded(@NonNull final TabItem tabItem, final boolean remove,
                                    final float velocity) {
        if (callback != null) {
            callback.onSwipeEnded(tabItem, remove, velocity);
        }
    }

    /**
     * Notifies, that the position or state of a tab has been changed.
     *
     * @param tabItem
     *         The tab item, which corresponds to the tab, whose position or state has been changed,
     *         as an instance of the class {@link TabItem}. The tab item may not be null
     */
    private void notifyOnViewStateChanged(@NonNull final TabItem tabItem) {
        if (callback != null) {
            callback.onViewStateChanged(tabItem);
        }
    }

    public DragHandler(@NonNull final TabSwitcher tabSwitcher,
                       @NonNull final Arithmetics arithmetics) {
        ensureNotNull(tabSwitcher, "The tab switcher may not be null");
        ensureNotNull(arithmetics, "The arithmetics may not be null");
        this.tabSwitcher = tabSwitcher;
        this.arithmetics = arithmetics;
        this.dragHelper = new DragHelper(0);
        this.overshootDragHelper = new DragHelper(0);
        Resources resources = tabSwitcher.getResources();
        this.swipeDragHelper =
                new DragHelper(resources.getDimensionPixelSize(R.dimen.swipe_threshold));
        this.tabInset = resources.getDimensionPixelSize(R.dimen.tab_inset);
        this.stackedTabCount = resources.getInteger(R.integer.stacked_tab_count);
        this.stackedTabSpacing = resources.getDimensionPixelSize(R.dimen.stacked_tab_spacing);
        this.maxOvershootDistance = resources.getDimensionPixelSize(R.dimen.max_overshoot_distance);
        this.maxStartOvershootAngle = resources.getInteger(R.integer.max_start_overshoot_angle);
        this.maxEndOvershootAngle = resources.getInteger(R.integer.max_end_overshoot_angle);
        ViewConfiguration configuration = ViewConfiguration.get(tabSwitcher.getContext());
        this.minFlingVelocity = configuration.getScaledMinimumFlingVelocity();
        this.maxFlingVelocity = configuration.getScaledMaximumFlingVelocity();
        this.minSwipeVelocity = resources.getDimensionPixelSize(R.dimen.min_swipe_velocity);
        this.maxTabSpacing = -1;
        reset(dragThreshold);
    }

    /**
     * Sets the callback, which should be notified, about the drag handler's events.
     *
     * @param callback
     *         The callback, which should be set, as an instance of the type {@link Callback} or
     *         null, if no callback should be notified
     */
    public final void setCallback(@Nullable final Callback callback) {
        this.callback = callback;
    }

    /**
     * Sets the maximum space between two neighboring tabs.
     *
     * @param maxTabSpacing
     *         The maximum space, which should be set, in pixels as a {@link Float} value
     */
    public final void setMaxTabSpacing(final float maxTabSpacing) {
        ensureGreater(maxTabSpacing, 0, "The maximum tab spacing must be greater than 0");
        this.maxTabSpacing = maxTabSpacing;
    }

    /**
     * Calculates and returns the position on the dragging axis, where the distance between a tab
     * and its predecessor should have reached the maximum.
     *
     * @param recalculate
     *         True, if the position should be forced to be recalculated, false otherwise
     * @param count
     *         The total number of tabs, which are contained by the tabs switcher, as an {@link
     *         Integer} value
     * @return The position, which has been calculated, in pixels as an {@link Float} value
     */
    public final float getAttachedPosition(final boolean recalculate, final int count) {
        if (recalculate || attachedPosition == -1) {
            float totalSpace =
                    arithmetics.getSize(Axis.DRAGGING_AXIS, tabSwitcher.getTabContainer()) -
                            (tabSwitcher.getLayout() == Layout.PHONE_PORTRAIT &&
                                    tabSwitcher.isToolbarShown() ?
                                    tabSwitcher.getToolbar().getHeight() + tabInset : 0);

            if (count == 3) {
                attachedPosition = totalSpace * 0.66f;
            } else if (count == 4) {
                attachedPosition = totalSpace * 0.75f;
            } else {
                attachedPosition = totalSpace * 0.5f;
            }
        }

        return attachedPosition;
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
    public final Pair<Float, State> clipTabPosition(final int count, final int index,
                                                    final float position,
                                                    @Nullable final TabItem predecessor) {
        Pair<Float, State> startPair =
                calculatePositionAndStateWhenStackedAtStart(count, index, predecessor);
        float startPosition = startPair.first;

        if (position <= startPosition) {
            State state = startPair.second;
            return Pair.create(startPosition, state);
        } else {
            Pair<Float, State> endPair = calculatePositionAndStateWhenStackedAtEnd(index);
            float endPosition = endPair.first;

            if (position >= endPosition) {
                State state = endPair.second;
                return Pair.create(endPosition, state);
            } else {
                State state = State.FLOATING;
                return Pair.create(position, state);
            }
        }
    }

    /**
     * Calculates and returns the position and state of a specific tab, when stacked at the start.
     *
     * @param count
     *         The total number of tabs, which are currently contained by the tab switcher, as an
     *         {@link Integer} value
     * @param index
     *         The index of the tab, whose position and state should be returned, as an {@link
     *         Integer} value
     * @param predecessor
     *         The predecessor of the given tab item as an instance of the class {@link TabItem} or
     *         null, if the tab item does not have a predecessor
     * @return A pair, which contains the position and state of the given tab item, when stacked at
     * the start, as an instance of the class {@link Pair}. The pair may not be null
     */
    @NonNull
    public final Pair<Float, State> calculatePositionAndStateWhenStackedAtStart(final int count,
                                                                                final int index,
                                                                                @Nullable final TabItem predecessor) {
        if ((count - index) <= stackedTabCount) {
            float position = stackedTabSpacing * (count - (index + 1));
            return Pair.create(position,
                    (predecessor == null || predecessor.getTag().getState() == State.FLOATING) ?
                            State.STACKED_START_ATOP : State.STACKED_START);
        } else {
            float position = stackedTabSpacing * stackedTabCount;
            return Pair.create(position,
                    (predecessor == null || predecessor.getTag().getState() == State.FLOATING) ?
                            State.STACKED_START_ATOP : State.HIDDEN);
        }
    }

    /**
     * Calculates and returns the position and state of a specific tab, when stacked at the end.
     *
     * @param index
     *         The index of the tab, whose position and state should be returned, as an {@link
     *         Integer} value
     * @return A pair, which contains the position and state of the given tab item, when stacked at
     * the end, as an instance of the class {@link Pair}. The pair may not be null
     */
    @NonNull
    public final Pair<Float, State> calculatePositionAndStateWhenStackedAtEnd(final int index) {
        float size = arithmetics.getSize(Axis.DRAGGING_AXIS, tabSwitcher.getTabContainer());
        int toolbarHeight =
                tabSwitcher.isToolbarShown() && tabSwitcher.getLayout() != Layout.PHONE_LANDSCAPE ?
                        tabSwitcher.getToolbar().getHeight() - tabInset : 0;
        int padding = arithmetics.getPadding(Axis.DRAGGING_AXIS, Gravity.START, tabSwitcher) +
                arithmetics.getPadding(Axis.DRAGGING_AXIS, Gravity.END, tabSwitcher);
        int offset = tabSwitcher.getLayout() == Layout.PHONE_LANDSCAPE ?
                stackedTabCount * stackedTabSpacing : 0;

        if (index < stackedTabCount) {
            float position =
                    size - toolbarHeight - tabInset - (stackedTabSpacing * (index + 1)) - padding +
                            offset;
            return Pair.create(position, State.STACKED_END);
        } else {
            float position =
                    size - toolbarHeight - tabInset - (stackedTabSpacing * stackedTabCount) -
                            padding + offset;
            return Pair.create(position, State.HIDDEN);
        }
    }

    /**
     * Handles a touch event.
     *
     * @param factory
     *         The factory, which should be used to create builders, which allow to configure and
     *         create the iterators for iterating the tabs, whose positions and states should be
     *         calculated, as an instance of the type {@link AbstractTabItemIterator.Factory}. The
     *         factory may not be null
     * @param event
     *         The event, which should be handled, as an instance of the class {@link MotionEvent}.
     *         The event may be not null
     * @return True, if the event has been handled, false otherwise
     */
    public final boolean handleTouchEvent(@NonNull final AbstractTabItemIterator.Factory factory,
                                          @NonNull final MotionEvent event) {
        ensureNotNull(factory, "The factory may not be null");
        ensureNotNull(event, "The motion event may not be null");

        if (tabSwitcher.isSwitcherShown() && !tabSwitcher.isEmpty()) {
            notifyOnCancelFling();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    handleDown(event);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (!tabSwitcher.isAnimationRunning() && event.getPointerId(0) == pointerId) {
                        if (velocityTracker == null) {
                            velocityTracker = VelocityTracker.obtain();
                        }

                        velocityTracker.addMovement(event);
                        handleDrag(factory, arithmetics.getPosition(Axis.DRAGGING_AXIS, event),
                                arithmetics.getPosition(Axis.ORTHOGONAL_AXIS, event));
                    } else {
                        handleRelease(factory, null, dragThreshold);
                        handleDown(event);
                    }

                    return true;
                case MotionEvent.ACTION_UP:
                    if (!tabSwitcher.isAnimationRunning() && event.getPointerId(0) == pointerId) {
                        handleRelease(factory, event, dragThreshold);
                    }

                    return true;
                default:
                    break;
            }
        }

        return false;
    }

    /**
     * Handles drag gestures.
     *
     * @param factory
     *         The factory, which should be used to create builders, which allow to configure and
     *         create the iterators for iterating the tabs, whose positions and states should be
     *         calculated, as an instance of the type {@link AbstractTabItemIterator.Factory}. The
     *         factory may not be null
     * @param dragPosition
     *         The position of the pointer on the dragging axis in pixels as a {@link Float} value
     * @param orthogonalPosition
     *         The position of the pointer of the orthogonal axis in pixels as a {@link Float}
     *         value
     * @return True, if any tabs have been moved, false otherwise
     */
    public final boolean handleDrag(@NonNull final AbstractTabItemIterator.Factory factory,
                                    final float dragPosition, final float orthogonalPosition) {
        ensureNotNull(factory, "The factory may not be null");

        if (dragPosition <= startOvershootThreshold) {
            if (!dragHelper.isReset()) {
                dragHelper.reset(0);
                dragDistance = 0;
            }

            dragState = DragState.OVERSHOOT_START;
            overshootDragHelper.update(dragPosition);
            float overshootDistance = overshootDragHelper.getDragDistance();

            if (overshootDistance < 0) {
                float absOvershootDistance = Math.abs(overshootDistance);
                float startOvershootDistance =
                        tabSwitcher.getCount() >= stackedTabCount ? maxOvershootDistance :
                                (tabSwitcher.getCount() > 1 ? (float) maxOvershootDistance /
                                        (float) tabSwitcher.getCount() : 0);

                if (absOvershootDistance <= startOvershootDistance) {
                    float ratio =
                            Math.max(0, Math.min(1, absOvershootDistance / startOvershootDistance));
                    AbstractTabItemIterator.AbstractBuilder builder = factory.create();
                    AbstractTabItemIterator iterator = builder.create();
                    TabItem tabItem = iterator.getItem(0);
                    float currentPosition = tabItem.getTag().getPosition();
                    float position = currentPosition - (currentPosition * ratio);
                    notifyOnStartOvershoot(position);
                } else {
                    float ratio =
                            (absOvershootDistance - startOvershootDistance) / maxOvershootDistance;

                    if (ratio >= 1) {
                        overshootDragHelper.setMinDragDistance(overshootDistance);
                        startOvershootThreshold =
                                dragPosition + maxOvershootDistance + startOvershootDistance;
                    }

                    notifyOnTiltOnStartOvershoot(
                            Math.max(0, Math.min(1, ratio)) * maxStartOvershootAngle);
                }
            }
        } else if (dragPosition >= endOvershootThreshold) {
            if (!dragHelper.isReset()) {
                dragHelper.reset(0);
                dragDistance = 0;
            }

            dragState = DragState.OVERSHOOT_END;
            overshootDragHelper.update(dragPosition);
            float overshootDistance = overshootDragHelper.getDragDistance();
            float ratio = overshootDistance / maxOvershootDistance;

            if (ratio >= 1) {
                overshootDragHelper.setMaxDragDistance(overshootDistance);
                endOvershootThreshold = dragPosition - maxOvershootDistance;
            }

            notifyOnTiltOnEndOvershoot(Math.max(0, Math.min(1, ratio)) *
                    -(tabSwitcher.getCount() > 1 ? maxEndOvershootAngle : maxStartOvershootAngle));
        } else {
            overshootDragHelper.reset();
            float previousDistance = dragHelper.isReset() ? 0 : dragHelper.getDragDistance();
            dragHelper.update(dragPosition);
            swipeDragHelper.update(orthogonalPosition);

            if (dragState == DragState.NONE && swipedTabItem == null &&
                    swipeDragHelper.hasThresholdBeenReached()) {
                TabItem tabItem = getFocusedTabView(factory, dragHelper.getDragStartPosition());

                if (tabItem != null && tabItem.getTab().isCloseable()) {
                    swipedTabItem = tabItem;
                }
            }

            if (swipedTabItem == null && dragHelper.hasThresholdBeenReached()) {
                if (dragState == DragState.OVERSHOOT_START) {
                    dragState = DragState.DRAG_TO_END;
                } else if (dragState == DragState.OVERSHOOT_END) {
                    dragState = DragState.DRAG_TO_START;
                } else {
                    float dragDistance = dragHelper.getDragDistance();

                    if (dragDistance == 0) {
                        dragState = DragState.NONE;
                    } else {
                        dragState = previousDistance - dragDistance < 0 ? DragState.DRAG_TO_END :
                                DragState.DRAG_TO_START;
                    }
                }
            }

            if (swipedTabItem != null) {
                notifyOnSwipe(swipedTabItem, swipeDragHelper.getDragDistance());
            } else if (dragState != DragState.NONE) {
                calculatePositions(factory);
                checkIfOvershooting(factory, dragPosition);
                return true;
            }
        }

        return false;
    }

    /**
     * Handles, when a drag gesture has been ended.
     *
     * @param factory
     *         The factory, which should be used to create builders, which allow to configure and
     *         create the iterators for iterating the tabs, whose positions and states should be
     *         calculated, as an instance of the type {@link AbstractTabItemIterator.Factory}. The
     *         factory may not be null
     * @param event
     *         The motion event, which ended the drag gesture, as an instance of the class {@link
     *         MotionEvent} or null, if no fling animation should be triggered
     * @param dragThreshold
     *         The drag threshold, which should be used to recognize drag gestures, in pixels as an
     *         {@link Integer} value
     */
    public final void handleRelease(@NonNull final AbstractTabItemIterator.Factory factory,
                                    @Nullable final MotionEvent event, final int dragThreshold) {
        ensureNotNull(factory, "The factory may not be null");

        if (swipedTabItem != null) {
            float swipeVelocity = 0;

            if (event != null && velocityTracker != null) {
                int pointerId = event.getPointerId(0);
                velocityTracker.computeCurrentVelocity(1000, maxFlingVelocity);
                swipeVelocity = Math.abs(velocityTracker.getXVelocity(pointerId));
            }

            View view = swipedTabItem.getView();
            boolean remove = swipeVelocity >= minSwipeVelocity ||
                    Math.abs(arithmetics.getPosition(Axis.ORTHOGONAL_AXIS, view)) >
                            arithmetics.getSize(Axis.ORTHOGONAL_AXIS, view) / 4f;
            notifyOnSwipeEnded(swipedTabItem, remove,
                    swipeVelocity > minSwipeVelocity ? swipeVelocity : 0);
            swipedTabItem = null;
        } else if (dragState == DragState.DRAG_TO_START || dragState == DragState.DRAG_TO_END) {
            if (event != null && velocityTracker != null && dragHelper.hasThresholdBeenReached()) {
                handleFling(event, dragState);
            }
        } else if (dragState == DragState.OVERSHOOT_END) {
            notifyOnRevertEndOvershoot();
        } else if (dragState == DragState.OVERSHOOT_START) {
            notifyOnRevertStartOvershoot();
        } else if (event != null && !dragHelper.hasThresholdBeenReached() &&
                !swipeDragHelper.hasThresholdBeenReached()) {
            handleClick(factory, event);
        }

        resetDragging(dragThreshold);
    }

    /**
     * Resets the drag handler to its initial state.
     *
     * @param dragThreshold
     *         The drag threshold, which should be used to recognize drag gestures, in pixels as an
     *         {@link Integer} value
     */
    public final void reset(final int dragThreshold) {
        resetDragging(dragThreshold);
        firstVisibleIndex = -1;
        attachedPosition = -1;

    }

    public final int getFirstVisibleIndex() {
        return firstVisibleIndex;
    }

    public final void setFirstVisibleIndex(final int index) {
        this.firstVisibleIndex = index;
    }

}