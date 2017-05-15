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

import android.graphics.RectF;
import android.support.annotation.Nullable;

/**
 * A drag gesture, which allows to switch between tabs, when swiping horizontally.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public class SwipeGesture extends DragGesture {

    /**
     * A builder, which allows to configure and create instances of the class {@link SwipeGesture}.
     */
    public static class Builder extends DragGesture.Builder<SwipeGesture, Builder> {

        @Override
        public final SwipeGesture create() {
            return new SwipeGesture(threshold, touchableArea);
        }

    }

    /**
     * Creates a new drag gesture, which allows to switch between tabs, when swiping horizontally.
     *
     * @param threshold
     *         The distance in pixels, the gesture must last until it is recognized, as an {@link
     *         Integer} value. The distance must be at least 0 or -1, if the default distance should
     *         be used
     * @param touchableArea
     *         The bounds of the onscreen area, which should be taken into consideration for
     *         recognizing the drag gesture, as an instance of the class {@link RectF} or null, if
     *         the area should not be restricted
     */
    private SwipeGesture(final int threshold, @Nullable final RectF touchableArea) {
        super(threshold, touchableArea);
    }

}