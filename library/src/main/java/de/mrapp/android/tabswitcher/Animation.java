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

import static de.mrapp.android.util.Condition.ensureAtLeast;

/**
 * An animation, which can be used to add or remove tabs to/from a {@link TabSwitcher}.
 *
 * @author Michael Rapp
 * @since 0.1.0
 */
public abstract class Animation {

    /**
     * An abstract base class for all builders, which allow to configure and create instances of the
     * class {@link Animation}.
     *
     * @param <AnimationType>
     *         The type of the animations, which are created by the builder
     * @param <BuilderType>
     *         The type of the builder
     */
    protected static abstract class Builder<AnimationType, BuilderType> {

        /**
         * The duration of the animations, which are created by the builder.
         */
        protected long duration;

        /**
         * The interpolator, which is used by the animations, which are created by the builder.
         */
        protected Interpolator interpolator;

        /**
         * Returns a reference to the builder.
         *
         * @return A reference to the builder, casted to the generic type BuilderType. The reference
         * may not be null
         */
        @NonNull
        @SuppressWarnings("unchecked")
        protected final BuilderType self() {
            return (BuilderType) this;
        }

        /**
         * Creates a new builder, which allows to configure and create instances of the class {@link
         * Animation}.
         */
        public Builder() {
            setDuration(-1);
            setInterpolator(null);
        }

        /**
         * Creates and returns the animation.
         *
         * @return The animation, which has been created, as an instance of the generic type
         * AnimationType. The animation may not be null
         */
        @NonNull
        public abstract AnimationType create();

        /**
         * Sets the duration of the animations, which are created by the builder.
         *
         * @param duration
         *         The duration, which should be set, in milliseconds as a {@link Long} value or -1,
         *         if the default duration should be used
         * @return The builder, this method has be called upon, as an instance of the generic type
         * BuilderType. The builder may not be null
         */
        @NonNull
        public final BuilderType setDuration(final long duration) {
            ensureAtLeast(duration, -1, "The duration must be at least -1");
            this.duration = duration;
            return self();
        }

        /**
         * Sets the interpolator, which should be used by the animations, which are created by the
         * builder.
         *
         * @param interpolator
         *         The interpolator, which should be set, as an instance of the type {@link
         *         Interpolator} or null, if the default interpolator should be used
         * @return The builder, this method has be called upon, as an instance of the generic type
         * BuilderType. The builder may not be null
         */
        @NonNull
        public final BuilderType setInterpolator(@Nullable final Interpolator interpolator) {
            this.interpolator = interpolator;
            return self();
        }

    }

    /**
     * The duration of the animation in milliseconds.
     */
    private final long duration;

    /**
     * The interpolator, which is used by the animation.
     */
    private final Interpolator interpolator;

    /**
     * Creates a new animation.
     *
     * @param duration
     *         The duration of the animation in milliseconds as a {@link Long} value or -1, if the
     *         default duration should be used
     * @param interpolator
     *         The interpolator, which should be used by the animation, as an instance of the type
     *         {@link Interpolator} or null, if the default interpolator should be used
     */
    protected Animation(final long duration, @Nullable final Interpolator interpolator) {
        ensureAtLeast(duration, -1, "The duration must be at least -1");
        this.duration = duration;
        this.interpolator = interpolator;
    }

    /**
     * Returns the duration of the animation.
     *
     * @return The duration of the animation in milliseconds as a {@link Long} value or -1, if the
     * default duration is used
     */
    public final long getDuration() {
        return duration;
    }

    /**
     * Returns the interpolator, which is used by the animation.
     *
     * @return The interpolator, which is used by the animation, as an instance of the type {@link
     * Interpolator} or null, if the default interpolator is used
     */
    @Nullable
    public final Interpolator getInterpolator() {
        return interpolator;
    }

}