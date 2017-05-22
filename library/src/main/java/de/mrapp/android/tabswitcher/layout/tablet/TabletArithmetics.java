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
package de.mrapp.android.tabswitcher.layout.tablet;

import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewPropertyAnimator;

import de.mrapp.android.tabswitcher.R;
import de.mrapp.android.tabswitcher.TabSwitcher;
import de.mrapp.android.tabswitcher.layout.AbstractDragEventHandler.DragState;
import de.mrapp.android.tabswitcher.layout.Arithmetics;

import static de.mrapp.android.util.Condition.ensureNotNull;
import static de.mrapp.android.util.Condition.ensureTrue;

/**
 * Provides methods, which allow to calculate the position, size and rotation of a {@link
 * TabSwitcher}'s children, when using the tablet layout.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public class TabletArithmetics implements Arithmetics {

    /**
     * The tab switcher, the arithmetics are calculated for.
     */
    private final TabSwitcher tabSwitcher;

    /**
     * The height of a tab in pixels.
     */
    private final int tabHeight;

    /**
     * The height of the container, which contains tabs, in pixels.
     */
    private final int tabContainerHeight;

    /**
     * The offset between two neighboring tabs in pixels.
     */
    private final int tabOffset;

    /**
     * Creates a new class, which provides methods, which allow to calculate the position, size and
     * rotation of a {@link TabSwitcher}'s children, when using the tablet layout.
     *
     * @param tabSwitcher
     *         The tab switcher, the arithmetics should be calculated for, as an instance of the
     *         class {@link TabSwitcher}. The tab switcher may not be null
     */
    public TabletArithmetics(@NonNull final TabSwitcher tabSwitcher) {
        ensureNotNull(tabSwitcher, "The tab switcher may not be null");
        this.tabSwitcher = tabSwitcher;
        Resources resources = tabSwitcher.getResources();
        this.tabHeight = resources.getDimensionPixelSize(R.dimen.tablet_tab_height);
        this.tabContainerHeight =
                resources.getDimensionPixelSize(R.dimen.tablet_tab_container_height);
        this.tabOffset = resources.getDimensionPixelSize(R.dimen.tablet_tab_offset);
    }

    @Override
    public final float getPosition(@NonNull final Axis axis, @NonNull final MotionEvent event) {
        ensureNotNull(axis, "The axis may not be null");
        ensureNotNull(event, "The motion event may not be null");

        if (axis == Axis.DRAGGING_AXIS) {
            return event.getX();
        } else {
            return event.getY();
        }
    }

    @Override
    public final float getPosition(@NonNull final Axis axis, @NonNull final View view) {
        ensureNotNull(axis, "The axis may not be null");
        ensureNotNull(view, "The view may not be null");

        if (axis == Axis.DRAGGING_AXIS) {
            Toolbar[] toolbars = tabSwitcher.getToolbars();
            return view.getX() - (tabSwitcher.areToolbarsShown() && toolbars != null ? Math.max(0,
                    toolbars[TabSwitcher.PRIMARY_TOOLBAR_INDEX].getWidth() - tabOffset) : 0);
        } else {
            return view.getY() - (tabContainerHeight - tabHeight);
        }
    }

    @Override
    public final void setPosition(@NonNull final Axis axis, @NonNull final View view,
                                  final float position) {
        ensureNotNull(axis, "The axis may not be null");
        ensureNotNull(view, "The view may not be null");

        if (axis == Axis.DRAGGING_AXIS) {
            Toolbar[] toolbars = tabSwitcher.getToolbars();
            view.setX((tabSwitcher.areToolbarsShown() && toolbars != null ? Math.max(0,
                    toolbars[TabSwitcher.PRIMARY_TOOLBAR_INDEX].getWidth() - tabOffset) : 0) +
                    position);
        } else {
            view.setY((tabContainerHeight - tabHeight) + position);
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

        if (axis == Axis.DRAGGING_AXIS) {
            Toolbar[] toolbars = tabSwitcher.getToolbars();
            animator.x((tabSwitcher.areToolbarsShown() && toolbars != null ? Math.max(0,
                    toolbars[TabSwitcher.PRIMARY_TOOLBAR_INDEX].getWidth() - tabOffset) : 0) +
                    position);
        } else {
            animator.y((tabContainerHeight - tabHeight) + position);
        }
    }

    @Override
    public final int getPadding(@NonNull final Axis axis, final int gravity,
                                @NonNull final View view) {
        ensureNotNull(axis, "The axis may not be null");
        ensureTrue(gravity == Gravity.START || gravity == Gravity.END, "Invalid gravity");
        ensureNotNull(view, "The view may not be null");
        if (axis == Axis.DRAGGING_AXIS) {
            return gravity == Gravity.START ? view.getPaddingLeft() : view.getPaddingRight();
        } else {
            return gravity == Gravity.START ? view.getPaddingTop() : view.getPaddingBottom();
        }
    }

    @Override
    public final float getScale(@NonNull final View view, final boolean includePadding) {
        ensureNotNull(view, "The view may not be null");
        return 1;
    }

    @Override
    public final void setScale(@NonNull final Axis axis, @NonNull final View view,
                               final float scale) {
        ensureNotNull(axis, "The axis may not be null");
        ensureNotNull(view, "The view may not be null");

        if (axis == Axis.DRAGGING_AXIS) {
            view.setScaleX(scale);
        } else {
            view.setScaleY(scale);
        }
    }

    @Override
    public final void animateScale(@NonNull final Axis axis,
                                   @NonNull final ViewPropertyAnimator animator,
                                   final float scale) {
        ensureNotNull(axis, "The axis may not be null");
        ensureNotNull(animator, "The animator may not be null");

        if (axis == Axis.DRAGGING_AXIS) {
            animator.scaleX(scale);
        } else {
            animator.scaleY(scale);
        }
    }

    @Override
    public final float getSize(@NonNull final Axis axis, @NonNull final View view) {
        ensureNotNull(axis, "The axis may not be null");
        ensureNotNull(view, "The view may not be null");

        if (axis == Axis.DRAGGING_AXIS) {
            return view.getWidth() * getScale(view, false);
        } else {
            return view.getHeight() * getScale(view, false);
        }
    }

    @Override
    public final float getTabContainerSize(@NonNull final Axis axis) {
        return getTabContainerSize(axis, true);
    }

    @Override
    public final float getTabContainerSize(@NonNull final Axis axis, final boolean includePadding) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final float getPivot(@NonNull final Axis axis, @NonNull final View view,
                                @NonNull final DragState dragState) {
        ensureNotNull(axis, "The axis may not be null");
        ensureNotNull(view, "The view may not be null");
        ensureNotNull(dragState, "The drag state may not be null");
        return getSize(axis, view) / 2f;
    }

    @Override
    public void setPivot(@NonNull final Axis axis, @NonNull final View view, final float pivot) {
        ensureNotNull(axis, "The axis may not be null");
        ensureNotNull(view, "The view may not be null");

        if (axis == Axis.DRAGGING_AXIS) {
            view.setTranslationY(
                    view.getTranslationY() + (view.getPivotY() - pivot) * (1 - view.getScaleY()));
            view.setPivotY(pivot);
        } else {
            view.setTranslationX(
                    view.getTranslationX() + (view.getPivotX() - pivot) * (1 - view.getScaleX()));
            view.setPivotX(pivot);
        }
    }

    @Override
    public final float getRotation(@NonNull final Axis axis, @NonNull final View view) {
        ensureNotNull(axis, "The axis may not be null");
        ensureNotNull(view, "The view may not be null");

        if (axis == Axis.DRAGGING_AXIS) {
            return view.getRotationX();
        } else {
            return view.getRotationY();
        }
    }

    @Override
    public final void setRotation(@NonNull final Axis axis, @NonNull final View view,
                                  final float angle) {
        ensureNotNull(axis, "The axis may not be null");
        ensureNotNull(view, "The view may not be null");

        if (axis == Axis.DRAGGING_AXIS) {
            view.setRotationX(angle);
        } else {
            view.setRotationY(angle);
        }
    }

    @Override
    public final void animateRotation(@NonNull final Axis axis,
                                      @NonNull final ViewPropertyAnimator animator,
                                      final float angle) {
        ensureNotNull(axis, "The axis may not be null");
        ensureNotNull(animator, "The animator may not be null");

        if (axis == Axis.DRAGGING_AXIS) {
            animator.rotationX(angle);
        } else {
            animator.rotationY(angle);
        }
    }

}
