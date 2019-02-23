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

import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.VelocityTracker;

import java.util.Comparator;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.mrapp.android.tabswitcher.TabSwitcher;
import de.mrapp.android.util.gesture.DragHelper;
import de.mrapp.util.Condition;

/**
 * An abstract base class for all event handlers, which can be managed by a {@link
 * TouchEventDispatcher} in order to dispatch touch events to them.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public abstract class AbstractTouchEventHandler implements Comparator<AbstractTouchEventHandler> {

    /**
     * The maximum priority of an event handler.
     */
    protected static final int MAX_PRIORITY = Integer.MAX_VALUE;

    /**
     * The minimum priority of an event handler.
     */
    protected static final int MIN_PRIORITY = Integer.MIN_VALUE;

    /**
     * The priority of the event handler.
     */
    private final int priority;

    /**
     * The tab switcher, the event handler belongs to.
     */
    private final TabSwitcher tabSwitcher;

    /**
     * The drag helper, which is used by the touch handler to recognize drag gestures.
     */
    private final DragHelper dragHelper;

    /**
     * The current threshold of the drag helper, which is used to recognize drag gestures.
     */
    private int dragThreshold;

    /**
     * The velocity tracker, which is used by the touch handler to measure the velocity of drag
     * gestures.
     */
    private VelocityTracker velocityTracker;

    /**
     * The id of the pointer, which has been used to start the current drag gesture.
     */
    private int pointerId;

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
        onDown(event);
    }

    /**
     * Returns the tab switcher, the event handler belongs to.
     *
     * @return The tab switcher, the event handler belongs to, as an instance of the class {@link
     * TabSwitcher}. The tab switcher may not be null
     */
    @NonNull
    protected final TabSwitcher getTabSwitcher() {
        return tabSwitcher;
    }

    /**
     * The velocity tracker, which is used by the touch handler to measure the velocity of drag
     * gestures.
     *
     * @return The velocity tracker, which is used by the touch handler to measure the velocity of
     * drag gestures as an instance of the class {@link VelocityTracker} or null, if the velocity
     * tracker has not be initialized yet
     */
    @Nullable
    protected final VelocityTracker getVelocityTracker() {
        return velocityTracker;
    }

    /**
     * Resets the event handler once a drag gesture has been ended.
     */
    protected void reset() {
        if (this.velocityTracker != null) {
            this.velocityTracker.recycle();
            this.velocityTracker = null;
        }

        this.pointerId = -1;
        this.dragHelper.reset(dragThreshold);
    }

    /**
     * Returns, whether performing a drag gesture is currently allowed, or not.
     *
     * @return True, if performing a drag gesture is currently allowed, false otherwise
     */
    protected abstract boolean isDraggingAllowed();

    /**
     * The method, which is invoked on implementing subclasses, when a touch event is about to be
     * handled.
     */
    protected abstract void onTouchEvent();

    /**
     * The method, which is invoked on implementing subclasses in order to handle, when a drag
     * gesture has been started.
     *
     * @param event
     *         The touch event, which started the drag gesture, as an instance of the class {@link
     *         MotionEvent}. The touch event may not be null
     */
    protected abstract void onDown(@NonNull final MotionEvent event);

    /**
     * The method, which is invoked on implementing subclasses in order to handle, when a drag
     * gesture is performed.
     *
     * @param event
     *         The last touch event of the drag gesture as an instance of the class {@link
     *         MotionEvent}. The touch event may not be null
     */
    protected abstract void onDrag(@NonNull final MotionEvent event);

    /**
     * Handles, when a drag gesture has been ended.
     *
     * @param event
     *         The touch event, which ended the drag gesture, as an instance of the class {@link
     *         MotionEvent} or null, if no fling animation should be triggered
     */
    protected abstract void onUp(@Nullable final MotionEvent event);

    /**
     * Creates a new handler, which can be managed by a {@link TouchEventDispatcher} in order to
     * dispatch touch events to it.
     *
     * @param priority
     *         The priority of the handler as an {@link Integer} value. The priority must be at
     *         least {@link AbstractTouchEventHandler#MIN_PRIORITY} and at maximum {@link
     *         AbstractTouchEventHandler#MAX_PRIORITY}
     * @param tabSwitcher
     *         The tab switcher, the event handler belongs to, as an instance of the class {@link
     *         TabSwitcher}. The tab switcher may not be null
     * @param dragThreshold
     *         The threshold of the drag helper, which is used to recognize drag gestures, in pixels
     *         as an {@link Integer} value The threshold must be at least 0
     */
    public AbstractTouchEventHandler(final int priority, @NonNull final TabSwitcher tabSwitcher,
                                     final int dragThreshold) {
        Condition.INSTANCE.ensureAtLeast(priority, MIN_PRIORITY,
                "The priority must be at least" + MIN_PRIORITY);
        Condition.INSTANCE.ensureAtMaximum(priority, MAX_PRIORITY,
                "The priority must be at maximum " + MAX_PRIORITY);
        Condition.INSTANCE.ensureNotNull(tabSwitcher, "The tab switcher may not be null");
        Condition.INSTANCE.ensureAtLeast(dragThreshold, 0, "The drag threshold must be at least 0");
        this.priority = priority;
        this.tabSwitcher = tabSwitcher;
        this.dragHelper = new DragHelper(0);
        this.dragThreshold = dragThreshold;
        reset();
    }

    /**
     * Returns, whether the event handler is reset, or not.
     *
     * @return True, if the event handler is reset, false otherwise
     */
    public final boolean isReset() {
        return pointerId == -1;
    }

    /**
     * Returns, whether a drag gesture is currently handled by the event handler, or not. This
     * method may be overridden by subclasses, if multiple drag helpers are used.
     *
     * @return True, if a drag gesture is currently handled by the event handler, false otherwise
     */
    @CallSuper
    public boolean isDragging() {
        return dragHelper.hasThresholdBeenReached();
    }

    /**
     * Returns the priority of the event handler. Events are first dispatched to handlers with a
     * higher priority.
     *
     * @return The priority of the event handler as an {@link Integer} value
     */
    public final int getPriority() {
        return priority;
    }

    /**
     * Returns the bounds of the onscreen area, the handler takes into consideration for handling
     * touch events. Touch events that occur outside of the area are clipped. By default, the area
     * is not restricted. Subclasses may override this method in order to restrict the area.
     *
     * @return The bounds of the onscreen area, the handler takes into consideration for handling
     * touch events, as an instance of the class {@link RectF} or null, if the area is not
     * restricted
     */
    @Nullable
    public RectF getTouchableArea() {
        return null;
    }

    /**
     * Returns, whether a specific touch event occurred inside the touchable area of the event
     * handler.
     *
     * @param event
     *         The touch event, which should be checked, as an instance of the class {@link
     *         MotionEvent}. The touch event may not be null
     * @return True, if the given touch event occurred inside the touchable area, false otherwise
     */
    public final boolean isInsideTouchableArea(@NonNull final MotionEvent event) {
        return getTouchableArea() == null || (event.getX() >= getTouchableArea().left &&
                event.getX() <= getTouchableArea().right &&
                event.getY() >= getTouchableArea().top &&
                event.getY() <= getTouchableArea().bottom);
    }

    /**
     * Returns the drag helper, which is used by the event handler to recognize drag gestures.
     *
     * @return The drag helper, which is used by the event handler to recognize drag gestures, as an
     * instance of the class DragHelper. The drag helper may not be null
     */
    @NonNull
    public final DragHelper getDragHelper() {
        return dragHelper;
    }

    /**
     * Sets the id of the pointer, which has been used to start the current drag gesture.
     *
     * @param pointerId
     *         The id, which should be set, as an {@link Integer} value or -1, if no drag gesture is
     *         currently started
     */
    public final void setPointerId(final int pointerId) {
        this.pointerId = pointerId;
    }

    /**
     * Handles a specific touch event. The event is only handled, if it occurred inside the
     * touchable area.
     *
     * @param event
     *         The event, which should be handled, as an instance of the class {@link MotionEvent}.
     *         The event may not be null
     * @return True, if the event has been handled, false otherwise
     */
    public final boolean handleTouchEvent(@NonNull final MotionEvent event) {
        Condition.INSTANCE.ensureNotNull(event, "The event may not be null");

        if (!tabSwitcher.isAnimationRunning() && isDraggingAllowed()) {
            onTouchEvent();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    handleDown(event);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (event.getPointerId(0) == pointerId) {
                        if (velocityTracker == null) {
                            velocityTracker = VelocityTracker.obtain();
                        }

                        velocityTracker.addMovement(event);
                        onDrag(event);
                    } else {
                        onUp(null);
                        handleDown(event);
                    }

                    return true;
                case MotionEvent.ACTION_UP:
                    if (event.getPointerId(0) == pointerId) {
                        onUp(event);
                    }

                    return true;
                default:
                    break;
            }
        }

        return false;
    }

    @Override
    public final int compare(final AbstractTouchEventHandler o1,
                             final AbstractTouchEventHandler o2) {
        int priority1 = o1.getPriority();
        int priority2 = o2.getPriority();
        return priority1 > priority2 ? 1 : (priority1 < priority2 ? -1 : 0);
    }

    @Override
    public final int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + getClass().hashCode();
        return result;
    }

    @Override
    public final boolean equals(final Object obj) {
        return obj != null && obj.getClass() == getClass();
    }

}