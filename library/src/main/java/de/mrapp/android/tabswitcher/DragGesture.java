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
package de.mrapp.android.tabswitcher;

import android.graphics.RectF;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.mrapp.util.Condition;

/**
 * A drag gesture, which can be used to perform certain actions when dragging in a particular
 * direction.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public abstract class DragGesture {

    /**
     * A builder, which allows to configure and create instances of the class {@link DragGesture}.
     *
     * @param <GestureType>
     *         The type of the drag gestures, which are created by the builder
     * @param <BuilderType>
     *         The type of the builder
     */
    protected static abstract class Builder<GestureType, BuilderType> {

        /**
         * The threshold of the gestures, which are created by the builder.
         */
        protected int threshold;

        /**
         * The bounds of the onscreen area, which is taken into consideration for recognizing the
         * drag gestures, which are created by the builder.
         */
        protected RectF touchableArea;

        /**
         * Returns a reference to the builder.
         *
         * @return A reference to the builder, casted to the generic type Builder. The reference may
         * not be null
         */
        @NonNull
        @SuppressWarnings("unchecked")
        protected final BuilderType self() {
            return (BuilderType) this;
        }

        /**
         * Creates a new builder, which allows to configure and create instances of the class {@link
         * DragGesture}.
         */
        public Builder() {
            setThreshold(-1);
            setTouchableArea(null);
        }

        /**
         * Creates and returns the drag gesture.
         *
         * @return The drag gesture, which has been created, as an instance of the generic type
         * DragGesture. The drag gesture may not be null
         */
        public abstract GestureType create();

        /**
         * Sets the threshold of the drag gestures, which are created by the builder.
         *
         * @param threshold
         *         The threshold, which should be set, in pixels as a {@link Integer} value. The
         *         threshold must be at least 0 or -1, if the default threshold should be used
         * @return The builder, this method has been called upon, as an instance of the generic type
         * BuilderType. The builder may not be null
         */
        @NonNull
        public final BuilderType setThreshold(final int threshold) {
            Condition.INSTANCE.ensureAtLeast(threshold, -1, "The threshold must be at least -1");
            this.threshold = threshold;
            return self();
        }

        /**
         * Sets the bounds of the onscreen area, which should be taken into consideration for
         * recognizing the drag gestures, which are create by the builder. drag gestures.
         *
         * @param left
         *         The coordinate of the left edge in pixels as a {@link Float} value. The
         *         coordinate must be at least 0
         * @param top
         *         The coordinate of the top edge in pixels as a {@link Float} value. The coordinate
         *         must be at least 0
         * @param right
         *         The coordinate of the right edge in pixels as a {@link Float} value. The
         *         coordinate must be greater than the coordinate of the left edge
         * @param bottom
         *         The coordinate of the bottom edge in pixels as a {@link Float} value. The
         *         coordinate must be greater than the coordinate of the top edge
         * @return The builder, this method has been called upon, as an instance of the generic type
         * BuilderType. The builder may not be null
         */
        @NonNull
        public final BuilderType setTouchableArea(final float left, final float top,
                                                  final float right, final float bottom) {
            Condition.INSTANCE.ensureAtLeast(left, 0, "The left coordinate must be at least 0");
            Condition.INSTANCE.ensureAtLeast(top, 0, "The top coordinate must be at least 0");
            Condition.INSTANCE.ensureGreater(right, left,
                    "The right coordinate must be greater than " + left);
            Condition.INSTANCE.ensureGreater(bottom, top,
                    "The bottom coordinate must be greater than " + top);
            return setTouchableArea(new RectF(left, top, right, bottom));
        }

        /**
         * Sets the bounds of the onscreen area, which should be taken into consideration for
         * recognizing the drag gestures, which are create by the builder. drag gestures.
         *
         * @param bounds
         *         The bounds, which should be set, as an instance of the class {@link RectF} or
         *         null, if the area should not be restricted
         * @return The builder, this method has been called upon, as an instance of the generic type
         * BuilderType. The builder may not be null
         */
        @NonNull
        public final BuilderType setTouchableArea(@Nullable final RectF bounds) {
            this.touchableArea = bounds;
            return self();
        }

    }

    /**
     * The distance in pixels, the gesture must last until it is recognized.
     */
    private final int threshold;

    /**
     * The bounds of the onscreen area, which is taken into consideration for recognizing the drag
     * gesture.
     */
    private final RectF touchableArea;

    /**
     * Creates a new drag gesture, which can be used to perform certain action when dragging in a
     * particular direction.
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
    protected DragGesture(final int threshold, @Nullable final RectF touchableArea) {
        Condition.INSTANCE.ensureAtLeast(threshold, -1, "The threshold must be at least -1");
        this.threshold = threshold;
        this.touchableArea = touchableArea;
    }

    /**
     * Returns the distance in pixels, the gesture must last until it is recognized.
     * <p>
     * The distance in pixels, the gesture must last until it is recognized, as an {@link Integer}
     * value. The distance must be at least 0 or -1, if the default distance should be used
     */
    public final int getThreshold() {
        return threshold;
    }

    /**
     * Returns the bounds of the onscreen area, which is taken into consideration for recognizing
     * the drag gesture.
     *
     * @return The bounds of the onscreen area, which is taken into consideration for recognizing
     * the drag gesture, as an instance of the class {@link RectF} or null, if the area is not
     * restricted
     */
    @Nullable
    public final RectF getTouchableArea() {
        return touchableArea;
    }

}