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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import static de.mrapp.android.util.Condition.ensureNotNull;

/**
 * A dispatcher, which allows to dispatch touch events to multiple event handlers in the order of
 * their priority. Only the first event handler, which is suited to handle an event, is invoked.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public class TouchEventDispatcher implements Iterable<AbstractTouchEventHandler> {

    /**
     * Defines the interface, a class, which should be notified, when event handlers are added to or
     * removed from a {@link TouchEventDispatcher}, must implement.
     */
    public interface Callback {

        /**
         * The method, which is invoked, when an event handler has been added.
         *
         * @param dispatcher
         *         The dispatcher, the event handler has been added to, as an instance of the class
         *         {@link TouchEventDispatcher}. The dispatcher may not be null
         * @param eventHandler
         *         The event handler, which has been added, as an instance of the class {@link
         *         AbstractTouchEventHandler}. The event handler may not be null
         */
        void onAddedEventHandler(@NonNull TouchEventDispatcher dispatcher,
                                 @NonNull AbstractTouchEventHandler eventHandler);

        /**
         * The method, which is invoked, when an event handler has been removed.
         *
         * @param dispatcher
         *         The dispatcher, the event handler has been removed from, as an instance of the
         *         class {@link TouchEventDispatcher}. The dispatcher may not be null
         * @param eventHandler
         *         The event handler, which has been removed, as an instance of the class {@link
         *         AbstractTouchEventHandler}. The event handler may not be null
         */
        void onRemovedEventHandler(@NonNull TouchEventDispatcher dispatcher,
                                   @NonNull AbstractTouchEventHandler eventHandler);

    }

    /**
     * An iterator, which allows to iterate the event handlers of a {@link TouchEventDispatcher}.
     */
    private class EventHandlerIterator implements Iterator<AbstractTouchEventHandler> {

        /**
         * The iterator, which allows to iterate the priorities of the event handlers.
         */
        private Iterator<Integer> priorityIterator;

        /**
         * The iterator, which allows to iterate the event handlers with the current priority.
         */
        private Iterator<AbstractTouchEventHandler> eventHandlerIterator;

        /**
         * Creates a new iterator, which allows to iterate the event handlers of a {@link
         * TouchEventDispatcher}.
         */
        public EventHandlerIterator() {
            priorityIterator = eventHandlers.keySet().iterator();

            if (priorityIterator.hasNext()) {
                int key = priorityIterator.next();
                Set<AbstractTouchEventHandler> handlers = eventHandlers.get(key);
                eventHandlerIterator = handlers.iterator();
            } else {
                eventHandlerIterator = null;
            }
        }

        @Override
        public boolean hasNext() {
            return (eventHandlerIterator != null && eventHandlerIterator.hasNext()) ||
                    priorityIterator.hasNext();
        }

        @Override
        public AbstractTouchEventHandler next() {
            if (eventHandlerIterator.hasNext()) {
                return eventHandlerIterator.next();
            } else if (priorityIterator.hasNext()) {
                int key = priorityIterator.next();
                Set<AbstractTouchEventHandler> handlers = eventHandlers.get(key);
                eventHandlerIterator = handlers.iterator();
                return next();
            }

            return null;
        }

    }

    /**
     * A sorted map, which contains the event handlers, touch events can be dispatched to. The
     * handlers are sorted by decreasing priority.
     */
    private final SortedMap<Integer, Set<AbstractTouchEventHandler>> eventHandlers;

    /**
     * The event handler, which is currently active.
     */
    private AbstractTouchEventHandler activeEventHandler;

    /**
     * The callback, which is notified, when event handlers are added or removed.
     */
    private Callback callback;

    /**
     * Returns, whether a specific touch event occurred inside the touchable area of an event
     * handler.
     *
     * @param event
     *         The touch event, which should be checked, as an instance of the class {@link
     *         MotionEvent}. The touch event may not be null
     * @param eventHandler
     *         The event handler as an instance of the class {@link AbstractTouchEventHandler}. The
     *         event handler may not be null
     * @return True, if the given touch event occurred inside the touchable area, false otherwise
     */
    private boolean isInsideTouchableArea(@NonNull final MotionEvent event,
                                          @NonNull final AbstractTouchEventHandler eventHandler) {
        RectF touchableArea = eventHandler.getTouchableArea();
        return touchableArea == null ||
                (event.getX() >= touchableArea.left && event.getX() <= touchableArea.right &&
                        event.getY() >= touchableArea.top && event.getY() <= touchableArea.bottom);
    }

    /**
     * Notifies the callback, that an event handler has been added to the dispatcher.
     *
     * @param eventHandler
     *         The event handler, which has been added, as an instance of the class {@link
     *         AbstractTouchEventHandler}. The event handler may not be null
     */
    private void notifyOnAddedEventHandler(@NonNull final AbstractTouchEventHandler eventHandler) {
        if (callback != null) {
            callback.onAddedEventHandler(this, eventHandler);
        }
    }

    /**
     * Notifies the callback, that an event handler has been removed from the dispatcher.
     *
     * @param eventHandler
     *         The event handler, which has been removed, as an instance of the class {@link
     *         AbstractTouchEventHandler}. The event handler may not be null
     */
    private void notifyOnRemovedEventHandler(
            @NonNull final AbstractTouchEventHandler eventHandler) {
        if (callback != null) {
            callback.onRemovedEventHandler(this, eventHandler);
        }
    }

    /**
     * Creates a new dispatcher, which allows to dispatch touch events to multiple event handlers in
     * the order of their priority.
     */
    public TouchEventDispatcher() {
        this.eventHandlers = new TreeMap<>(Collections.reverseOrder());
        this.activeEventHandler = null;
        this.callback = null;
    }

    /**
     * Sets the callback, which should be notified, when event handlers are added or removed.
     *
     * @param callback
     *         The callback, which should be set, as an instance of the type {@link Callback} or
     *         null, if no callback should be notified
     */
    public final void setCallback(@Nullable final Callback callback) {
        this.callback = callback;
    }

    /**
     * Adds a specific event handler to the dispatcher.
     *
     * @param handler
     *         The event handler, which should be added, as an instance of hte class {@link
     *         AbstractTouchEventHandler}. The event handler may not be null
     */
    public final void addEventHandler(@NonNull final AbstractTouchEventHandler handler) {
        ensureNotNull(handler, "The handler may not be null");
        int key = handler.getPriority();
        Set<AbstractTouchEventHandler> handlers = eventHandlers.get(key);

        if (handlers == null) {
            handlers = new LinkedHashSet<>();
            eventHandlers.put(key, handlers);
        }

        handlers.add(handler);
        notifyOnAddedEventHandler(handler);
    }

    /**
     * Removes a specific event handler from the dispatcher.
     *
     * @param handler
     *         The event handler, which should be removed, as an instance of the class {@link
     *         AbstractTouchEventHandler}. The event handler may not be null
     */
    public final void removeEventHandler(@NonNull final AbstractTouchEventHandler handler) {
        ensureNotNull(handler, "The handler may not be null");
        Collection<AbstractTouchEventHandler> handlers = eventHandlers.get(handler.getPriority());

        if (handlers != null) {
            Iterator<AbstractTouchEventHandler> iterator = handlers.iterator();
            AbstractTouchEventHandler eventHandler;

            while ((eventHandler = iterator.next()) != null) {
                if (handler.equals(eventHandler)) {
                    iterator.remove();
                    notifyOnRemovedEventHandler(eventHandler);
                }
            }
        }

        if (handler.equals(activeEventHandler)) {
            activeEventHandler.onUp(null);
            activeEventHandler = null;
        }
    }

    /**
     * Handles a specific touch event by dispatching it to the first suited handler.
     *
     * @param event
     *         The event, which should be dispatched, as an instance of the class {@link
     *         MotionEvent}. The event may not be null
     * @return True, if the event has been handled, false otherwise
     */
    public final boolean dispatchTouchEvent(@NonNull final MotionEvent event) {
        ensureNotNull(event, "The event may not be null");
        boolean handled = false;

        if (activeEventHandler != null) {
            if (isInsideTouchableArea(event, activeEventHandler)) {
                handled = activeEventHandler.handleTouchEvent(event);
            } else {
                activeEventHandler.onUp(event);
                activeEventHandler = null;
            }
        }

        if (!handled) {
            Iterator<AbstractTouchEventHandler> iterator = iterator();
            AbstractTouchEventHandler handler;

            while ((handler = iterator.next()) != null && !handled) {
                if (isInsideTouchableArea(event, handler)) {
                    handled = handler.handleTouchEvent(event);
                }
            }
        }

        return handled;
    }

    @Override
    public final Iterator<AbstractTouchEventHandler> iterator() {
        return new EventHandlerIterator();
    }

}