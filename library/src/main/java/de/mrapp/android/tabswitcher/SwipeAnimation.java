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
import android.support.annotation.Nullable;
import android.view.animation.Interpolator;

import static de.mrapp.android.util.Condition.ensureNotNull;

/**
 * A swipe animation, which moves tabs on the orthogonal axis, while animating their size and
 * opacity at the same time. Swipe animations can be used to add or remove tabs to a {@link
 * TabSwitcher} when using the smartphone layout.
 *
 * @author Michael Rapp
 * @since 0.1.0
 */
public class SwipeAnimation extends Animation {

    /**
     * Contains all possible directions of a swipe animation.
     */
    public enum SwipeDirection {

        /**
         * When the tab should be swiped in/out from/to the left, respectively the top, when
         * dragging horizontally.
         */
        LEFT,

        /**
         * When the tab should be swiped in/out from/to the right, respectively the bottom, when
         * dragging horizontally.
         */
        RIGHT

    }

    /**
     * A builder, which allows to configure and create instances of the class {@link
     * SwipeAnimation}.
     */
    public static class Builder extends Animation.Builder<SwipeAnimation, Builder> {

        /**
         * The direction of the animations, which are created by the builder.
         */
        private SwipeDirection direction;

        /**
         * Creates a new builder, which allows to configure and create instances of the class {@link
         * SwipeAnimation}.
         */
        public Builder() {
            setDirection(SwipeDirection.RIGHT);
        }

        /**
         * Sets the direction of the animations, which are created by the builder.
         *
         * @param direction
         *         The direction, which should be set, as a value of the enum {@link
         *         SwipeDirection}. The direction may either be {@link SwipeDirection#LEFT} or
         *         {@link SwipeDirection#RIGHT}
         * @return The builder, this method has be called upon, as an instance of the generic type
         * BuilderType. The builder may not be null
         */
        @NonNull
        public final Builder setDirection(@NonNull final SwipeDirection direction) {
            ensureNotNull(direction, "The direction may not be null");
            this.direction = direction;
            return self();
        }

        @NonNull
        @Override
        public final SwipeAnimation create() {
            return new SwipeAnimation(duration, interpolator, direction);
        }

    }

    /**
     * The direction of the swipe animation.
     */
    private final SwipeDirection direction;

    /**
     * Creates a new swipe animation.
     *
     * @param duration
     *         The duration of the animation in milliseconds as a {@link Long} value or -1, if the
     *         default duration should be used
     * @param interpolator
     *         The interpolator, which should be used by the animation, as an instance of the type
     *         {@link Interpolator} or null, if the default interpolator should be used
     * @param direction
     *         The direction of the swipe animation as a value of the enum {@link SwipeDirection}.
     *         The direction may not be null
     */
    private SwipeAnimation(final long duration, @Nullable final Interpolator interpolator,
                           @NonNull final SwipeDirection direction) {
        super(duration, interpolator);
        ensureNotNull(direction, "The direction may not be null");
        this.direction = direction;
    }

    /**
     * Returns the direction of the swipe animation.
     *
     * @return The direction of the swipe animation as a value of the enum {@link SwipeDirection}.
     * The direction may not be null
     */
    @NonNull
    public final SwipeDirection getDirection() {
        return direction;
    }

}