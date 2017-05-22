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
package de.mrapp.android.tabswitcher.layout.phone;

import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.widget.FrameLayout;

import de.mrapp.android.tabswitcher.Layout;
import de.mrapp.android.tabswitcher.R;
import de.mrapp.android.tabswitcher.TabSwitcher;
import de.mrapp.android.tabswitcher.layout.AbstractDragHandler.DragState;
import de.mrapp.android.tabswitcher.layout.Arithmetics;

import static de.mrapp.android.util.Condition.ensureNotNull;
import static de.mrapp.android.util.Condition.ensureTrue;

/**
 * Provides methods, which allow to calculate the position, size and rotation of a {@link
 * TabSwitcher}'s children, when using the smartphone layout.
 *
 * @author Michael Rapp
 * @since 0.1.0
 */
public class PhoneArithmetics implements Arithmetics {

    /**
     * The tab switcher, the arithmetics are calculated for.
     */
    private final TabSwitcher tabSwitcher;

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
        } else if (tabSwitcher.getLayout() == Layout.PHONE_LANDSCAPE) {
            return axis == Axis.DRAGGING_AXIS ? Axis.ORTHOGONAL_AXIS : Axis.DRAGGING_AXIS;
        } else {
            return axis;
        }
    }

    /**
     * Returns the default pivot of a view on a specific axis.
     *
     * @param axis
     *         The axis as a value of the enum {@link Axis}. The axis may not be null
     * @param view
     *         The view, whose pivot should be returned, as an instance of the class {@link View}.
     *         The view may not be null
     * @return The pivot of the given view on the given axis as a {@link Float} value
     */
    private float getDefaultPivot(@NonNull final Axis axis, @NonNull final View view) {
        if (axis == Axis.DRAGGING_AXIS || axis == Axis.Y_AXIS) {
            return tabSwitcher.getLayout() == Layout.PHONE_LANDSCAPE ? getSize(axis, view) / 2f : 0;
        } else {
            return tabSwitcher.getLayout() == Layout.PHONE_LANDSCAPE ? 0 : getSize(axis, view) / 2f;
        }
    }

    /**
     * Returns the pivot of a view on a specific axis, when it is swiped.
     *
     * @param axis
     *         The axis as a value of the enum {@link Axis}. The axis may not be null
     * @param view
     *         The view, whose pivot should be returned, as an instance of the class {@link View}.
     *         The view may not be null
     * @return The pivot of the given view on the given axis as a {@link Float} value
     */
    private float getPivotWhenSwiping(@NonNull final Axis axis, @NonNull final View view) {
        if (axis == Axis.DRAGGING_AXIS || axis == Axis.Y_AXIS) {
            return endOvershootPivot;
        } else {
            return getDefaultPivot(axis, view);
        }
    }

    /**
     * Returns the pivot of a view on a specific axis, when overshooting at the start.
     *
     * @param axis
     *         The axis as a value of the enum {@link Axis}. The axis may not be null
     * @param view
     *         The view, whose pivot should be returned, as an instance of the class {@link View}.
     *         The view may not be null
     * @return The pivot of the given view on the given axis as a {@link Float} value
     */
    private float getPivotWhenOvershootingAtStart(@NonNull final Axis axis,
                                                  @NonNull final View view) {
        return getSize(axis, view) / 2f;
    }

    /**
     * Returns the pivot of a view on a specific axis, when overshooting at the end.
     *
     * @param axis
     *         The axis as a value of the enum {@link Axis}. The axis may not be null
     * @param view
     *         The view, whose pivot should be returned, as an instance of the class {@link View}.
     *         The view may not be null
     * @return The pivot of the given view on the given axis as a {@link Float} value
     */
    private float getPivotWhenOvershootingAtEnd(@NonNull final Axis axis,
                                                @NonNull final View view) {
        if (axis == Axis.DRAGGING_AXIS || axis == Axis.Y_AXIS) {
            return tabSwitcher.getCount() > 1 ? endOvershootPivot : getSize(axis, view) / 2f;
        } else {
            return getSize(axis, view) / 2f;
        }
    }

    /**
     * Creates a new class, which provides methods, which allow to calculate the position, size and
     * rotation of a {@link TabSwitcher}'s children.
     *
     * @param tabSwitcher
     *         The tab switcher, the arithmetics should be calculated for, as an instance of the
     *         class {@link TabSwitcher}. The tab switcher may not be null
     */
    public PhoneArithmetics(@NonNull final TabSwitcher tabSwitcher) {
        ensureNotNull(tabSwitcher, "The tab switcher may not be null");
        this.tabSwitcher = tabSwitcher;
        Resources resources = tabSwitcher.getResources();
        this.tabTitleContainerHeight =
                resources.getDimensionPixelSize(R.dimen.tab_title_container_height);
        this.tabInset = resources.getDimensionPixelSize(R.dimen.tab_inset);
        this.stackedTabCount = resources.getInteger(R.integer.stacked_tab_count);
        this.stackedTabSpacing = resources.getDimensionPixelSize(R.dimen.stacked_tab_spacing);
        this.endOvershootPivot = resources.getDimensionPixelSize(R.dimen.end_overshoot_pivot);
    }

    @Override
    public final float getPosition(@NonNull final Axis axis, @NonNull final MotionEvent event) {
        ensureNotNull(axis, "The axis may not be null");
        ensureNotNull(event, "The motion event may not be null");

        if (getOrientationInvariantAxis(axis) == Axis.DRAGGING_AXIS) {
            return event.getY();
        } else {
            return event.getX();
        }
    }

    @Override
    public final float getPosition(@NonNull final Axis axis, @NonNull final View view) {
        ensureNotNull(axis, "The axis may not be null");
        ensureNotNull(view, "The view may not be null");

        if (getOrientationInvariantAxis(axis) == Axis.DRAGGING_AXIS) {
            Toolbar[] toolbars = tabSwitcher.getToolbars();
            return view.getY() - (tabSwitcher.areToolbarsShown() && tabSwitcher.isSwitcherShown() &&
                    toolbars != null ? toolbars[0].getHeight() - tabInset : 0) -
                    getPadding(axis, Gravity.START, tabSwitcher);
        } else {
            FrameLayout.LayoutParams layoutParams =
                    (FrameLayout.LayoutParams) view.getLayoutParams();
            return view.getX() - layoutParams.leftMargin - tabSwitcher.getPaddingLeft() / 2f +
                    tabSwitcher.getPaddingRight() / 2f +
                    (tabSwitcher.getLayout() == Layout.PHONE_LANDSCAPE &&
                            tabSwitcher.isSwitcherShown() ?
                            stackedTabCount * stackedTabSpacing / 2f : 0);
        }
    }

    @Override
    public final void setPosition(@NonNull final Axis axis, @NonNull final View view,
                                  final float position) {
        ensureNotNull(axis, "The axis may not be null");
        ensureNotNull(view, "The view may not be null");

        if (getOrientationInvariantAxis(axis) == Axis.DRAGGING_AXIS) {
            Toolbar[] toolbars = tabSwitcher.getToolbars();
            view.setY((tabSwitcher.areToolbarsShown() && tabSwitcher.isSwitcherShown() &&
                    toolbars != null ? toolbars[0].getHeight() - tabInset : 0) +
                    getPadding(axis, Gravity.START, tabSwitcher) + position);
        } else {
            FrameLayout.LayoutParams layoutParams =
                    (FrameLayout.LayoutParams) view.getLayoutParams();
            view.setX(position + layoutParams.leftMargin + tabSwitcher.getPaddingLeft() / 2f -
                    tabSwitcher.getPaddingRight() / 2f -
                    (tabSwitcher.getLayout() == Layout.PHONE_LANDSCAPE &&
                            tabSwitcher.isSwitcherShown() ?
                            stackedTabCount * stackedTabSpacing / 2f : 0));
        }
    }

    @Override
    public final void animatePosition(@NonNull final Axis axis,
                                      @NonNull final ViewPropertyAnimator animator,
                                      @NonNull final View view, final float position,
                                      final boolean includePadding) {
        ensureNotNull(axis, "The axis may not be null");
        ensureNotNull(animator, "The animator may not be null");
        ensureNotNull(view, "The view may not be null");

        if (getOrientationInvariantAxis(axis) == Axis.DRAGGING_AXIS) {
            Toolbar[] toolbars = tabSwitcher.getToolbars();
            animator.y((tabSwitcher.areToolbarsShown() && tabSwitcher.isSwitcherShown() &&
                    toolbars != null ? toolbars[0].getHeight() - tabInset : 0) +
                    (includePadding ? getPadding(axis, Gravity.START, tabSwitcher) : 0) + position);
        } else {
            FrameLayout.LayoutParams layoutParams =
                    (FrameLayout.LayoutParams) view.getLayoutParams();
            animator.x(position + layoutParams.leftMargin + (includePadding ?
                    tabSwitcher.getPaddingLeft() / 2f - tabSwitcher.getPaddingRight() / 2f : 0) -
                    (tabSwitcher.getLayout() == Layout.PHONE_LANDSCAPE &&
                            tabSwitcher.isSwitcherShown() ?
                            stackedTabCount * stackedTabSpacing / 2f : 0));
        }
    }

    @Override
    public final int getPadding(@NonNull final Axis axis, final int gravity,
                                @NonNull final View view) {
        ensureNotNull(axis, "The axis may not be null");
        ensureTrue(gravity == Gravity.START || gravity == Gravity.END, "Invalid gravity");
        ensureNotNull(view, "The view may not be null");

        if (getOrientationInvariantAxis(axis) == Axis.DRAGGING_AXIS) {
            return gravity == Gravity.START ? view.getPaddingTop() : view.getPaddingBottom();
        } else {
            return gravity == Gravity.START ? view.getPaddingLeft() : view.getPaddingRight();
        }
    }

    @Override
    public final float getScale(@NonNull final View view, final boolean includePadding) {
        ensureNotNull(view, "The view may not be null");
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) view.getLayoutParams();
        float width = view.getWidth();
        float targetWidth = width + layoutParams.leftMargin + layoutParams.rightMargin -
                (includePadding ? tabSwitcher.getPaddingLeft() + tabSwitcher.getPaddingRight() :
                        0) - (tabSwitcher.getLayout() == Layout.PHONE_LANDSCAPE ?
                stackedTabCount * stackedTabSpacing : 0);
        return targetWidth / width;
    }

    @Override
    public final void setScale(@NonNull final Axis axis, @NonNull final View view,
                               final float scale) {
        ensureNotNull(axis, "The axis may not be null");
        ensureNotNull(view, "The view may not be null");

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
        ensureNotNull(axis, "The axis may not be null");
        ensureNotNull(animator, "The animator may not be null");

        if (getOrientationInvariantAxis(axis) == Axis.DRAGGING_AXIS) {
            animator.scaleY(scale);
        } else {
            animator.scaleX(scale);
        }
    }

    @Override
    public final float getSize(@NonNull final Axis axis, @NonNull final View view) {
        ensureNotNull(axis, "The axis may not be null");
        ensureNotNull(view, "The view may not be null");

        if (getOrientationInvariantAxis(axis) == Axis.DRAGGING_AXIS) {
            return view.getHeight() * getScale(view, false);
        } else {
            return view.getWidth() * getScale(view, false);
        }
    }

    @Override
    public final float getTabContainerSize(@NonNull final Axis axis) {
        return getTabContainerSize(axis, true);
    }

    @Override
    public final float getTabContainerSize(@NonNull final Axis axis, final boolean includePadding) {
        ensureNotNull(axis, "The axis may not be null");
        ViewGroup tabContainer = tabSwitcher.getTabContainer();
        assert tabContainer != null;
        FrameLayout.LayoutParams layoutParams =
                (FrameLayout.LayoutParams) tabContainer.getLayoutParams();
        int padding = !includePadding ? (getPadding(axis, Gravity.START, tabSwitcher) +
                getPadding(axis, Gravity.END, tabSwitcher)) : 0;
        Toolbar[] toolbars = tabSwitcher.getToolbars();

        if (getOrientationInvariantAxis(axis) == Axis.DRAGGING_AXIS) {
            int toolbarSize =
                    !includePadding && tabSwitcher.areToolbarsShown() && toolbars != null ?
                            toolbars[0].getHeight() - tabInset : 0;
            return tabContainer.getHeight() - layoutParams.topMargin - layoutParams.bottomMargin -
                    padding - toolbarSize;
        } else {
            return tabContainer.getWidth() - layoutParams.leftMargin - layoutParams.rightMargin -
                    padding;
        }
    }

    @Override
    public final float getPivot(@NonNull final Axis axis, @NonNull final View view,
                                @NonNull final DragState dragState) {
        ensureNotNull(axis, "The axis may not be null");
        ensureNotNull(view, "The view may not be null");
        ensureNotNull(dragState, "The drag state may not be null");

        if (dragState == DragState.SWIPE) {
            return getPivotWhenSwiping(axis, view);
        } else if (dragState == DragState.OVERSHOOT_START) {
            return getPivotWhenOvershootingAtStart(axis, view);
        } else if (dragState == DragState.OVERSHOOT_END) {
            return getPivotWhenOvershootingAtEnd(axis, view);
        } else {
            return getDefaultPivot(axis, view);
        }
    }

    @Override
    public final void setPivot(@NonNull final Axis axis, @NonNull final View view,
                               final float pivot) {
        ensureNotNull(axis, "The axis may not be null");
        ensureNotNull(view, "The view may not be null");
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
    public final float getRotation(@NonNull final Axis axis, @NonNull final View view) {
        ensureNotNull(axis, "The axis may not be null");
        ensureNotNull(view, "The view may not be null");

        if (getOrientationInvariantAxis(axis) == Axis.DRAGGING_AXIS) {
            return view.getRotationY();
        } else {
            return view.getRotationX();
        }
    }

    @Override
    public final void setRotation(@NonNull final Axis axis, @NonNull final View view,
                                  final float angle) {
        ensureNotNull(axis, "The axis may not be null");
        ensureNotNull(view, "The view may not be null");

        if (getOrientationInvariantAxis(axis) == Axis.DRAGGING_AXIS) {
            view.setRotationY(
                    tabSwitcher.getLayout() == Layout.PHONE_LANDSCAPE ? -1 * angle : angle);
        } else {
            view.setRotationX(
                    tabSwitcher.getLayout() == Layout.PHONE_LANDSCAPE ? -1 * angle : angle);
        }
    }

    @Override
    public final void animateRotation(@NonNull final Axis axis,
                                      @NonNull final ViewPropertyAnimator animator,
                                      final float angle) {
        ensureNotNull(axis, "The axis may not be null");
        ensureNotNull(animator, "The animator may not be null");

        if (getOrientationInvariantAxis(axis) == Axis.DRAGGING_AXIS) {
            animator.rotationY(
                    tabSwitcher.getLayout() == Layout.PHONE_LANDSCAPE ? -1 * angle : angle);
        } else {
            animator.rotationX(
                    tabSwitcher.getLayout() == Layout.PHONE_LANDSCAPE ? -1 * angle : angle);
        }
    }

}