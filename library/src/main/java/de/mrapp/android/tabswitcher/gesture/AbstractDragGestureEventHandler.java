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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.mrapp.android.tabswitcher.TabSwitcher;

/**
 * An abstract base class for all event handlers, which allow to handle drag gestures.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public abstract class AbstractDragGestureEventHandler extends AbstractTouchEventHandler {

    /**
     * The bounds of the onscreen area, the handler takes into consideration for handling
     * touch events.
     */
    private final RectF touchableArea;

    /**
     * Creates a new handler, which can be managed by a {@link TouchEventDispatcher} in order to
     * dispatch touch events to it.
     *
     * @param tabSwitcher
     *         The tab switcher, the event handler belongs to, as an instance of the class {@link
     *         TabSwitcher}. The tab switcher may not be null
     * @param dragThreshold
     *         The threshold of the drag helper, which is used to recognize drag gestures, in pixels
     *         as an {@link Integer} value The threshold must be at least 0
     * @param touchableArea
     *         The bounds of the onscreen area, the handler should take into consideration for
     *         handling touch events, as an instance of the class {@link RectF} or null, if the are
     *         should not be restricted
     */
    public AbstractDragGestureEventHandler(@NonNull final TabSwitcher tabSwitcher,
                                           final int dragThreshold,
                                           @Nullable final RectF touchableArea) {
        super(MAX_PRIORITY, tabSwitcher, dragThreshold);
        this.touchableArea = touchableArea;
    }

    @Nullable
    @Override
    public final RectF getTouchableArea() {
        return touchableArea;
    }

}