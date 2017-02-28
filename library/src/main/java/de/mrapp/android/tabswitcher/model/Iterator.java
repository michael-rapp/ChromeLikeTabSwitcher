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
package de.mrapp.android.tabswitcher.model;

import android.support.annotation.NonNull;

import de.mrapp.android.tabswitcher.TabSwitcher;
import de.mrapp.android.tabswitcher.util.ViewRecycler;

import static de.mrapp.android.util.Condition.ensureNotNull;

/**
 * An iterator, which allows to iterate the tab items, which correspond to the tabs of a {@link
 * TabSwitcher}.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public class Iterator extends AbstractIterator {

    /**
     * A builder, which allows to configure and create instances of the class {@link Iterator}.
     */
    public static class Builder extends AbstractBuilder<Builder, Iterator> {

        /**
         * The tab switcher, whose tabs should be iterated by the iterator.
         */
        private final TabSwitcher tabSwitcher;

        /**
         * The view recycler, which allows to inflated the views, which are used to visualize the
         * iterated tabs.
         */
        private final ViewRecycler<TabItem, ?> viewRecycler;

        /**
         * Creates a new builder, which allows to configure and create instances of the class {@link
         * Iterator}.
         *
         * @param tabSwitcher
         *         The tab switcher, whose tabs should be iterated by the iterator, as an instance
         *         of the class {@link TabSwitcher}. The tab switcher may not be null
         * @param viewRecycler
         *         The view recycler, which allows to inflate the views, which are used to visualize
         *         the iterated tabs, as an instance of the class {@link ViewRecycler}. The view
         *         recycler may not be null
         */
        public Builder(@NonNull final TabSwitcher tabSwitcher,
                       @NonNull final ViewRecycler<TabItem, ?> viewRecycler) {
            ensureNotNull(tabSwitcher, "The tab switcher may not be null");
            ensureNotNull(viewRecycler, "The view recycler may not be null");
            this.tabSwitcher = tabSwitcher;
            this.viewRecycler = viewRecycler;
        }

        @NonNull
        @Override
        public Iterator create() {
            return new Iterator(tabSwitcher, viewRecycler, reverse, start);
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
    private final ViewRecycler<TabItem, ?> viewRecycler;

    /**
     * Creates a new iterator, which allows to iterate the tab items, which correspond to the tabs
     * of a {@link TabSwitcher}.
     *
     * @param tabSwitcher
     *         The tab switcher, whose tabs should be iterated, as an instance of the class {@link
     *         TabSwitcher}. The tab switcher may not be null
     * @param viewRecycler
     *         The view recycler, which allows to inflate the views, which are used to visualize the
     *         iterated tabs, as an instance of the class {@link ViewRecycler}. The view recycler
     *         may not be null
     * @param reverse
     *         True, if the tabs should be iterated in reverse order, false otherwise
     * @param start
     *         The index of the first tab, which should be iterated, as an {@link Integer} value or
     *         -1, if all tabs should be iterated
     */
    private Iterator(@NonNull final TabSwitcher tabSwitcher,
                     @NonNull final ViewRecycler<TabItem, ?> viewRecycler, final boolean reverse,
                     final int start) {
        ensureNotNull(tabSwitcher, "The tab switcher may not be null");
        ensureNotNull(viewRecycler, "The view recycler may not be null");
        this.tabSwitcher = tabSwitcher;
        this.viewRecycler = viewRecycler;
        initialize(reverse, start);
    }

    @Override
    protected final int getCount() {
        return tabSwitcher.getCount();
    }

    @NonNull
    @Override
    protected final TabItem getItem(final int index) {
        return TabItem.create(tabSwitcher, viewRecycler, index);
    }

}