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

import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.MotionEvent;

import java.util.Comparator;

import static de.mrapp.android.util.Condition.ensureAtLeast;
import static de.mrapp.android.util.Condition.ensureAtMaximum;
import static de.mrapp.android.util.Condition.ensureNotNull;

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
    public static final int MAX_PRIORITY = Integer.MAX_VALUE;

    /**
     * The minimum priority of an event handler.
     */
    public static final int MIN_PRIORITY = Integer.MIN_VALUE;

    /**
     * The priority of the event handler.
     */
    private final int priority;

    /**
     * The method, which is invoked on implementing subclasses in order to handle a specific touch
     * event.
     *
     * @param event
     *         The event, which should be handled, as an instance of the class {@link MotionEvent}.
     *         The event may not be null
     * @return True, if the event has been handled, false otherwise
     */
    protected abstract boolean onHandleTouchEvent(@NonNull final MotionEvent event);

    /**
     * Creates a new handler, which can be managed by a {@link TouchEventDispatcher} in order to
     * dispatch touch events to it.
     *
     * @param priority
     *         The priority of the handler as an {@link Integer} value. The priority must be at
     *         least {@link AbstractTouchEventHandler#MIN_PRIORITY} and at maximum {@link
     *         AbstractTouchEventHandler#MAX_PRIORITY}
     */
    public AbstractTouchEventHandler(final int priority) {
        ensureAtLeast(priority, MIN_PRIORITY, "The priority must be at least" + MIN_PRIORITY);
        ensureAtMaximum(priority, MAX_PRIORITY, "The priority must be at maximum " + MAX_PRIORITY);
        this.priority = priority;
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
     * touch events. Touch events that occur outside of the area are clipped.
     *
     * @return The bounds of the onscreen area, the handler takes into consideration for handling
     * touch events, as an instance of the class {@link RectF} or null, if the area should not be
     * restricted
     */
    @Nullable
    public RectF getTouchableArea() {
        return null;
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
        ensureNotNull(event, "The event may not be null");
        RectF touchableArea = getTouchableArea();
        return (touchableArea == null ||
                (event.getX() >= touchableArea.left && event.getX() <= touchableArea.right &&
                        event.getY() >= touchableArea.top &&
                        event.getY() <= touchableArea.bottom)) && onHandleTouchEvent(event);
    }

    @Override
    public int compare(final AbstractTouchEventHandler o1, final AbstractTouchEventHandler o2) {
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