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

import android.view.animation.Interpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.mrapp.util.Condition;

/**
 * A swipe animation, which moves tabs on the orthogonal axis. When using the smartphone layout,
 * their size and opacity is animated at the same time. Swipe animations can be used to add or
 * remove tabs to a {@link TabSwitcher}.
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
         * When the tab should be moved to/from the left, respectively the top when in landscape
         * mode.
         */
        LEFT_OR_TOP,

        /**
         * When the tab should be moved to/from the right, respectively the bottom when in landscape
         * mode.
         */
        RIGHT_OR_BOTTOM

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
         * The duration of the animations, which are used to relocate the other tabs, when a tabs
         * has been added or removed.
         */
        private long relocateAnimationDuration;

        /**
         * Creates a new builder, which allows to configure and create instances of the class {@link
         * SwipeAnimation}.
         */
        public Builder() {
            setDirection(SwipeDirection.RIGHT_OR_BOTTOM);
            setRelocateAnimationDuration(-1);
        }

        /**
         * Sets the direction of the animations, which are created by the builder.
         *
         * @param direction
         *         The direction, which should be set, as a value of the enum {@link
         *         SwipeDirection}. The direction may either be {@link SwipeDirection#LEFT_OR_TOP}
         *         or {@link SwipeDirection#RIGHT_OR_BOTTOM}
         * @return The builder, this method has be called upon, as an instance of the generic type
         * BuilderType. The builder may not be null
         */
        @NonNull
        public final Builder setDirection(@NonNull final SwipeDirection direction) {
            Condition.INSTANCE.ensureNotNull(direction, "The direction may not be null");
            this.direction = direction;
            return self();
        }

        /**
         * Sets the duration of the animations, which are used to relocate the other tabs, when a
         * tab has been added or removed.
         *
         * @param relocateAnimationDuration
         *         The duration, which should be set, in milliseconds as a {@link Long} value or -1,
         *         if the default duration should be used
         * @return The builder, this method has be called upon, as an instance of the generic type
         * BuilderType. The builder may not be null
         */
        @NonNull
        public final Builder setRelocateAnimationDuration(final long relocateAnimationDuration) {
            Condition.INSTANCE.ensureAtLeast(relocateAnimationDuration, -1,
                    "The relocate animation duration must be at least -1");
            this.relocateAnimationDuration = relocateAnimationDuration;
            return self();
        }

        @NonNull
        @Override
        public final SwipeAnimation create() {
            return new SwipeAnimation(duration, interpolator, direction, relocateAnimationDuration);
        }

    }

    /**
     * The direction of the swipe animation.
     */
    private final SwipeDirection direction;

    /**
     * The duration of the animations, which are used to relocate the other tabs, when a tabs has
     * been added or removed.
     */
    private final long relocateAnimationDuration;

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
     * @param relocateAnimationDuration
     *         The duration of the animations, which are used to relocate other tabs, when a tab has
     *         been added or removed, in milliseconds as a {@link Long} value or -1, if the default
     *         duration should be used
     */
    private SwipeAnimation(final long duration, @Nullable final Interpolator interpolator,
                           @NonNull final SwipeDirection direction,
                           final long relocateAnimationDuration) {
        super(duration, interpolator);
        Condition.INSTANCE.ensureNotNull(direction, "The direction may not be null");
        Condition.INSTANCE.ensureAtLeast(relocateAnimationDuration, -1,
                "The relocate animation duration must be at least -1");
        this.direction = direction;
        this.relocateAnimationDuration = relocateAnimationDuration;
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

    /**
     * Returns the duration of the animations, which are used to relocate the other tabs, when a tab
     * has been added or removed.
     *
     * @return The duration of the animations, which are used to relocate the other tabs, when a tab
     * has been added or removed, in milliseconds as a {@link Long} value or -1, if the default
     * duration is used
     */
    public final long getRelocateAnimationDuration() {
        return relocateAnimationDuration;
    }

}