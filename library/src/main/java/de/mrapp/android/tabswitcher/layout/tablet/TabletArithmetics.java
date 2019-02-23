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
package de.mrapp.android.tabswitcher.layout.tablet;

import android.content.res.Resources;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import de.mrapp.android.tabswitcher.R;
import de.mrapp.android.tabswitcher.TabSwitcher;
import de.mrapp.android.tabswitcher.layout.AbstractArithmetics;
import de.mrapp.android.tabswitcher.layout.AbstractDragTabsEventHandler.DragState;
import de.mrapp.android.tabswitcher.model.AbstractItem;
import de.mrapp.util.Condition;

/**
 * Provides methods, which allow to calculate the position, size and rotation of a {@link
 * TabSwitcher}'s tabs, when using the tablet layout.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public class TabletArithmetics extends AbstractArithmetics {

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
     * The offset between a button, which allows to add a new tab, and a neighboring tab.
     */
    private final int addTabButtonOffset;

    /**
     * Creates a new class, which provides methods, which allow to calculate the position, size and
     * rotation of a {@link TabSwitcher}'s tabs, when using the tablet layout.
     *
     * @param tabSwitcher
     *         The tab switcher, the arithmetics should be calculated for, as an instance of the
     *         class {@link TabSwitcher}. The tab switcher may not be null
     */
    public TabletArithmetics(@NonNull final TabSwitcher tabSwitcher) {
        super(tabSwitcher);
        Resources resources = tabSwitcher.getResources();
        tabHeight = resources.getDimensionPixelSize(R.dimen.tablet_tab_height);
        tabContainerHeight = resources.getDimensionPixelSize(R.dimen.tablet_tab_container_height);
        tabOffset = resources.getDimensionPixelSize(R.dimen.tablet_tab_offset);
        addTabButtonOffset = resources.getDimensionPixelSize(R.dimen.tablet_add_tab_button_offset);
    }

    @Override
    public final int getTabSwitcherPadding(@NonNull final Axis axis, final int gravity) {
        Condition.INSTANCE.ensureNotNull(axis, "The axis may not be null");
        Condition.INSTANCE
                .ensureTrue(gravity == Gravity.START || gravity == Gravity.END, "Invalid gravity");
        if (axis == Axis.DRAGGING_AXIS) {
            return gravity == Gravity.START ? getTabSwitcher().getPaddingLeft() :
                    getTabSwitcher().getPaddingRight();
        } else {
            return gravity == Gravity.START ? getTabSwitcher().getPaddingTop() :
                    getTabSwitcher().getPaddingBottom();
        }
    }

    @Override
    public final float getTabContainerSize(@NonNull final Axis axis, final boolean includePadding) {
        Condition.INSTANCE.ensureNotNull(axis, "The axis may not be null");

        if (axis == Axis.DRAGGING_AXIS) {
            ViewGroup tabContainer = getTabSwitcher().getTabContainer();
            assert tabContainer != null;
            float size = tabContainer.getWidth();
            int padding = !includePadding ?
                    getTabSwitcher().getPaddingRight() + getTabSwitcher().getPaddingLeft() : 0;
            Toolbar[] toolbars = getTabSwitcher().getToolbars();
            float primaryToolbarSize =
                    !includePadding && getTabSwitcher().areToolbarsShown() && toolbars != null ?
                            Math.max(0, toolbars[TabSwitcher.PRIMARY_TOOLBAR_INDEX].getWidth() -
                                    tabOffset) : 0;
            float secondaryToolbarSize =
                    !includePadding && getTabSwitcher().areToolbarsShown() && toolbars != null ?
                            Math.max(0, toolbars[TabSwitcher.SECONDARY_TOOLBAR_INDEX].getWidth() -
                                    (getTabSwitcher().isAddTabButtonShown() ? addTabButtonOffset :
                                            0)) : 0;
            return size - padding - primaryToolbarSize - secondaryToolbarSize;
        } else {
            return tabContainerHeight;
        }
    }

    @Override
    public final float getTouchPosition(@NonNull final Axis axis,
                                        @NonNull final MotionEvent event) {
        Condition.INSTANCE.ensureNotNull(axis, "The axis may not be null");
        Condition.INSTANCE.ensureNotNull(event, "The motion event may not be null");

        if (axis == Axis.DRAGGING_AXIS) {
            return event.getX();
        } else {
            return event.getY();
        }
    }

    @Override
    public final float getPosition(@NonNull final Axis axis, @NonNull final AbstractItem item) {
        Condition.INSTANCE.ensureNotNull(axis, "The axis may not be null");
        Condition.INSTANCE.ensureNotNull(item, "The view may not be null");
        View view = item.getView();

        if (axis == Axis.DRAGGING_AXIS) {
            Toolbar[] toolbars = getTabSwitcher().getToolbars();
            return view.getX() - (getTabSwitcher().areToolbarsShown() && toolbars != null ?
                    Math.max(0,
                            toolbars[TabSwitcher.PRIMARY_TOOLBAR_INDEX].getWidth() - tabOffset) :
                    0);
        } else {
            return view.getY() - (tabContainerHeight - tabHeight);
        }
    }

    @Override
    public final void setPosition(@NonNull final Axis axis, @NonNull final AbstractItem item,
                                  final float position) {
        Condition.INSTANCE.ensureNotNull(axis, "The axis may not be null");
        Condition.INSTANCE.ensureNotNull(item, "The item may not be null");
        View view = item.getView();

        if (axis == Axis.DRAGGING_AXIS) {
            Toolbar[] toolbars = getTabSwitcher().getToolbars();
            view.setX((getTabSwitcher().areToolbarsShown() && toolbars != null ? Math.max(0,
                    toolbars[TabSwitcher.PRIMARY_TOOLBAR_INDEX].getWidth() - tabOffset) : 0) +
                    position);
        } else {
            view.setY((tabContainerHeight - tabHeight) + position);
        }
    }

    @Override
    public final void animatePosition(@NonNull final Axis axis,
                                      @NonNull final ViewPropertyAnimator animator,
                                      @NonNull final AbstractItem item, final float position,
                                      final boolean includePadding) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final float getScale(@NonNull final AbstractItem item, final boolean includePadding) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void setScale(@NonNull final Axis axis, @NonNull final AbstractItem item,
                               final float scale) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void animateScale(@NonNull final Axis axis,
                                   @NonNull final ViewPropertyAnimator animator,
                                   final float scale) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final float getSize(@NonNull final Axis axis, @NonNull final AbstractItem item) {
        View view = item.getView();

        if (axis == Axis.DRAGGING_AXIS) {
            return view.getWidth();
        } else {
            return view.getHeight();
        }
    }

    @Override
    public final float getPivot(@NonNull final Axis axis, @NonNull final AbstractItem item,
                                @NonNull final DragState dragState) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setPivot(@NonNull final Axis axis, @NonNull final AbstractItem item,
                         final float pivot) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final float getRotation(@NonNull final Axis axis, @NonNull final AbstractItem item) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void setRotation(@NonNull final Axis axis, @NonNull final AbstractItem item,
                                  final float angle) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void animateRotation(@NonNull final Axis axis,
                                      @NonNull final ViewPropertyAnimator animator,
                                      final float angle) {
        throw new UnsupportedOperationException();
    }

}
