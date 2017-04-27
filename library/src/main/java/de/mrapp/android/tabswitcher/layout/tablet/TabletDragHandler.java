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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.View;

import de.mrapp.android.tabswitcher.R;
import de.mrapp.android.tabswitcher.TabSwitcher;
import de.mrapp.android.tabswitcher.iterator.AbstractTabItemIterator;
import de.mrapp.android.tabswitcher.iterator.TabItemIterator;
import de.mrapp.android.tabswitcher.layout.AbstractDragHandler;
import de.mrapp.android.tabswitcher.layout.Arithmetics;
import de.mrapp.android.tabswitcher.model.State;
import de.mrapp.android.tabswitcher.model.TabItem;
import de.mrapp.android.util.view.AttachedViewRecycler;

import static de.mrapp.android.util.Condition.ensureNotNull;

/**
 * A drag handler, which allows to calculate the position and state of tabs on touch events, when
 * using the tablet layout.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public class TabletDragHandler extends AbstractDragHandler<AbstractDragHandler.Callback> {

    /**
     * The view recycler, which allows to inflate the views, which are used to visualize the tabs,
     * whose positions and states are calculated by the drag handler.
     */
    private final AttachedViewRecycler<TabItem, ?> viewRecycler;

    /**
     * The offset between two neighboring tabs in pixels.
     */
    private final int tabOffset;

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
    public TabletDragHandler(@NonNull final TabSwitcher tabSwitcher,
                             @NonNull final Arithmetics arithmetics,
                             @NonNull final AttachedViewRecycler<TabItem, ?> viewRecycler) {
        super(tabSwitcher, arithmetics, true);
        ensureNotNull(viewRecycler, "The view recycler may not be null");
        this.viewRecycler = viewRecycler;
        this.tabOffset =
                tabSwitcher.getResources().getDimensionPixelSize(R.dimen.tablet_tab_offset);
    }

    @Override
    @Nullable
    protected final TabItem getFocusedTab(final float position) {
        AbstractTabItemIterator iterator =
                new TabItemIterator.Builder(getTabSwitcher(), viewRecycler).create();
        TabItem tabItem;

        while ((tabItem = iterator.next()) != null) {
            if (tabItem.getTag().getState() == State.FLOATING ||
                    tabItem.getTag().getState() == State.STACKED_START_ATOP ||
                    tabItem.getIndex() == 0) {
                View view = tabItem.getView();
                Toolbar[] toolbars = getTabSwitcher().getToolbars();
                float toolbarWidth = getTabSwitcher().areToolbarsShown() && toolbars != null ?
                        Math.max(0, toolbars[0].getWidth() - tabOffset) : 0;
                float viewPosition =
                        getArithmetics().getPosition(Arithmetics.Axis.DRAGGING_AXIS, view) +
                                toolbarWidth + getArithmetics()
                                .getPadding(Arithmetics.Axis.DRAGGING_AXIS, Gravity.START,
                                        getTabSwitcher());

                if (viewPosition <= position) {
                    return tabItem;
                }
            }
        }

        return null;
    }

}