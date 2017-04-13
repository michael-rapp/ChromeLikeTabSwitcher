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

/**
 * A peek animation, which animates the size of a tab starting at a specific position in order to
 * show the tab for a short time at the end of a {@link TabSwitcher}. Peek animations can be used to
 * add tabs while the tab switcher is not shown and when using the smartphone layout. They are meant
 * to be used when adding a tab without selecting it and enable the user to peek at the added tab
 * for a short moment.
 *
 * @author Michael Rapp
 * @since 0.1.0
 */
public class PeekAnimation extends Animation {

    /**
     * A builder, which allows to configure and create instances of the class {@link
     * PeekAnimation}.
     */
    public static class Builder extends Animation.Builder<PeekAnimation, Builder> {

        /**
         * The horizontal position, the animations, which are created by the builder, start at.
         */
        private float x;

        /**
         * The vertical position, the animations, which are created by the builder, start at.
         */
        private float y;

        /**
         * Creates a new builder, which allows to configure and create instances of the class {@link
         * PeekAnimation}.
         */
        public Builder() {
            setX(0);
            setY(0);
        }

        /**
         * Sets the horizontal position, the animations, which are created by the builder, should
         * start at.
         *
         * @param x
         *         The horizontal position, which should be set, in pixels as a {@link Float} value
         * @return The builder, this method has be called upon, as an instance of the generic type
         * BuilderType. The builder may not be null
         */
        @NonNull
        public final Builder setX(final float x) {
            this.x = x;
            return self();
        }

        /**
         * Sets the vertical position, the animations, which are created by the builder, should
         * start at.
         *
         * @param y
         *         The vertical position, which should be set, in pixels as a {@link Float} value
         * @return The builder, this method has be called upon, as an instance of the generic type
         * BuilderType. The builder may not be null
         */
        @NonNull
        public final Builder setY(final float y) {
            this.y = y;
            return self();
        }

        @NonNull
        @Override
        public final PeekAnimation create() {
            return new PeekAnimation(duration, interpolator, x, y);
        }

    }

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
     *         The horizontal position, the animation should start at, in pixels as a {@link Float}
     *         value
     * @param y
     *         The vertical position, the animation should start at, in pixels as a {@link Float}
     *         value
     */
    private PeekAnimation(final long duration, @Nullable final Interpolator interpolator,
                          final float x, final float y) {
        super(duration, interpolator);
        this.x = x;
        this.y = y;
    }

    /**
     * Returns the horizontal position, the animation starts at.
     *
     * @return The horizontal position, the animation starts at, in pixels as a {@link Float} value
     */
    public final float getX() {
        return x;
    }

    /**
     * Returns the vertical position, the animation starts at.
     *
     * @return The vertical position, the animation starts at, in pixels as a {@link Float} value
     */
    public final float getY() {
        return y;
    }

}