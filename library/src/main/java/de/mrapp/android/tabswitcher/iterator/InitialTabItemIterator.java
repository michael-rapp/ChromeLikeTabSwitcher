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
package de.mrapp.android.tabswitcher.iterator;

import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;

import de.mrapp.android.tabswitcher.DragHandler;
import de.mrapp.android.tabswitcher.R;
import de.mrapp.android.tabswitcher.TabSwitcher;
import de.mrapp.android.tabswitcher.model.State;
import de.mrapp.android.tabswitcher.model.TabItem;
import de.mrapp.android.tabswitcher.util.AttachedViewRecycler;

import static de.mrapp.android.util.Condition.ensureEqual;
import static de.mrapp.android.util.Condition.ensureNotNull;

/**
 * An iterator, which allows to iterate the tab items, which corresponds to the tabs of a {@link
 * TabSwitcher}. When a tab item is referenced for the first time, its initial position and state is
 * calculated and the tab item is stored in a backing array. When the tab item is iterated again, it
 * is retrieved from the backing array.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public class InitialTabItemIterator extends AbstractTabItemIterator {

    /**
     * A builder, which allows to configure and create instances of the class {@link
     * InitialTabItemIterator}.
     */
    public static class Builder extends AbstractBuilder<Builder, InitialTabItemIterator> {

        /**
         * The tab switcher, whose tabs should be iterated by the iterator, which is created by the
         * builder.
         */
        private final TabSwitcher tabSwitcher;

        /**
         * The view recycler, which allows to inflate the views, which are used to visualize the
         * tabs, which are iterated by the iterator, which is created by the builder.
         */
        private final AttachedViewRecycler<TabItem, ?> viewRecycler;

        /**
         * The drag handler, which is used to calculate the initial position and state of tabs.
         */
        private final DragHandler dragHandler;

        /**
         * The backing array, which is used to store tab items, once their initial position and
         * state has been calculated.
         */
        private final TabItem[] array;

        /**
         * Creates a new builder, which allows to configure and create instances of the class {@link
         * InitialTabItemIterator}.
         *
         * @param tabSwitcher
         *         The tab switcher, whose tabs should be iterated by the iterator, which is created
         *         by the builder, as an instance of the class {@link TabSwitcher}. The tab switcher
         *         may not be null
         * @param viewRecycler
         *         The view recycler, which allows to inflate the views, which are used to visualize
         *         the tabs, which are iterated by the iterator, which is created by the builder, as
         *         an instance of the class {@link AttachedViewRecycler}. The view recycler may not
         *         be null
         * @param dragHandler
         *         The drag handler, which should be used to calculate the initial position and
         *         state of tabs, as an instance of the class {@link DragHandler}. The drag handler
         *         may not be null
         * @param array
         *         The backing array, which should be used to store tab items, once their initial
         *         position and state has been calculated, as an array of the type {@link TabItem}.
         *         The array may not be null and the array's length must be equal to the number of
         *         tabs, which are contained by the given tab switcher
         */
        public Builder(@NonNull final TabSwitcher tabSwitcher,
                       @NonNull final AttachedViewRecycler<TabItem, ?> viewRecycler,
                       @NonNull final DragHandler dragHandler, @NonNull final TabItem[] array) {
            ensureNotNull(tabSwitcher, "The tab switcher may not be null");
            ensureNotNull(viewRecycler, "The view recycler may not be null");
            ensureNotNull(dragHandler, "The drag handler may not be null");
            ensureNotNull(array, "The array may not be null");
            ensureEqual(array.length, tabSwitcher.getCount(),
                    "The array's length must be " + tabSwitcher.getCount());
            this.tabSwitcher = tabSwitcher;
            this.viewRecycler = viewRecycler;
            this.dragHandler = dragHandler;
            this.array = array;
        }

        @NonNull
        @Override
        public InitialTabItemIterator create() {
            return new InitialTabItemIterator(tabSwitcher, viewRecycler, dragHandler, array,
                    reverse, start);
        }

    }

    /**
     * A factory, which allows to create instances of the class {@link Builder}.
     */
    public static class Factory implements AbstractTabItemIterator.Factory {

        /**
         * Tha tab switcher, which is used by the builders, which are created by the factory.
         */
        private final TabSwitcher tabSwitcher;

        /**
         * The view recycler, which is used by the builders, which are created by the factory.
         */
        private final AttachedViewRecycler<TabItem, ?> viewRecycler;

        /**
         * The drag handler, which is used by the builders, which are created by the factory.
         */
        private final DragHandler dragHandler;

        /**
         * The backing array, which is used by the builders, which are created by the factory.
         */
        private final TabItem[] array;

        /**
         * Creates a new factory, which allows to create instances of the class {@link Builder}.
         *
         * @param tabSwitcher
         *         The tab swticher, which should be used by the builders, which are created by the
         *         factory, as an instance of the class {@link TabSwitcher}. The tab switcher may
         *         not be null
         * @param viewRecycler
         *         The view recycler, which should be used by the builders, which are created by the
         *         factory, as an instance of the class {@link AttachedViewRecycler}. The view
         *         recycler may not be null
         * @param dragHandler
         *         The drag handler, which should be used by the builders, which are created by the
         *         factory, as an instance of the class {@link DragHandler}. The drag handler may
         *         not be null
         * @param array
         *         The backing array, which should be used by the builders, which are created by the
         *         factory, as an array of the type {@link TabItem}. The array may not be null and
         *         the array's length must be equal to the number of tabs, which are contained by
         *         the given tab switcher
         */
        public Factory(@NonNull final TabSwitcher tabSwitcher,
                       @NonNull final AttachedViewRecycler<TabItem, ?> viewRecycler,
                       @NonNull final DragHandler dragHandler, @NonNull final TabItem[] array) {
            ensureNotNull(tabSwitcher, "The tab switcher may not be null");
            ensureNotNull(viewRecycler, "The view recycler may not be null");
            ensureNotNull(dragHandler, "The drag handler may not be null");
            ensureNotNull(array, "The array may not be null");
            ensureEqual(array.length, tabSwitcher.getCount(),
                    "The array's length must be " + tabSwitcher.getCount());
            this.tabSwitcher = tabSwitcher;
            this.viewRecycler = viewRecycler;
            this.dragHandler = dragHandler;
            this.array = array;
        }

        @NonNull
        @Override
        public AbstractBuilder<?, ?> create() {
            return new Builder(tabSwitcher, viewRecycler, dragHandler, array);
        }

    }

    /**
     * The tab switcher, whose tabs are iterated.
     */
    private final TabSwitcher tabSwitcher;

    /**
     * The view recycler, which allows to inflated the views, which are used to visualize the
     * iterated tabs.
     */
    private final AttachedViewRecycler<TabItem, ?> viewRecycler;

    /**
     * The drag handler, which is used to calculate the initial position and state of tab items.
     */
    private final DragHandler dragHandler;

    /**
     * The backing array, which is used to store tab items, once their initial position and state
     * has been calculated.
     */
    private final TabItem[] array;

    /**
     * The number of tabs, which are contained by a stack.
     */
    private final int stackedTabCount;

    /**
     * The space between tabs, which are part of a stack, in pixels.
     */
    private final float stackedTabSpacing;

    /**
     * Calculates the initial position and state of a specific tab item.
     *
     * @param tabItem
     *         The tab item, whose position and state should be calculated, as an instance of the
     *         class {@link TabItem}. The tab item may not be null
     * @param predecessor
     *         The predecessor of the given tab item as an instance of the class {@link TabItem} or
     *         null, if the tab item does not have a predecessor
     */
    private void calculateAndClipStartPosition(@NonNull final TabItem tabItem,
                                               @Nullable final TabItem predecessor) {
        float position = calculateStartPosition(tabItem);
        Pair<Float, State> pair =
                dragHandler.clipTabPosition(position, tabItem, predecessor, tabSwitcher.getCount());
        tabItem.getTag().setPosition(pair.first);
        tabItem.getTag().setState(pair.second);
    }

    /**
     * Calculates and returns the initial position of a specific tab item.
     *
     * @param tabItem
     *         The tab item, whose position should be calculated, as an instance of the class {@link
     *         TabItem}. The tab item may not be null
     * @return The position, which has been calculated, as a {@link Float} value
     */
    private float calculateStartPosition(@NonNull final TabItem tabItem) {
        if (tabItem.getIndex() == 0) {
            return getCount() > stackedTabCount ? stackedTabCount * stackedTabSpacing :
                    (getCount() - 1) * stackedTabSpacing;

        } else {
            return -1;
        }
    }

    /**
     * Creates a new iterator, which allows to iterate the tab items, which corresponds to the tabs
     * of a {@link TabSwitcher}.
     *
     * @param tabSwitcher
     *         The tab switcher, whose tabs should be iterated, as an instance of the class {@link
     *         TabSwitcher}. The tab switcher may not be null
     * @param viewRecycler
     *         The view recycler, which allows to inflate the views, which are used to visualize the
     *         iterated tabs, as an instance of the class {@link AttachedViewRecycler}. The view
     *         recycler may not be null
     * @param dragHandler
     *         The drag handler, which should be used to calculate the initial position and state of
     *         tab items, as an instance of the class {@link TabItem}. The tab item may not be null
     * @param array
     *         The backing array, which should be used to store tab items, once their initial
     *         position and state has been calculated, as an array of the type {@link TabItem}. The
     *         array may not be null and the array's length must be equal to the number of tabs,
     *         which are contained by the given tab switcher
     * @param reverse
     *         True, if the tabs should be iterated in reverse order, false otherwise
     * @param start
     *         The index of the first tab, which should be iterated, as an {@link Integer} value or
     *         -1, if all tabs should be iterated
     */
    private InitialTabItemIterator(@NonNull final TabSwitcher tabSwitcher,
                                   @NonNull final AttachedViewRecycler<TabItem, ?> viewRecycler,
                                   @NonNull final DragHandler dragHandler,
                                   @NonNull final TabItem[] array, final boolean reverse,
                                   final int start) {
        ensureNotNull(tabSwitcher, "The tab switcher may not be null");
        ensureNotNull(viewRecycler, "The view recycler may not be null");
        ensureNotNull(dragHandler, "The drag handler may not be null");
        ensureNotNull(array, "The array may not be null");
        ensureEqual(array.length, tabSwitcher.getCount(),
                "The array's length must be " + tabSwitcher.getCount());
        this.tabSwitcher = tabSwitcher;
        this.viewRecycler = viewRecycler;
        this.dragHandler = dragHandler;
        this.array = array;
        Resources resources = tabSwitcher.getResources();
        this.stackedTabCount = resources.getInteger(R.integer.stacked_tab_count);
        this.stackedTabSpacing = resources.getDimensionPixelSize(R.dimen.stacked_tab_spacing);
        initialize(reverse, start);
    }

    @Override
    public final int getCount() {
        return array.length;
    }

    @NonNull
    @Override
    public final TabItem getItem(final int index) {
        TabItem tabItem = array[index];

        if (tabItem == null) {
            tabItem = TabItem.create(tabSwitcher, viewRecycler, index);
            calculateAndClipStartPosition(tabItem, index > 0 ? getItem(index - 1) : null);
            array[index] = tabItem;
        }

        return tabItem;
    }

}