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

import androidx.annotation.NonNull;
import de.mrapp.android.tabswitcher.DragGesture;
import de.mrapp.android.tabswitcher.PullDownGesture;
import de.mrapp.android.tabswitcher.R;
import de.mrapp.android.tabswitcher.SwipeGesture;
import de.mrapp.android.tabswitcher.TabSwitcher;
import de.mrapp.util.Condition;

/**
 * A factory, which allows to create instances of the class {@link AbstractDragGestureEventHandler}.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public class DragGestureEventHandlerFactory {

    /**
     * The tab switcher, the event handler are created for.
     */
    private final TabSwitcher tabSwitcher;

    /**
     * Creates a new factory, which allows to create instances of the class {@link
     * AbstractDragGestureEventHandler}.
     *
     * @param tabSwitcher
     *         The tab switcher, the event handler should be created for, as an instance of the
     *         class {@link TabSwitcher}. The tab switcher may not be null
     */
    public DragGestureEventHandlerFactory(@NonNull final TabSwitcher tabSwitcher) {
        Condition.INSTANCE.ensureNotNull(tabSwitcher, "The tab switcher may not be null");
        this.tabSwitcher = tabSwitcher;
    }

    /**
     * Creates and returns the event handler, which corresponds to a specific drag gesture.
     *
     * @param dragGesture
     *         The drag gesture, the event handler should be created from, as an instance of the
     *         class {@link DragGesture}. The drag gesture may not be null
     * @return The event handler, which has been created, as an instance of the class {@link
     * AbstractTouchEventHandler}. The event handler may not be null
     */
    @NonNull
    public final AbstractTouchEventHandler fromGesture(@NonNull final DragGesture dragGesture) {
        Condition.INSTANCE.ensureNotNull(dragGesture, "The drag gesture may not be null");

        if (dragGesture instanceof SwipeGesture) {
            int dragThreshold = dragGesture.getThreshold() != -1 ? dragGesture.getThreshold() :
                    tabSwitcher.getResources()
                            .getDimensionPixelSize(R.dimen.swipe_gesture_threshold);
            return new SwipeGestureEventHandler(tabSwitcher, dragThreshold,
                    dragGesture.getTouchableArea(),
                    ((SwipeGesture) dragGesture).getAnimationDuration());
        } else if (dragGesture instanceof PullDownGesture) {
            int dragThreshold = dragGesture.getThreshold() != -1 ? dragGesture.getThreshold() :
                    tabSwitcher.getResources()
                            .getDimensionPixelSize(R.dimen.pull_down_gesture_threshold);
            return new PullDownGestureEventHandler(tabSwitcher, dragThreshold,
                    dragGesture.getTouchableArea());
        }

        throw new IllegalArgumentException(
                "Unsupported drag gesture: " + dragGesture.getClass().getSimpleName());
    }

}