/*
 * Copyright 2015 - 2016 Michael Rapp
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

import static de.mrapp.android.util.Condition.ensureAtLeast;

/**
 * A helper class, which may be used to recognize drag gestures.
 *
 * @author Michael Rapp
 * @since 1.4.0
 */
public class DragHelper {

    /**
     * The distance in pixels, the gesture must last until it is recognized.
     */
    private int threshold;

    /**
     * The distance, which has been passed while dragging, in pixels or 0, if the threshold has not
     * been reached yet.
     */
    private float distance;

    /**
     * The position, where the threshold was reached or -1, if the threshold has not been reached
     * yet.
     */
    private float thresholdReachedPosition;

    /**
     * The position, where the gesture has been started at or -1, if no gesture has been started
     * yet.
     */
    private float dragStartPosition;

    /**
     * The time, when the gesture has been started or -1, if no gesture has been started yet.
     */
    private long dragStartTime;

    /**
     * True, if the method <code>reset():void</code> has been called since the last value has been
     * added, false otherwise.
     */
    private boolean reset;

    /**
     * True, if the threshold has already been reached, false otherwise.
     */
    private boolean reachedThreshold;

    /**
     * Returns, whether the threshold is reached by a specific distance.
     *
     * @param distance
     *         The distance, which should be checked, as an {@link Integer} value
     * @return True, if the threshold is reached by the given distance, false otherwise
     */
    private boolean reachedThreshold(final float distance) {
        return Math.abs(distance) >= threshold;
    }

    /**
     * Creates a new helper class, which may be used to recognize drag gestures.
     *
     * @param threshold
     *         The distance in pixels, the gesture must last until it is recognized, as an {@link
     *         Integer} value. The value must be at least 0
     */
    public DragHelper(final int threshold) {
        ensureAtLeast(threshold, 0, "The threshold must be at least 0");
        this.threshold = threshold;
        this.distance = 0;
        this.thresholdReachedPosition = -1;
        this.dragStartPosition = -1;
        this.dragStartTime = -1;
        this.reachedThreshold = false;
        reset();
    }

    /**
     * Returns the distance in pixels, the gesture must last until it is recognized.
     *
     * @return The distance in pixels, the gesture must last until it is recognized, as an {@link
     * Integer} value. The value must be at least 0
     */
    public final int getThreshold() {
        return threshold;
    }

    /**
     * Marks the instance to be reset. This will cause all properties to be reset to default values,
     * when a value is added by calling the method <code>update(float):void</code> the next time.
     * Therefore this method may be used to start recognizing a new drag gesture, whenever a value
     * is added the next time, while the values of the previous recognition can be still retrieved
     * until recognizing the new gesture begins.
     */
    public final void reset() {
        reset = true;
    }

    /**
     * Marks the instance to be reset. This will cause all properties to be reset to default values,
     * when a value is added by calling the method <code>update(float):void</code> the next time.
     * Therefore this method may be used to start recognizing a new drag gesture, whenever a value
     * is added the next time, while the values of the previous recognition can be still retrieved
     * until recognizing the new gesture begins. Furthermore, this method changes the threshold,
     * which is used by the instance.
     *
     * @param threshold
     *         The distance in pixels, the gesture must last until it is recognized, as an {@link
     *         Integer} value. The value must be at least 0
     */
    public final void reset(final int threshold) {
        reset();
        this.threshold = threshold;
    }

    /**
     * Returns, whether the instance has been marked to be reset, since the method
     * <code>update(float):void</code> has been called the last time. See method
     * <code>reset():void</code> for further information.
     *
     * @return True, if the instance has been marked to be reset, false otherwise
     */
    @Deprecated
    public final boolean isReseted() {
        return reset;
    }

    /**
     * Returns, whether the instance has been marked to be reset, since the method
     * <code>update(float):void</code> has been called the last time. See method
     * <code>reset():void</code> for further information.
     *
     * @return True, if the instance has been marked to be reset, false otherwise
     */
    public final boolean isReset() {
        return reset;
    }

    /**
     * Updates the instance by adding a new position. This will cause all properties to be
     * re-calculated, depending on the new position.
     *
     * @param position
     *         The position, which should be added, as a {@link Float} value
     */
    public final void update(final float position) {
        if (reset) {
            reset = false;
            distance = 0;
            thresholdReachedPosition = -1;
            dragStartTime = -1;
            dragStartPosition = position;
            reachedThreshold = false;
        }

        if (!reachedThreshold) {
            if (reachedThreshold(position - dragStartPosition)) {
                dragStartTime = System.currentTimeMillis();
                reachedThreshold = true;
                thresholdReachedPosition = position;
            }
        } else {
            distance = position - thresholdReachedPosition;
        }
    }

    /**
     * Returns, whether the threshold has already been reached, or not.
     *
     * @return True, if the threshold has been already reached, false otherwise
     */
    public final boolean hasThresholdBeenReached() {
        return reachedThreshold;
    }

    /**
     * Returns the distance, which has been passed while dragging, in pixels.
     *
     * @return The distance, which has been passed while dragging, as an {@link Integer} value or 0,
     * if the threshold has not been reached yet
     */
    @Deprecated
    public final int getDistance() {
        return Math.round(distance);
    }

    /**
     * Returns the distance, which has been passed while dragging, in pixels.
     *
     * @return The distance, which has been passed while dragging, as a {@link Float} value or 0, if
     * the threshold has not been reached yet
     */
    public final float getDragDistance() {
        return distance;
    }

    /**
     * Returns the position, where the gesture has been started at.
     *
     * @return The position, where the gesture has been started at, as an {@link Integer} value or
     * -1, if no gesture has been started yet
     */
    @Deprecated
    public final int getStartPosition() {
        return Math.round(dragStartPosition);
    }

    /**
     * Returns the position, where the drag gesture has been started at.
     *
     * @return The position, where the drag gesture has been started at, as a {@link Float} value or
     * -1, if no gesture has been started yet
     */
    public final float getDragStartPosition() {
        return dragStartPosition;
    }

    /**
     * Returns the speed of the drag gesture in pixels per millisecond.
     *
     * @return The speed of the drag gesture as a {@link Float} value or -1, if the threshold has
     * not been reached yet
     */
    public final float getDragSpeed() {
        if (hasThresholdBeenReached()) {
            long interval = System.currentTimeMillis() - dragStartTime;
            return (float) Math.abs(getDistance()) / (float) interval;
        } else {
            return -1;
        }
    }

}