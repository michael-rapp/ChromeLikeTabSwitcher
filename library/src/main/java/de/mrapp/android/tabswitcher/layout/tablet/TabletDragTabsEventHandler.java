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
import android.graphics.RectF;
import android.view.Gravity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import de.mrapp.android.tabswitcher.R;
import de.mrapp.android.tabswitcher.TabSwitcher;
import de.mrapp.android.tabswitcher.iterator.AbstractItemIterator;
import de.mrapp.android.tabswitcher.iterator.ItemIterator;
import de.mrapp.android.tabswitcher.layout.AbstractDragTabsEventHandler;
import de.mrapp.android.tabswitcher.layout.Arithmetics;
import de.mrapp.android.tabswitcher.layout.Arithmetics.Axis;
import de.mrapp.android.tabswitcher.model.AbstractItem;
import de.mrapp.android.tabswitcher.model.AddTabItem;
import de.mrapp.android.tabswitcher.model.State;
import de.mrapp.android.util.view.AttachedViewRecycler;
import de.mrapp.util.Condition;

/**
 * A drag handler, which allows to calculate the position and state of tabs on touch events, when
 * using the tablet layout.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public class TabletDragTabsEventHandler
        extends AbstractDragTabsEventHandler<AbstractDragTabsEventHandler.Callback> {

    /**
     * The view recycler, which allows to inflate the views, which are used to visualize the tabs,
     * whose positions and states are calculated by the drag handler.
     */
    private final AttachedViewRecycler<AbstractItem, ?> viewRecycler;

    /**
     * The offset between two neighboring tabs in pixels.
     */
    private final int tabOffset;

    /**
     * The height of the view group, which contains the tab switcher's tabs.
     */
    private final int tabContainerHeight;

    /**
     * Calculates and returns the position of a specific item, in relation to
     * the position of the tab switcher.
     *
     * @param item
     *         The item, whose position should be returned, as an instance of the class {@link
     *         AbstractItem}. The item may not be null
     * @return The position, which has been calculated, as a {@link Float} value
     */
    private float getViewPosition(@NonNull final AbstractItem item) {
        Toolbar[] toolbars = getTabSwitcher().getToolbars();
        float toolbarWidth = getTabSwitcher().areToolbarsShown() && toolbars != null ?
                Math.max(0, toolbars[TabSwitcher.PRIMARY_TOOLBAR_INDEX].getWidth() - tabOffset) : 0;
        return getArithmetics().getPosition(Axis.DRAGGING_AXIS, item) + toolbarWidth +
                getArithmetics().getTabSwitcherPadding(Axis.DRAGGING_AXIS, Gravity.START);
    }

    /**
     * Returns, whether a specific item is focusable, or not.
     *
     * @param item
     *         The item, which should be checked, as an instance of the class {@link AbstractItem}.
     *         The item may not be null
     * @param selectedItemIndex
     *         The index of the currently selected item as an {@link Integer} value
     * @param successor
     *         The successor of the given item as an instance of the class {@link AbstractItem} or
     *         null, if the item does not have a successor
     * @return True, if the given item is focusable, false otherwise
     */
    private boolean isItemFocusable(@NonNull final AbstractItem item, final int selectedItemIndex,
                                    @Nullable final AbstractItem successor) {
        return item instanceof AddTabItem || item.getIndex() == selectedItemIndex ||
                item.getTag().getState() == State.FLOATING ||
                item.getTag().getState() == State.STACKED_START_ATOP ||
                (item.getTag().getState() == State.STACKED_END &&
                        (successor == null || successor.getTag().getState() == State.FLOATING));
    }

    /**
     * Creates a new drag handler, which allows to calculate the position and state of tabs on touch
     * events, when using the tablet layout.
     *
     * @param tabSwitcher
     *         The tab switcher, whose tabs' positions and states should be calculated by the drag
     *         handler, as an instance of the class {@link TabSwitcher}. The tab switcher may not be
     *         null
     * @param arithmetics
     *         The arithmetics, which should be used to calculate the position, size and rotation of
     *         tabs, as an instance of the type {@link Arithmetics}. The arithmetics may not be
     *         null
     * @param viewRecycler
     *         The view recycler, which allows to inflate the views, which are used to visualize the
     *         tabs, whose positions and states should be calculated by the tab switcher, as an
     *         instance of the class AttachedViewRecycler. The view recycler may not be null
     */
    public TabletDragTabsEventHandler(@NonNull final TabSwitcher tabSwitcher,
                                      @NonNull final Arithmetics arithmetics,
                                      @NonNull final AttachedViewRecycler<AbstractItem, ?> viewRecycler) {
        super(tabSwitcher, arithmetics, true);
        Condition.INSTANCE.ensureNotNull(viewRecycler, "The view recycler may not be null");
        this.viewRecycler = viewRecycler;
        Resources resources = tabSwitcher.getResources();
        this.tabOffset = resources.getDimensionPixelSize(R.dimen.tablet_tab_offset);
        this.tabContainerHeight =
                resources.getDimensionPixelSize(R.dimen.tablet_tab_container_height);
    }

    @NonNull
    @Override
    public final RectF getTouchableArea() {
        float left = getTabSwitcher().getPaddingLeft();
        float top = getTabSwitcher().getPaddingTop();
        int right = getTabSwitcher().getPaddingRight();
        return new RectF(left, top, getTabSwitcher().getWidth() - right, top + tabContainerHeight);
    }

    @Override
    @Nullable
    protected final AbstractItem getFocusedItem(final float position) {
        int selectedItemIndex = getTabSwitcher().getSelectedTabIndex() +
                (getTabSwitcher().isAddTabButtonShown() ? 1 : 0);
        ItemIterator.Builder builder = new ItemIterator.Builder(getTabSwitcher(), viewRecycler);
        AbstractItemIterator iterator = builder.start(selectedItemIndex).create();
        AbstractItem item;

        while ((item = iterator.next()) != null) {
            if (isItemFocusable(item, selectedItemIndex, iterator.peek())) {
                float viewPosition = getViewPosition(item);
                float viewWidth = getArithmetics().getSize(Axis.DRAGGING_AXIS, item);

                if (position > viewPosition + viewWidth) {
                    break;
                } else if (position >= viewPosition) {
                    return item;
                }
            }
        }

        if (selectedItemIndex - 1 >= 0) {
            iterator = builder.start(selectedItemIndex - 1).reverse(true).create();

            while ((item = iterator.next()) != null) {
                if (isItemFocusable(item, selectedItemIndex, iterator.previous())) {
                    float viewPosition = getViewPosition(item);
                    float viewWidth = getArithmetics().getSize(Axis.DRAGGING_AXIS, item);

                    if (position >= viewPosition && position <= viewPosition + viewWidth) {
                        return item;
                    }
                }
            }
        }

        return null;
    }

}