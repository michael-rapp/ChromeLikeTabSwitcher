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

import static de.mrapp.android.util.Condition.ensureGreater;
import static de.mrapp.android.util.Condition.ensureNotNull;

/**
 * An animation, which can be used to add or remove tabs to/from a {@link TabSwitcher}.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public abstract class Animation {

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
     * A swipe animation, which moves tabs on the orthogonal axis, while  animating their size and
     * alpha at the same time
     */
    public static class SwipeAnimation extends Animation {

        /**
         * The direction of the swipe animation.
         */
        private final SwipeDirection direction;

        /**
         * Creates a new swipe animation.
         *
         * @param direction
         *         The direction of the swipe animation as a value of the enum {@link
         *         SwipeDirection}. The direction may not be null
         */
        public SwipeAnimation(@NonNull final SwipeDirection direction) {
            ensureNotNull(direction, "The direction may not be null");
            this.direction = direction;
        }

        /**
         * Returns the direction of the swipe animation.
         *
         * @return The direction of the swipe animation as a value of the enum {@link
         * SwipeDirection}. The direction may not be null
         */
        @NonNull
        public final SwipeDirection getDirection() {
            return direction;
        }

    }

    /**
     * A reveal animation, which animates the size of a tab starting at a specific position.
     */
    public static class RevealAnimation extends Animation {

        /**
         * The horizontal position, the animation starts at.
         */
        private final float x;

        /**
         * The vertical position, the animation starts at.
         */
        private final float y;

        /**
         * Creates a new reveal animation.
         *
         * @param x
         *         The horizontal position, the animation should start at, in pixels as a {@link
         *         Float} value
         * @param y
         *         The vertical position, the animation should start at, in pixels as a {@link
         *         Float} value
         */
        public RevealAnimation(final float x, final float y) {
            this.x = x;
            this.y = y;
        }

        /**
         * Returns the horizontal position, the animation starts at.
         *
         * @return The horizontal position, the animation starts at, in pixels as a {@link Float}
         * value
         */
        public final float getX() {
            return x;
        }

        /**
         * Returns the vertical position, the animation starts at.
         *
         * @return The vertical position, the animation starts at, in pixels as a {@link Float}
         * value
         */
        public final float getY() {
            return y;
        }

    }

    /**
     * The duration of the animation in milliseconds.
     */
    private long duration;

    /**
     * Creates and returns a swipe animation, which moves tabs on the orthogonal axis, while
     * animating their size and alpha at the same time. By default, the swipe animation uses the
     * direction <code>SwipeDirection.RIGHT</code>.
     *
     * @return The animation, which has been created, as an instance of the class {@link
     * SwipeAnimation}. The animation may not be null
     */
    @NonNull
    public static SwipeAnimation createSwipeAnimation() {
        return createSwipeAnimation(SwipeDirection.RIGHT);
    }

    /**
     * Creates and returns a swipe animation, which moves tabs on the orthogonal axis by using a
     * specific direction, while animating their size and alpha at the same time.
     *
     * @param direction
     *         The direction, which should be used by the animation, as a value of the enum {@link
     *         SwipeDirection}. The direction may not be null
     * @return The animation, which has been created, as an instance of the class {@link
     * SwipeAnimation}. The animation may not be null
     */
    @NonNull
    public static SwipeAnimation createSwipeAnimation(@NonNull final SwipeDirection direction) {
        return new SwipeAnimation(direction);
    }

    /**
     * Creates and returns a reveal animation, which animates the size of a tab starting at a
     * specific position.
     *
     * @param x
     *         The horizontal position, the animation should start at, in pixels as a {@link Float}
     *         value
     * @param y
     *         The vertical position, the animation should start at, in pixels as a {@link Float}
     *         value
     * @return The animation, which has been created, as an instance of the class {@link
     * RevealAnimation}. The animation may not be null
     */
    @NonNull
    public static RevealAnimation createRevealAnimation(final float x, final float y) {
        return new RevealAnimation(x, y);
    }

    /**
     * Returns the duration of the animation.
     *
     * @return The duration of the animation in milliseconds as a {@link Long} value
     */
    public final long getDuration() {
        return duration;
    }

    /**
     * Sets the duration of the animation.
     *
     * @param duration
     *         The duration, which should be set, in milliseconds as a {@link Long} value. The
     *         duration must be greater than 0
     */
    public final void setDuration(final long duration) {
        ensureGreater(duration, 0, "The duration must be greater than 0");
        this.duration = duration;
    }

}