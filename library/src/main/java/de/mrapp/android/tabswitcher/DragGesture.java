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

import android.support.annotation.NonNull;

import static de.mrapp.android.util.Condition.ensureAtLeast;
import static de.mrapp.android.util.Condition.ensureNotNull;

/**
 * A drag gesture, which can be used to perform certain actions when dragging in a particular
 * direction.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public abstract class DragGesture {

    /**
     * Contains all possible directions of drag gestures.
     */
    public enum DragDirection {

        /**
         * When dragging horizontally.
         */
        HORIZONTAL,

        /**
         * When dragging vertically.
         */
        VERTICAL

    }

    protected static abstract class Builder<GestureType, BuilderType> {

        /**
         * The threshold of the gestures, which are created by the builder.
         */
        protected int threshold;

        /**
         * The direction of the gestures, which are created by the builder.
         */
        protected DragDirection direction;

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
         *
         * @param threshold
         *         The default threshold of the gestures, which are created by the builder, as an
         *         {@link Integer} value. The threshold must be at least 0
         * @param direction
         *         The direction of the gestures, which are created by the builder, as a value of
         *         the enum {@link DragDirection}. The direction may either be {@link
         *         DragDirection#HORIZONTAL} or {@link DragDirection#VERTICAL}
         */
        public Builder(final int threshold, @NonNull final DragDirection direction) {
            setThreshold(threshold);
            setDirection(direction);
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
         *         threshold must be at least 0
         * @return The builder, this method has been called upon, as an instance of the generic type
         * BuilderType. The builder may not be null
         */
        @NonNull
        public final BuilderType setThreshold(final int threshold) {
            ensureAtLeast(threshold, 0, "The threshold must be at least 0");
            this.threshold = threshold;
            return self();
        }

        /**
         * Sets the direction of the drag gestures, which are created by the builder.
         *
         * @param direction
         *         The direction, which should be set, as a value of the enum {@link DragDirection}.
         *         The direction may either be {@link DragDirection#HORIZONTAL} or {@link
         *         DragDirection#VERTICAL}
         * @return The builder, this method has been called upon, as an instance of the generic type
         * BuilderType. The builder may not be null
         */
        @NonNull
        public final BuilderType setDirection(@NonNull final DragDirection direction) {
            this.direction = direction;
            return self();
        }

    }

    /**
     * The distance in pixels, the gesture must last until it is recognized.
     */
    private final int threshold;

    /**
     * The direction of the gesture.
     */
    private final DragDirection direction;

    /**
     * Creates a new drag gesture, which can be used to perform certain action when dragging in a
     * particular direction.
     *
     * @param threshold
     *         The distance in pixels, the gesture must last until it is recognized, as an {@link
     *         Integer} value. The distance must be at least 0
     * @param direction
     *         The direction of the gesture as a value of the enum {@link DragDirection}. The
     *         direction may either be {@link DragDirection#HORIZONTAL} or {@link
     *         DragDirection#VERTICAL}
     */
    protected DragGesture(final int threshold, @NonNull final DragDirection direction) {
        ensureAtLeast(threshold, 0, "The threshold must be at least 0");
        ensureNotNull(direction, "The direction may not be null");
        this.threshold = threshold;
        this.direction = direction;
    }

    /**
     * Returns the distance in pixels, the gesture must last until it is recognized.
     *
     * The distance in pixels, the gesture must last until it is recognized, as an {@link
     * Integer} value. The distance must be at least 0
     */
    public final int getThreshold() {
        return threshold;
    }

    /**
     * Returns the direction of the gesture.
     *
     * @return The direction of the gesture as a value of the enum {@link DragGesture}. The
     * direction may either be {@link DragDirection#HORIZONTAL} or {@link DragDirection#VERTICAL}
     */
    @NonNull
    public final DragDirection getDirection() {
        return direction;
    }

}