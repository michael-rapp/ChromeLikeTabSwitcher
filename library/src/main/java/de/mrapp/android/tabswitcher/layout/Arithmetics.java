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

package de.mrapp.android.tabswitcher.layout;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewPropertyAnimator;

import androidx.annotation.NonNull;
import de.mrapp.android.tabswitcher.TabSwitcher;
import de.mrapp.android.tabswitcher.layout.AbstractDragTabsEventHandler.DragState;
import de.mrapp.android.tabswitcher.model.AbstractItem;

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
     * Returns the padding of the tab switcher on a specific axis and using a specific gravity.
     *
     * @param axis
     *         The axis as a value of the enum {@link Axis}. The axis may not be null
     * @param gravity
     *         The gravity as an {@link Integer} value. The gravity must be
     *         <code>Gravity.START</code> or <code>Gravity.END</code>
     * @return The padding of the tab switcher on the given axis and using the given gravity as an
     * {@link Integer} value
     */
    int getTabSwitcherPadding(@NonNull Axis axis, int gravity);

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
     * Returns the position of a touch event on a specific axis.
     *
     * @param axis
     *         The axis as a value of the enum {@link Axis}. The axis may not be null
     * @param event
     *         The touch event, whose position should be returned, as an instance of the class
     *         {@link MotionEvent}. The motion event may not be null
     * @return The position of the given touch event on the given axis as a {@link Float} value
     */
    float getTouchPosition(@NonNull Axis axis, @NonNull MotionEvent event);

    /**
     * Returns the position of a specific item on a specific axis.
     *
     * @param axis
     *         The axis as a value of the enum {@link Axis}. The axis may not be null
     * @param item
     *         The item, whose position should be returned, as an instance of the class {@link
     *         AbstractItem}. The item may not be null
     * @return The position of the given item on the given axis as a {@link Float} value
     */
    float getPosition(@NonNull Axis axis, @NonNull AbstractItem item);

    /**
     * Sets the position of an item on a specific axis.
     *
     * @param axis
     *         The axis as a value of the enum {@link Axis}. The axis may not be null
     * @param item
     *         The item, whose position should be set, as an instance of the class {@link
     *         AbstractItem}. The item may not be null
     * @param position
     *         The position, which should be set, as a {@link Float} value
     */
    void setPosition(@NonNull Axis axis, @NonNull AbstractItem item, float position);

    /**
     * Animates the position of an item on a specific axis. By default, the item's padding is not
     * taken into account.
     *
     * @param axis
     *         The axis as a value of the enum {@link Axis}. The axis may not be null
     * @param animator
     *         The animator, which should be used to animate the position, as an instance of the
     *         class {@link ViewPropertyAnimator}. The animator may not be null
     * @param item
     *         The item, whose position should be animated, as an instance of the class {@link
     *         View}. The view may not be null
     * @param position
     *         The position, which should be set by the animation, as a {@link Float} value
     */
    void animatePosition(@NonNull Axis axis, @NonNull ViewPropertyAnimator animator,
                         @NonNull AbstractItem item, float position);

    /**
     * Animates the position of an item on a specific axis.
     *
     * @param axis
     *         The axis as a value of the enum {@link Axis}. The axis may not be null
     * @param animator
     *         The animator, which should be used to animate the position, as an instance of the
     *         class {@link ViewPropertyAnimator}. The animator may not be null
     * @param item
     *         The item, whose position should be animated, as an instance of the class {@link
     *         View}. The view may not be null
     * @param position
     *         The position, which should be set by the animation, as a {@link Float} value
     * @param includePadding
     *         True, if the item's padding should be taken into account, false otherwise
     */
    void animatePosition(@NonNull Axis axis, @NonNull ViewPropertyAnimator animator,
                         @NonNull AbstractItem item, float position, boolean includePadding);

    /**
     * Returns the scale of an item, depending on its margin. By default, the item's padding is
     * not taken into account.
     *
     * @param item
     *         The item, whose scale should be returned, as an instance of the class {@link
     *         AbstractItem}. The item may not be null
     * @return The scale of the given item as a {@link Float} value
     */
    float getScale(@NonNull final AbstractItem item);

    /**
     * Returns the scale of an item, depending on its margin.
     *
     * @param item
     *         The item, whose scale should be returned, as an instance of the class {@link
     *         AbstractItem}. The item may not be null
     * @param includePadding
     *         True, if the item's padding should be taken into account as well, false otherwise
     * @return The scale of the given item as a {@link Float} value
     */
    float getScale(@NonNull final AbstractItem item, final boolean includePadding);

    /**
     * Sets the scale of an item on a specific axis.
     *
     * @param axis
     *         The axis as a value of the enum {@link Axis}. The axis may not be null
     * @param item
     *         The item, whose scale should be set, as an instance of the class {@link
     *         AbstractItem}. The item may not be null
     * @param scale
     *         The scale, which should be set, as a {@link Float} value
     */
    void setScale(@NonNull Axis axis, @NonNull AbstractItem item, float scale);

    /**
     * Animates the scale of an item on a specific axis.
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
     * Returns the size of an item on a specific axis.
     *
     * @param axis
     *         The axis as a value of the enum {@link Axis}. The axis may not be null
     * @param item
     *         The item, whose size should be returned, as an instance of the class {@link
     *         AbstractItem}. The item may not be null
     * @return The size of the given item on the given axis as a {@link Float} value
     */
    float getSize(@NonNull Axis axis, @NonNull AbstractItem item);

    /**
     * Returns the pivot of an item on a specific axis, depending on the current drag state.
     *
     * @param axis
     *         The axis as a value of the enum {@link Axis}. The axis may not be null
     * @param item
     *         The item, whose pivot should be returned, as an instance of the class {@link
     *         AbstractItem}. The item may not be null
     * @param dragState
     *         The current drag state as a value of the enum {@link DragState}. The drag state may
     *         not be null
     * @return The pivot of the given item on the given axis as a {@link Float} value
     */
    float getPivot(@NonNull Axis axis, @NonNull AbstractItem item, @NonNull DragState dragState);

    /**
     * Sets the pivot of an item on a specific axis.
     *
     * @param axis
     *         The axis as a value of the enum {@link Axis}. The axis may not be null
     * @param item
     *         The item, whose pivot should be set, as an instance of the class {@link View}. The
     *         item may not be null
     * @param pivot
     *         The pivot, which should be set, as a {@link Float} value
     */
    void setPivot(@NonNull Axis axis, @NonNull AbstractItem item, float pivot);

    /**
     * Returns the rotation of an item on a specific axis.
     *
     * @param axis
     *         The axis as a value of the enum {@link Axis}. The axis may not be null
     * @param item
     *         The item, whose rotation should be returned, as an instance of the class {@link
     *         AbstractItem}. The item may not be null
     * @return The rotation of the given item on the given axis as a {@link Float} value
     */
    float getRotation(@NonNull Axis axis, @NonNull AbstractItem item);

    /**
     * Sets the rotation of an item on a specific axis.
     *
     * @param axis
     *         The axis as a value of the enum {@link Axis}. The axis may not be null
     * @param item
     *         The item, whose rotation should be set, as an instance of the class {@link
     *         AbstractItem}. The item may not be null
     * @param angle
     *         The rotation, which should be set, as a {@link Float} value
     */
    void setRotation(@NonNull Axis axis, @NonNull AbstractItem item, float angle);

    /**
     * Animates the rotation of an item on a specific axis.
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