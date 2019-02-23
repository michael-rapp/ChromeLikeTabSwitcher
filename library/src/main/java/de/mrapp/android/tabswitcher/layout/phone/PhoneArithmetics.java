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
package de.mrapp.android.tabswitcher.layout.phone;

import android.content.res.Resources;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import de.mrapp.android.tabswitcher.Layout;
import de.mrapp.android.tabswitcher.R;
import de.mrapp.android.tabswitcher.TabSwitcher;
import de.mrapp.android.tabswitcher.layout.AbstractArithmetics;
import de.mrapp.android.tabswitcher.layout.AbstractDragTabsEventHandler.DragState;
import de.mrapp.android.tabswitcher.model.AbstractItem;
import de.mrapp.util.Condition;

/**
 * Provides methods, which allow to calculate the position, size and rotation of a {@link
 * TabSwitcher}'s tabs, when using the smartphone layout.
 *
 * @author Michael Rapp
 * @since 0.1.0
 */
public class PhoneArithmetics extends AbstractArithmetics {

    /**
     * The height of a tab's title container in pixels.
     */
    private final int tabTitleContainerHeight;

    /**
     * The inset of tabs in pixels.
     */
    private final int tabInset;

    /**
     * The number of tabs, which are contained by a stack.
     */
    private final int stackedTabCount;

    /**
     * The space between tabs, which are part of a stack, in pixels.
     */
    private final float stackedTabSpacing;

    /**
     * The pivot when overshooting at the end.
     */
    private final float endOvershootPivot;

    /**
     * Modifies a specific axis depending on the orientation of the tab switcher.
     *
     * @param axis
     *         The original axis as a value of the enum {@link Axis}. The axis may not be null
     * @return The orientation invariant axis as a value of the enum {@link Axis}. The orientation
     * invariant axis may not be null
     */
    @NonNull
    private Axis getOrientationInvariantAxis(@NonNull final Axis axis) {
        if (axis == Axis.Y_AXIS) {
            return Axis.DRAGGING_AXIS;
        } else if (axis == Axis.X_AXIS) {
            return Axis.ORTHOGONAL_AXIS;
        } else if (getTabSwitcher().getLayout() == Layout.PHONE_LANDSCAPE) {
            return axis == Axis.DRAGGING_AXIS ? Axis.ORTHOGONAL_AXIS : Axis.DRAGGING_AXIS;
        } else {
            return axis;
        }
    }

    /**
     * Returns the default pivot of an item on a specific axis.
     *
     * @param axis
     *         The axis as a value of the enum {@link Axis}. The axis may not be null
     * @param item
     *         The item, whose pivot should be returned, as an instance of the class {@link
     *         AbstractItem}. The item may not be null
     * @return The pivot of the given item on the given axis as a {@link Float} value
     */
    private float getDefaultPivot(@NonNull final Axis axis, @NonNull final AbstractItem item) {
        if (axis == Axis.DRAGGING_AXIS || axis == Axis.Y_AXIS) {
            return getTabSwitcher().getLayout() == Layout.PHONE_LANDSCAPE ?
                    getSize(axis, item) / 2f : 0;
        } else {
            return getTabSwitcher().getLayout() == Layout.PHONE_LANDSCAPE ? 0 :
                    getSize(axis, item) / 2f;
        }
    }

    /**
     * Returns the pivot of an item on a specific axis, when it is swiped.
     *
     * @param axis
     *         The axis as a value of the enum {@link Axis}. The axis may not be null
     * @param item
     *         The item, whose pivot should be returned, as an instance of the class {@link
     *         AbstractItem}. The item may not be null
     * @return The pivot of the given item on the given axis as a {@link Float} value
     */
    private float getPivotWhenSwiping(@NonNull final Axis axis, @NonNull final AbstractItem item) {
        if (axis == Axis.DRAGGING_AXIS || axis == Axis.Y_AXIS) {
            return endOvershootPivot;
        } else {
            return getDefaultPivot(axis, item);
        }
    }

    /**
     * Returns the pivot of an item on a specific axis, when overshooting at the start.
     *
     * @param axis
     *         The axis as a value of the enum {@link Axis}. The axis may not be null
     * @param item
     *         The item, whose pivot should be returned, as an instance of the class {@link
     *         AbstractItem}. The item may not be null
     * @return The pivot of the given item on the given axis as a {@link Float} value
     */
    private float getPivotWhenOvershootingAtStart(@NonNull final Axis axis,
                                                  @NonNull final AbstractItem item) {
        return getSize(axis, item) / 2f;
    }

    /**
     * Returns the pivot of an item on a specific axis, when overshooting at the end.
     *
     * @param axis
     *         The axis as a value of the enum {@link Axis}. The axis may not be null
     * @param item
     *         The item, whose pivot should be returned, as an instance of the class {@link
     *         AbstractItem}. The item may not be null
     * @return The pivot of the given item on the given axis as a {@link Float} value
     */
    private float getPivotWhenOvershootingAtEnd(@NonNull final Axis axis,
                                                @NonNull final AbstractItem item) {
        if (axis == Axis.DRAGGING_AXIS || axis == Axis.Y_AXIS) {
            return getTabSwitcher().getCount() > 1 ? endOvershootPivot : getSize(axis, item) / 2f;
        } else {
            return getSize(axis, item) / 2f;
        }
    }

    /**
     * Creates a new class, which provides methods, which allow to calculate the position, size and
     * rotation of a {@link TabSwitcher}'s tabs, when using the smartphone layout.
     *
     * @param tabSwitcher
     *         The tab switcher, the arithmetics should be calculated for, as an instance of the
     *         class {@link TabSwitcher}. The tab switcher may not be null
     */
    public PhoneArithmetics(@NonNull final TabSwitcher tabSwitcher) {
        super(tabSwitcher);
        Resources resources = tabSwitcher.getResources();
        tabTitleContainerHeight =
                resources.getDimensionPixelSize(R.dimen.tab_title_container_height);
        tabInset = resources.getDimensionPixelSize(R.dimen.tab_inset);
        stackedTabCount = resources.getInteger(R.integer.phone_stacked_tab_count);
        stackedTabSpacing = resources.getDimensionPixelSize(R.dimen.stacked_tab_spacing);
        endOvershootPivot = resources.getDimensionPixelSize(R.dimen.end_overshoot_pivot);
    }

    @Override
    public final int getTabSwitcherPadding(@NonNull final Axis axis, final int gravity) {
        Condition.INSTANCE.ensureNotNull(axis, "The axis may not be null");
        Condition.INSTANCE
                .ensureTrue(gravity == Gravity.START || gravity == Gravity.END, "Invalid gravity");

        if (getOrientationInvariantAxis(axis) == Axis.DRAGGING_AXIS) {
            return gravity == Gravity.START ? getTabSwitcher().getPaddingTop() :
                    getTabSwitcher().getPaddingBottom();
        } else {
            return gravity == Gravity.START ? getTabSwitcher().getPaddingLeft() :
                    getTabSwitcher().getPaddingRight();
        }
    }

    @Override
    public final float getTabContainerSize(@NonNull final Axis axis, final boolean includePadding) {
        Condition.INSTANCE.ensureNotNull(axis, "The axis may not be null");
        ViewGroup tabContainer = getTabSwitcher().getTabContainer();
        assert tabContainer != null;
        FrameLayout.LayoutParams layoutParams =
                (FrameLayout.LayoutParams) tabContainer.getLayoutParams();
        int padding = !includePadding ? (getTabSwitcherPadding(axis, Gravity.START) +
                getTabSwitcherPadding(axis, Gravity.END)) : 0;
        Toolbar[] toolbars = getTabSwitcher().getToolbars();

        if (getOrientationInvariantAxis(axis) == Axis.DRAGGING_AXIS) {
            int toolbarSize =
                    !includePadding && getTabSwitcher().areToolbarsShown() && toolbars != null ?
                            toolbars[0].getHeight() - tabInset : 0;
            return tabContainer.getHeight() - layoutParams.topMargin - layoutParams.bottomMargin -
                    padding - toolbarSize;
        } else {
            return tabContainer.getWidth() - layoutParams.leftMargin - layoutParams.rightMargin -
                    padding;
        }
    }

    @Override
    public final float getTouchPosition(@NonNull final Axis axis,
                                        @NonNull final MotionEvent event) {
        Condition.INSTANCE.ensureNotNull(axis, "The axis may not be null");
        Condition.INSTANCE.ensureNotNull(event, "The motion event may not be null");

        if (getOrientationInvariantAxis(axis) == Axis.DRAGGING_AXIS) {
            return event.getY();
        } else {
            return event.getX();
        }
    }

    @Override
    public final float getPosition(@NonNull final Axis axis, @NonNull final AbstractItem item) {
        Condition.INSTANCE.ensureNotNull(axis, "The axis may not be null");
        Condition.INSTANCE.ensureNotNull(item, "The item may not be null");
        View view = item.getView();

        if (getOrientationInvariantAxis(axis) == Axis.DRAGGING_AXIS) {
            Toolbar[] toolbars = getTabSwitcher().getToolbars();
            return view.getY() -
                    (getTabSwitcher().areToolbarsShown() && getTabSwitcher().isSwitcherShown() &&
                            toolbars != null ?
                            toolbars[TabSwitcher.PRIMARY_TOOLBAR_INDEX].getHeight() - tabInset :
                            0) - getTabSwitcherPadding(axis, Gravity.START);
        } else {
            FrameLayout.LayoutParams layoutParams =
                    (FrameLayout.LayoutParams) view.getLayoutParams();
            return view.getX() - layoutParams.leftMargin - getTabSwitcher().getPaddingLeft() / 2f +
                    getTabSwitcher().getPaddingRight() / 2f +
                    (getTabSwitcher().getLayout() == Layout.PHONE_LANDSCAPE &&
                            getTabSwitcher().isSwitcherShown() ?
                            stackedTabCount * stackedTabSpacing / 2f : 0);
        }
    }

    @Override
    public final void setPosition(@NonNull final Axis axis, @NonNull final AbstractItem item,
                                  final float position) {
        Condition.INSTANCE.ensureNotNull(axis, "The axis may not be null");
        Condition.INSTANCE.ensureNotNull(item, "The item may not be null");
        View view = item.getView();

        if (getOrientationInvariantAxis(axis) == Axis.DRAGGING_AXIS) {
            Toolbar[] toolbars = getTabSwitcher().getToolbars();
            view.setY((getTabSwitcher().areToolbarsShown() && getTabSwitcher().isSwitcherShown() &&
                    toolbars != null ?
                    toolbars[TabSwitcher.PRIMARY_TOOLBAR_INDEX].getHeight() - tabInset : 0) +
                    getTabSwitcherPadding(axis, Gravity.START) + position);
        } else {
            FrameLayout.LayoutParams layoutParams =
                    (FrameLayout.LayoutParams) view.getLayoutParams();
            view.setX(position + layoutParams.leftMargin + getTabSwitcher().getPaddingLeft() / 2f -
                    getTabSwitcher().getPaddingRight() / 2f -
                    (getTabSwitcher().getLayout() == Layout.PHONE_LANDSCAPE &&
                            getTabSwitcher().isSwitcherShown() ?
                            stackedTabCount * stackedTabSpacing / 2f : 0));
        }
    }

    @Override
    public final void animatePosition(@NonNull final Axis axis,
                                      @NonNull final ViewPropertyAnimator animator,
                                      @NonNull final AbstractItem item, final float position,
                                      final boolean includePadding) {
        Condition.INSTANCE.ensureNotNull(axis, "The axis may not be null");
        Condition.INSTANCE.ensureNotNull(animator, "The animator may not be null");
        Condition.INSTANCE.ensureNotNull(item, "The item may not be null");

        if (getOrientationInvariantAxis(axis) == Axis.DRAGGING_AXIS) {
            Toolbar[] toolbars = getTabSwitcher().getToolbars();
            animator.y((getTabSwitcher().areToolbarsShown() && getTabSwitcher().isSwitcherShown() &&
                    toolbars != null ?
                    toolbars[TabSwitcher.PRIMARY_TOOLBAR_INDEX].getHeight() - tabInset : 0) +
                    (includePadding ? getTabSwitcherPadding(axis, Gravity.START) : 0) + position);
        } else {
            View view = item.getView();
            FrameLayout.LayoutParams layoutParams =
                    (FrameLayout.LayoutParams) view.getLayoutParams();
            animator.x(position + layoutParams.leftMargin + (includePadding ?
                    getTabSwitcher().getPaddingLeft() / 2f -
                            getTabSwitcher().getPaddingRight() / 2f : 0) -
                    (getTabSwitcher().getLayout() == Layout.PHONE_LANDSCAPE &&
                            getTabSwitcher().isSwitcherShown() ?
                            stackedTabCount * stackedTabSpacing / 2f : 0));
        }
    }

    @Override
    public final float getScale(@NonNull final AbstractItem item, final boolean includePadding) {
        Condition.INSTANCE.ensureNotNull(item, "The item may not be null");
        View view = item.getView();
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) view.getLayoutParams();
        float width = view.getWidth();
        float targetWidth = width + layoutParams.leftMargin + layoutParams.rightMargin -
                (includePadding ?
                        getTabSwitcher().getPaddingLeft() + getTabSwitcher().getPaddingRight() :
                        0) - (getTabSwitcher().getLayout() == Layout.PHONE_LANDSCAPE ?
                stackedTabCount * stackedTabSpacing : 0);
        return targetWidth / width;
    }

    @Override
    public final void setScale(@NonNull final Axis axis, @NonNull final AbstractItem item,
                               final float scale) {
        Condition.INSTANCE.ensureNotNull(axis, "The axis may not be null");
        Condition.INSTANCE.ensureNotNull(item, "The item may not be null");
        View view = item.getView();

        if (getOrientationInvariantAxis(axis) == Axis.DRAGGING_AXIS) {
            view.setScaleY(scale);
        } else {
            view.setScaleX(scale);
        }
    }

    @Override
    public final void animateScale(@NonNull final Axis axis,
                                   @NonNull final ViewPropertyAnimator animator,
                                   final float scale) {
        Condition.INSTANCE.ensureNotNull(axis, "The axis may not be null");
        Condition.INSTANCE.ensureNotNull(animator, "The animator may not be null");

        if (getOrientationInvariantAxis(axis) == Axis.DRAGGING_AXIS) {
            animator.scaleY(scale);
        } else {
            animator.scaleX(scale);
        }
    }

    @Override
    public final float getSize(@NonNull final Axis axis, @NonNull final AbstractItem item) {
        Condition.INSTANCE.ensureNotNull(axis, "The axis may not be null");
        Condition.INSTANCE.ensureNotNull(item, "The item may not be null");
        View view = item.getView();

        if (getOrientationInvariantAxis(axis) == Axis.DRAGGING_AXIS) {
            return view.getHeight() * getScale(item);
        } else {
            return view.getWidth() * getScale(item);
        }
    }

    @Override
    public final float getPivot(@NonNull final Axis axis, @NonNull final AbstractItem item,
                                @NonNull final DragState dragState) {
        Condition.INSTANCE.ensureNotNull(axis, "The axis may not be null");
        Condition.INSTANCE.ensureNotNull(item, "The item may not be null");
        Condition.INSTANCE.ensureNotNull(dragState, "The drag state may not be null");

        if (dragState == DragState.SWIPE) {
            return getPivotWhenSwiping(axis, item);
        } else if (dragState == DragState.OVERSHOOT_START) {
            return getPivotWhenOvershootingAtStart(axis, item);
        } else if (dragState == DragState.OVERSHOOT_END) {
            return getPivotWhenOvershootingAtEnd(axis, item);
        } else {
            return getDefaultPivot(axis, item);
        }
    }

    @Override
    public final void setPivot(@NonNull final Axis axis, @NonNull final AbstractItem item,
                               final float pivot) {
        Condition.INSTANCE.ensureNotNull(axis, "The axis may not be null");
        Condition.INSTANCE.ensureNotNull(item, "The item may not be null");
        View view = item.getView();
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) view.getLayoutParams();

        if (getOrientationInvariantAxis(axis) == Axis.DRAGGING_AXIS) {
            float newPivot = pivot - layoutParams.topMargin - tabTitleContainerHeight;
            view.setTranslationY(view.getTranslationY() +
                    (view.getPivotY() - newPivot) * (1 - view.getScaleY()));
            view.setPivotY(newPivot);
        } else {
            float newPivot = pivot - layoutParams.leftMargin;
            view.setTranslationX(view.getTranslationX() +
                    (view.getPivotX() - newPivot) * (1 - view.getScaleX()));
            view.setPivotX(newPivot);
        }
    }

    @Override
    public final float getRotation(@NonNull final Axis axis, @NonNull final AbstractItem item) {
        Condition.INSTANCE.ensureNotNull(axis, "The axis may not be null");
        Condition.INSTANCE.ensureNotNull(item, "The view may not be null");
        View view = item.getView();

        if (getOrientationInvariantAxis(axis) == Axis.DRAGGING_AXIS) {
            return view.getRotationY();
        } else {
            return view.getRotationX();
        }
    }

    @Override
    public final void setRotation(@NonNull final Axis axis, @NonNull final AbstractItem item,
                                  final float angle) {
        Condition.INSTANCE.ensureNotNull(axis, "The axis may not be null");
        Condition.INSTANCE.ensureNotNull(item, "The item may not be null");
        View view = item.getView();

        if (getOrientationInvariantAxis(axis) == Axis.DRAGGING_AXIS) {
            view.setRotationY(
                    getTabSwitcher().getLayout() == Layout.PHONE_LANDSCAPE ? -1 * angle : angle);
        } else {
            view.setRotationX(
                    getTabSwitcher().getLayout() == Layout.PHONE_LANDSCAPE ? -1 * angle : angle);
        }
    }

    @Override
    public final void animateRotation(@NonNull final Axis axis,
                                      @NonNull final ViewPropertyAnimator animator,
                                      final float angle) {
        Condition.INSTANCE.ensureNotNull(axis, "The axis may not be null");
        Condition.INSTANCE.ensureNotNull(animator, "The animator may not be null");

        if (getOrientationInvariantAxis(axis) == Axis.DRAGGING_AXIS) {
            animator.rotationY(
                    getTabSwitcher().getLayout() == Layout.PHONE_LANDSCAPE ? -1 * angle : angle);
        } else {
            animator.rotationX(
                    getTabSwitcher().getLayout() == Layout.PHONE_LANDSCAPE ? -1 * angle : angle);
        }
    }

}