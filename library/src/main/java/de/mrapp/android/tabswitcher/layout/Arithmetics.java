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

package de.mrapp.android.tabswitcher.layout;

import android.support.annotation.NonNull;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewPropertyAnimator;

import de.mrapp.android.tabswitcher.TabSwitcher;
import de.mrapp.android.tabswitcher.layout.AbstractDragHandler.DragState;

/**
 * Defines the interface, a class, which provides methods, which allow to calculate the position,
 * size and rotation of a {@link TabSwitcher}'s children, must implement.
 *
 * @author Michael Rapp
 * @since 0.1.0
 */
public interface Arithmetics {

    /**
     * Contains all axes on which the tabs of a {@link TabSwitcher} can be moved.
     */
    enum Axis {

        /**
         * The axis on which a tab is moved when dragging it.
         */
        DRAGGING_AXIS,

        /**
         * The axis on which a tab is moved, when it is added to or removed from the switcher.
         */
        ORTHOGONAL_AXIS,

        /**
         * The horizontal axis.
         */
        X_AXIS,

        /**
         * The vertical axis.
         */
        Y_AXIS

    }

    /**
     * Returns the position of a motion event on a specific axis.
     *
     * @param axis
     *         The axis as a value of the enum {@link Axis}. The axis may not be null
     * @param event
     *         The motion event, whose position should be returned, as an instance of the class
     *         {@link MotionEvent}. The motion event may not be null
     * @return The position of the given motion event on the given axis as a {@link Float} value
     */
    float getPosition(@NonNull Axis axis, @NonNull MotionEvent event);

    /**
     * Returns the position of a view on a specific axis.
     *
     * @param axis
     *         The axis as a value of the enum {@link Axis}. The axis may not be null
     * @param view
     *         The view, whose position should be returned, as an instance of the class {@link
     *         View}. The view may not be null
     * @return The position of the given view on the given axis as a {@link Float} value
     */
    float getPosition(@NonNull Axis axis, @NonNull View view);

    /**
     * Sets the position of a view on a specific axis.
     *
     * @param axis
     *         The axis as a value of the enum {@link Axis}. The axis may not be null
     * @param view
     *         The view, whose position should be set, as an instance of the class {@link View}. The
     *         view may not be null
     * @param position
     *         The position, which should be set, as a {@link Float} value
     */
    void setPosition(@NonNull Axis axis, @NonNull View view, float position);

    /**
     * Animates the position of a view on a specific axis.
     *
     * @param axis
     *         The axis as a value of the enum {@link Axis}. The axis may not be null
     * @param animator
     *         The animator, which should be used to animate the position, as an instance of the
     *         class {@link ViewPropertyAnimator}. The animator may not be null
     * @param view
     *         The view, whose position should be animated, as an instance of the class {@link
     *         View}. The view may not be null
     * @param position
     *         The position, which should be set by the animation, as a {@link Float} value
     * @param includePadding
     *         True, if the view's padding should be taken into account, false otherwise
     */
    void animatePosition(@NonNull Axis axis, @NonNull ViewPropertyAnimator animator,
                         @NonNull View view, float position, boolean includePadding);

    /**
     * Returns the padding of a view on a specific axis and using a specific gravity.
     *
     * @param axis
     *         The axis as a value of the enum {@link Axis}. The axis may not be null
     * @param gravity
     *         The gravity as an {@link Integer} value. The gravity must be
     *         <code>Gravity.START</code> or <code>Gravity.END</code>
     * @param view
     *         The view, whose padding should be returned, as an instance of the class {@link View}.
     *         The view may not be null
     * @return The padding of the given view on the given axis and using the given gravity as an
     * {@link Integer} value
     */
    int getPadding(@NonNull Axis axis, int gravity, @NonNull View view);

    /**
     * Returns the scale of a view, depending on its margin.
     *
     * @param view
     *         The view, whose scale should be returned, as an instance of the class {@link View}.
     *         The view may not be null
     * @param includePadding
     *         True, if the view's padding should be taken into account as well, false otherwise
     * @return The scale of the given view as a {@link Float} value
     */
    float getScale(@NonNull final View view, final boolean includePadding);

    /**
     * Sets the scale of a view on a specific axis.
     *
     * @param axis
     *         The axis as a value of the enum {@link Axis}. The axis may not be null
     * @param view
     *         The view, whose scale should be set, as an instance of the class {@link View}. The
     *         view may not be null
     * @param scale
     *         The scale, which should be set, as a {@link Float} value
     */
    void setScale(@NonNull Axis axis, @NonNull View view, float scale);

    /**
     * Animates the scale of a view on a specific axis.
     *
     * @param axis
     *         The axis as a value of the enum {@link Axis}. The axis may not be null
     * @param animator
     *         The animator, which should be used to animate the scale, as an instance of the class
     *         {@link ViewPropertyAnimator}. The animator may not be null
     * @param scale
     *         The scale, which should be set by the animation, as a {@link Float} value
     */
    void animateScale(@NonNull Axis axis, @NonNull ViewPropertyAnimator animator, float scale);

    /**
     * Returns the size of a view on a specific axis.
     *
     * @param axis
     *         The axis as a value of the enum {@link Axis}. The axis may not be null
     * @param view
     *         The view, whose size should be returned, as an instance of the class {@link View}.
     *         The view may not be null
     * @return The size of the given view on the given axis as a {@link Float} value
     */
    float getSize(@NonNull Axis axis, @NonNull View view);

    /**
     * Returns the size of the container, which contains the tab switcher's tabs, on a specific
     * axis. By default, the padding and the size of the toolbars are included.
     *
     * @param axis
     *         The axis as a value of the enum {@link Axis}. The axis may not be null
     * @return The size of the container, which contains the tab switcher's tabs, on the given axis
     * as a {@link Float} value
     */
    float getTabContainerSize(@NonNull Axis axis);

    /**
     * Returns the size of the container, which contains the tab switcher's tabs, on a specific
     * axis.
     *
     * @param axis
     *         The axis as a value of the enum {@link Axis}. The axis may not be null
     * @param includePadding
     *         True, if the padding and the size of the toolbars should be included, false
     *         otherwise
     * @return The size of the container, which contains the tab switcher's tabs, on the given axis
     * as a {@link Float} value
     */
    float getTabContainerSize(@NonNull Axis axis, boolean includePadding);

    /**
     * Returns the pivot of a view on a specific axis, depending on the current drag state.
     *
     * @param axis
     *         The axis as a value of the enum {@link Axis}. The axis may not be null
     * @param view
     *         The view, whose pivot should be returned, as an instance of the class {@link View}.
     *         The view may not be null
     * @param dragState
     *         The current drag state as a value of the enum {@link DragState}. The drag state may
     *         not be null
     * @return The pivot of the given view on the given axis as a {@link Float} value
     */
    float getPivot(@NonNull Axis axis, @NonNull View view, @NonNull DragState dragState);

    /**
     * Sets the pivot of a view on a specific axis.
     *
     * @param axis
     *         The axis as a value of the enum {@link Axis}. The axis may not be null
     * @param view
     *         The view, whose pivot should be set, as an instance of the class {@link View}. The
     *         view may not be null
     * @param pivot
     *         The pivot, which should be set, as a {@link Float} value
     */
    void setPivot(@NonNull Axis axis, @NonNull View view, float pivot);

    /**
     * Returns the rotation of a view on a specific axis.
     *
     * @param axis
     *         The axis as a value of the enum {@link Axis}. The axis may not be null
     * @param view
     *         The view, whose rotation should be returned, as an instance of the class {@link
     *         View}. The view may not be null
     * @return The rotation of the given view on the given axis as a {@link Float} value
     */
    float getRotation(@NonNull Axis axis, @NonNull View view);

    /**
     * Sets the rotation of a view on a specific axis.
     *
     * @param axis
     *         The axis as a value of the enum {@link Axis}. The axis may not be null
     * @param view
     *         The view, whose rotation should be set, as an instance of the class {@link View}. The
     *         view may not be null
     * @param angle
     *         The rotation, which should be set, as a {@link Float} value
     */
    void setRotation(@NonNull Axis axis, @NonNull View view, float angle);

    /**
     * Animates the rotation of a view on a specific axis.
     *
     * @param axis
     *         The axis as a value of the enum {@link Axis}. The axis may not be null
     * @param animator
     *         The animator, should be used to animate the rotation, as an instance of the class
     *         {@link ViewPropertyAnimator}. The animator may not be null
     * @param angle
     *         The rotation, which should be set by the animation, as a {@link Float} value
     */
    void animateRotation(@NonNull Axis axis, @NonNull ViewPropertyAnimator animator, float angle);

}