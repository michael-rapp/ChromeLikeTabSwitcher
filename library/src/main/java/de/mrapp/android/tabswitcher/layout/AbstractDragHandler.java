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
package de.mrapp.android.tabswitcher.layout;

import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;

import de.mrapp.android.tabswitcher.R;
import de.mrapp.android.tabswitcher.TabSwitcher;
import de.mrapp.android.tabswitcher.layout.Arithmetics.Axis;
import de.mrapp.android.tabswitcher.model.TabItem;
import de.mrapp.android.util.gesture.DragHelper;

import static de.mrapp.android.util.Condition.ensureNotNull;

/**
 * An abstract base class for all drag handlers, which allow to calculate the position and state of
 * tabs on touch events.
 *
 * @param <CallbackType>
 *         The type of the drag handler's callback
 * @author Michael Rapp
 * @since 0.1.0
 */
public abstract class AbstractDragHandler<CallbackType extends AbstractDragHandler.Callback> {

    /**
     * Contains all possible states of dragging gestures, which can be performed on a {@link
     * TabSwitcher}.
     */
    public enum DragState {

        /**
         * When no dragging gesture is being performed.
         */
        NONE,

        /**
         * When the tabs are dragged towards the start.
         */
        DRAG_TO_START,

        /**
         * When the tabs are dragged towards the end.
         */
        DRAG_TO_END,

        /**
         * When an overshoot at the start is being performed.
         */
        OVERSHOOT_START,

        /**
         * When an overshoot at the end is being performed.
         */
        OVERSHOOT_END,

        /**
         * When a tab is swiped.
         */
        SWIPE

    }

    /**
     * Defines the interface, a class, which should be notified about the events of a drag handler,
     * must implement.
     */
    public interface Callback {

        /**
         * The method, which is invoked in order to calculate the positions of all tabs, depending
         * on the current drag distance.
         *
         * @param dragState
         *         The current drag state as a value of the enum {@link DragState}. The drag state
         *         must either be {@link DragState#DRAG_TO_END} or {@link DragState#DRAG_TO_START}
         * @param dragDistance
         *         The current drag distance in pixels as a {@link Float} value
         * @return A drag state, which specifies whether the tabs are overshooting, or not. If the
         * tabs are overshooting, the drag state must be {@link DragState#OVERSHOOT_START} or {@link
         * DragState#OVERSHOOT_END}, null otherwise
         */
        @Nullable
        DragState onDrag(@NonNull DragState dragState, float dragDistance);

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
         */
        void onRevertStartOvershoot();

        /**
         * The method, which is invoked, when an overshoot at the end should be reverted.
         */
        void onRevertEndOvershoot();

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
         *         The velocity of the swipe gesture in pixels per second as a {@link Float} value
         */
        void onSwipeEnded(@NonNull TabItem tabItem, boolean remove, float velocity);

    }

    /**
     * The tab switcher, whose tabs' positions and states are calculated by the drag handler.
     */
    private final TabSwitcher tabSwitcher;

    /**
     * The arithmetics, which are used to calculate the positions, size and rotation of tabs.
     */
    private final Arithmetics arithmetics;

    /**
     * True, if tabs can be swiped on the orthogonal axis, false otherwise.
     */
    private final boolean swipeEnabled;

    /**
     * The drag helper, which is used to recognize drag gestures on the dragging axis.
     */
    private final DragHelper dragHelper;

    /**
     * The drag helper, which is used to recognize swipe gestures on the orthogonal axis.
     */
    private final DragHelper swipeDragHelper;

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
     * The callback, which is notified about the drag handler's events.
     */
    private CallbackType callback;

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
     * @param event
     *         The motion event, which triggered the click, as an instance of the class {@link
     *         MotionEvent}. The motion event may not be null
     */
    private void handleClick(@NonNull final MotionEvent event) {
        TabItem tabItem = getFocusedTab(arithmetics.getPosition(Axis.DRAGGING_AXIS, event));

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
     * Handles, when the tabs are overshooting.
     */
    private void handleOvershoot() {
        if (!dragHelper.isReset()) {
            dragHelper.reset(0);
            dragDistance = 0;
        }
    }

    /**
     * Notifies the callback in order to calculate the positions of all tabs, depending on the
     * current drag distance.
     *
     * @param dragState
     *         The current drag state as a value of the enum {@link DragState}. The drag state must
     *         either be {@link DragState#DRAG_TO_END} or {@link DragState#DRAG_TO_START}
     * @param dragDistance
     *         The current drag distance in pixels as a {@link Float} value
     * @return A drag state, which specifies whether the tabs are overshooting, or not. If the tabs
     * are overshooting, the drag state must be {@link DragState#OVERSHOOT_START} or {@link
     * DragState#OVERSHOOT_END}, null otherwise
     */
    private DragState notifyOnDrag(@NonNull final DragState dragState, final float dragDistance) {
        if (callback != null) {
            return callback.onDrag(dragState, dragDistance);
        }

        return null;
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
            callback.onRevertStartOvershoot();
        }
    }

    /**
     * Notifies the callback, that an overshoot at the end should be reverted.
     */
    private void notifyOnRevertEndOvershoot() {
        if (callback != null) {
            callback.onRevertEndOvershoot();
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
     *         The velocity of the swipe gesture in pixels per second as a {@link Float} value
     */
    private void notifyOnSwipeEnded(@NonNull final TabItem tabItem, final boolean remove,
                                    final float velocity) {
        if (callback != null) {
            callback.onSwipeEnded(tabItem, remove, velocity);
        }
    }

    /**
     * Returns the tab switcher, whose tabs' positions and states are calculated by the drag
     * handler.
     *
     * @return The tab switcher, whose tabs' positions and states are calculated by the drag
     * handler, as an instance of the class {@link TabSwitcher}. The tab switcher may not be null
     */
    @NonNull
    protected TabSwitcher getTabSwitcher() {
        return tabSwitcher;
    }

    /**
     * Returns the arithmetics, which are used to calculate the positions, size and rotation of
     * tabs.
     *
     * @return The arithmetics, which are used to calculate the positions, size and rotation of
     * tabs, as an instance of the type {@link Arithmetics}. The arithmetics may not be null
     */
    @NonNull
    protected Arithmetics getArithmetics() {
        return arithmetics;
    }

    /**
     * Returns the callback, which should be notified about the drag handler's events.
     *
     * @return The callback, which should be notified about the drag handler's events, as an
     * instance of the generic type CallbackType or null, if no callback should be notified
     */
    @Nullable
    protected CallbackType getCallback() {
        return callback;
    }

    /**
     * Creates a new drag handler, which allows to calculate the position and state of tabs on touch
     * events.
     *
     * @param tabSwitcher
     *         The tab switcher, whose tabs' positions and states should be calculated by the drag
     *         handler, as an instance of the class {@link TabSwitcher}. The tab switcher may not be
     *         null
     * @param arithmetics
     *         The arithmetics, which should be used to calculate the position, size and rotation of
     *         tabs, as an instance of the type {@link Arithmetics}. The arithmetics may not be
     *         null
     * @param swipeEnabled
     *         True, if tabs can be swiped on the orthogonal axis, false otherwise
     */
    public AbstractDragHandler(@NonNull final TabSwitcher tabSwitcher,
                               @NonNull final Arithmetics arithmetics, final boolean swipeEnabled) {
        ensureNotNull(tabSwitcher, "The tab switcher may not be null");
        ensureNotNull(arithmetics, "The arithmetics may not be null");
        this.tabSwitcher = tabSwitcher;
        this.arithmetics = arithmetics;
        this.swipeEnabled = swipeEnabled;
        this.dragHelper = new DragHelper(0);
        Resources resources = tabSwitcher.getResources();
        this.swipeDragHelper =
                new DragHelper(resources.getDimensionPixelSize(R.dimen.swipe_threshold));
        this.callback = null;
        ViewConfiguration configuration = ViewConfiguration.get(tabSwitcher.getContext());
        this.minFlingVelocity = configuration.getScaledMinimumFlingVelocity();
        this.maxFlingVelocity = configuration.getScaledMaximumFlingVelocity();
        this.minSwipeVelocity = resources.getDimensionPixelSize(R.dimen.min_swipe_velocity);
        resetDragging(resources.getDimensionPixelSize(R.dimen.drag_threshold));
    }

    /**
     * The method, which is invoked on implementing subclasses in order to retrieve the tab item,
     * which corresponds to the tab, which is focused when clicking/dragging at a specific position.
     *
     * @param position
     *         The position on the dragging axis in pixels as a {@link Float} value
     * @return The tab item, which corresponds to the focused tab, as an instance of the class
     * {@link TabItem} or null, if no tab is focused
     */
    protected abstract TabItem getFocusedTab(final float position);

    /**
     * The method, which is invoked on implementing subclasses, when the tabs are overshooting at
     * the start.
     *
     * @param dragPosition
     *         The position of the pointer on the dragging axis in pixels as a {@link Float} value
     * @param overshootThreshold
     *         The position on the dragging axis, an overshoot at the start currently starts at, in
     *         pixels as a {@link Float} value
     * @return The updated position on the dragging axis, an overshoot at the start starts at, in
     * pixels as a {@link Float} value
     */
    protected float onOvershootStart(final float dragPosition, final float overshootThreshold) {
        return overshootThreshold;
    }

    /**
     * The method, which is invoked on implementing subclasses, when the tabs are overshooting at
     * the end.
     *
     * @param dragPosition
     *         The position of the pointer on the dragging axis in pixels as a {@link Float} value
     * @param overshootThreshold
     *         The position on the dragging axis, an overshoot at the end currently starts at, in
     *         pixels as a {@link Float} value
     * @return The updated position on the dragging axis, an overshoot at the end starts at, in
     * pixels as a {@link Float} value
     */
    protected float onOvershootEnd(final float dragPosition, final float overshootThreshold) {
        return overshootThreshold;
    }

    /**
     * The method, which is invoked on implementing subclasses, when an overshoot has been reverted.
     */
    protected void onOvershootReverted() {

    }

    /**
     * The method, which invoked on implementing subclasses, when the drag handler has been reset.
     */
    protected void onReset() {

    }

    /**
     * Returns, whether the threshold of a swiped tab item, which causes the corresponding tab to be
     * removed, has been reached, or not.
     *
     * @param swipedTabItem
     *         The swiped tab item as an instance of the class {@link TabItem}. The tab item may not
     *         be null
     * @return True, if the threshold has been reached, false otherwise
     */
    protected boolean isSwipeThresholdReached(@NonNull final TabItem swipedTabItem) {
        return false;
    }

    /**
     * Sets the callback, which should be notified about the drag handler's events.
     *
     * @param callback
     *         The callback, which should be set, as an instance of the generic type CallbackType or
     *         null, if no callback should be notified
     */
    public final void setCallback(@Nullable final CallbackType callback) {
        this.callback = callback;
    }

    /**
     * Handles a touch event.
     *
     * @param event
     *         The event, which should be handled, as an instance of the class {@link MotionEvent}.
     *         The event may be not null
     * @return True, if the event has been handled, false otherwise
     */
    public final boolean handleTouchEvent(@NonNull final MotionEvent event) {
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
                        handleDrag(arithmetics.getPosition(Axis.DRAGGING_AXIS, event),
                                arithmetics.getPosition(Axis.ORTHOGONAL_AXIS, event));
                    } else {
                        handleRelease(null, dragThreshold);
                        handleDown(event);
                    }

                    return true;
                case MotionEvent.ACTION_UP:
                    if (!tabSwitcher.isAnimationRunning() && event.getPointerId(0) == pointerId) {
                        handleRelease(event, dragThreshold);
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
     * @param dragPosition
     *         The position of the pointer on the dragging axis in pixels as a {@link Float} value
     * @param orthogonalPosition
     *         The position of the pointer of the orthogonal axis in pixels as a {@link Float}
     *         value
     * @return True, if any tabs have been moved, false otherwise
     */
    public final boolean handleDrag(final float dragPosition, final float orthogonalPosition) {
        if (dragPosition <= startOvershootThreshold) {
            handleOvershoot();
            dragState = DragState.OVERSHOOT_START;
            startOvershootThreshold = onOvershootStart(dragPosition, startOvershootThreshold);
        } else if (dragPosition >= endOvershootThreshold) {
            handleOvershoot();
            dragState = DragState.OVERSHOOT_END;
            endOvershootThreshold = onOvershootEnd(dragPosition, endOvershootThreshold);
        } else {
            onOvershootReverted();
            float previousDistance = dragHelper.isReset() ? 0 : dragHelper.getDragDistance();
            dragHelper.update(dragPosition);

            if (swipeEnabled) {
                swipeDragHelper.update(orthogonalPosition);

                if (dragState == DragState.NONE && swipeDragHelper.hasThresholdBeenReached()) {
                    TabItem tabItem = getFocusedTab(dragHelper.getDragStartPosition());

                    if (tabItem != null) {
                        dragState = DragState.SWIPE;
                        swipedTabItem = tabItem;
                    }
                }
            }

            if (dragState != DragState.SWIPE && dragHelper.hasThresholdBeenReached()) {
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

            if (dragState == DragState.SWIPE) {
                notifyOnSwipe(swipedTabItem, swipeDragHelper.getDragDistance());
            } else if (dragState != DragState.NONE) {
                float currentDragDistance = dragHelper.getDragDistance();
                float distance = currentDragDistance - dragDistance;
                dragDistance = currentDragDistance;
                DragState overshoot = notifyOnDrag(dragState, distance);

                if (overshoot == DragState.OVERSHOOT_END && (dragState == DragState.DRAG_TO_END ||
                        dragState == DragState.OVERSHOOT_END)) {
                    endOvershootThreshold = dragPosition;
                    dragState = DragState.OVERSHOOT_END;
                } else if (overshoot == DragState.OVERSHOOT_START &&
                        (dragState == DragState.DRAG_TO_START ||
                                dragState == DragState.OVERSHOOT_START)) {
                    startOvershootThreshold = dragPosition;
                    dragState = DragState.OVERSHOOT_START;
                }

                return true;
            }
        }

        return false;
    }

    /**
     * Handles, when a drag gesture has been ended.
     *
     * @param event
     *         The motion event, which ended the drag gesture, as an instance of the class {@link
     *         MotionEvent} or null, if no fling animation should be triggered
     * @param dragThreshold
     *         The drag threshold, which should be used to recognize drag gestures, in pixels as an
     *         {@link Integer} value
     */
    public final void handleRelease(@Nullable final MotionEvent event, final int dragThreshold) {
        if (dragState == DragState.SWIPE) {
            float swipeVelocity = 0;

            if (event != null && velocityTracker != null) {
                int pointerId = event.getPointerId(0);
                velocityTracker.computeCurrentVelocity(1000, maxFlingVelocity);
                swipeVelocity = Math.abs(velocityTracker.getXVelocity(pointerId));
            }

            boolean remove = swipedTabItem.getTab().isCloseable() &&
                    (swipeVelocity >= minSwipeVelocity || isSwipeThresholdReached(swipedTabItem));
            notifyOnSwipeEnded(swipedTabItem, remove,
                    swipeVelocity >= minSwipeVelocity ? swipeVelocity : 0);
        } else if (dragState == DragState.DRAG_TO_START || dragState == DragState.DRAG_TO_END) {
            if (event != null && velocityTracker != null && dragHelper.hasThresholdBeenReached()) {
                handleFling(event, dragState);
            }
        } else if (dragState == DragState.OVERSHOOT_END) {
            notifyOnRevertEndOvershoot();
        } else if (dragState == DragState.OVERSHOOT_START) {
            notifyOnRevertStartOvershoot();
        } else if (event != null) {
            handleClick(event);
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
        onReset();
    }

}
