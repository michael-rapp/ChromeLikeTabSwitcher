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

import android.support.annotation.NonNull;
import android.view.MotionEvent;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
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
public class TouchEventDispatcher {

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
     * Creates a new dispatcher, which allows to dispatch touch events to multiple event handlers in
     * the order of their priority.
     */
    public TouchEventDispatcher() {
        this.eventHandlers = new TreeMap<>(Collections.reverseOrder());
        this.activeEventHandler = null;
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
            handlers.remove(handler);
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
        Iterator<Map.Entry<Integer, Set<AbstractTouchEventHandler>>> entryIterator =
                eventHandlers.entrySet().iterator();
        Map.Entry<Integer, Set<AbstractTouchEventHandler>> entry;
        boolean handled = false;

        while ((entry = entryIterator.next()) != null && !handled) {
            Iterator<AbstractTouchEventHandler> handlerIterator = entry.getValue().iterator();
            AbstractTouchEventHandler handler;

            while ((handler = handlerIterator.next()) != null && !handled) {
                handled = handler.handleTouchEvent(event);
            }
        }

        return handled;
    }

}