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
import android.support.annotation.Nullable;

import de.mrapp.android.tabswitcher.TabSwitcher;
import de.mrapp.android.tabswitcher.util.ViewRecycler;

import static de.mrapp.android.util.Condition.ensureAtLeast;
import static de.mrapp.android.util.Condition.ensureNotNull;

/**
 * An iterator, which allows to iterate the tab items, which correspond to the tabs of a {@link
 * TabSwitcher}.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public class Iterator implements java.util.Iterator<TabItem> {

    /**
     * A builder, which allows to configure and create instances of the class {@link Iterator}.
     */
    public static class Builder {

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
         * True, if the tabs should be iterated in reverse order, false otherwise.
         */
        private boolean reverse;

        /**
         * The index of the first tab, which should be iterated.
         */
        private int start;

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
            reverse(false);
            start(-1);
        }

        /**
         * Sets, whether the tabs should be iterated in reverse order, or not.
         *
         * @param reverse
         *         True, if the tabs should be iterated in reverse order, false otherwise
         * @return The builder, this method has been called upon, as an instance of the class {@link
         * Builder}. The builder may not be null
         */
        @NonNull
        public Builder reverse(final boolean reverse) {
            this.reverse = reverse;
            return this;
        }

        /**
         * Sets the index of the first tab, which should be iterated.
         *
         * @param start
         *         The index, which should be set, as an {@link Integer} value or -1, if all tabs
         *         should be iterated Builder}. The builder may not be null
         */
        @NonNull
        public Builder start(final int start) {
            ensureAtLeast(start, -1, "The start must be at least -1");
            this.start = start;
            return this;
        }

        /**
         * Creates the iterator, which has been configured by using the builder.
         *
         * @return The iterator, which has been created, as an instance of the class {@link
         * Iterator}. The iterator may not be null
         */
        @NonNull
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
     * True, if the tabs should be iterated in reverse order, false otherwise.
     */
    private final boolean reverse;

    /**
     * The index of the next tab.
     */
    private int index;

    /**
     * The current tab item.
     */
    private TabItem current;

    /**
     * The previous tab item.
     */
    private TabItem previous;

    /**
     * The first tab item.
     */
    private TabItem first;

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
    private Iterator(final TabSwitcher tabSwitcher, final ViewRecycler<TabItem, ?> viewRecycler,
                     final boolean reverse, final int start) {
        ensureNotNull(tabSwitcher, "The tab switcher may not be null");
        ensureNotNull(viewRecycler, "The view recycler may not be null");
        ensureAtLeast(start, -1, "The start must be at least -1");
        this.tabSwitcher = tabSwitcher;
        this.viewRecycler = viewRecycler;
        this.reverse = reverse;
        this.previous = null;
        this.index = start != -1 ? start : (reverse ? tabSwitcher.getCount() - 1 : 0);
        int previousIndex = reverse ? this.index + 1 : this.index - 1;

        if (previousIndex >= 0 && previousIndex < tabSwitcher.getCount()) {
            this.current = TabItem.create(tabSwitcher, viewRecycler, previousIndex);
        } else {
            this.current = null;
        }
    }

    /**
     * Returns the tab item, which corresponds to the first tab.
     *
     * @return The tab item, which corresponds to the first tab, as an instance of the class {@link
     * TabItem}. The tab item may not be null
     */
    @NonNull
    public final TabItem first() {
        return first;
    }

    /**
     * Returns the tab item, which corresponds to the previous tab.
     *
     * @return The tab item, which corresponds to the previous tab, as an instance of the class
     * {@link TabItem}. The tab item may not be null
     */
    @Nullable
    public final TabItem previous() {
        return previous;
    }

    @Override
    public final boolean hasNext() {
        if (reverse) {
            return index >= 0;
        } else {
            return tabSwitcher.getCount() - index >= 1;
        }
    }

    @Override
    public final TabItem next() {
        if (hasNext()) {
            previous = current;

            if (first == null) {
                first = current;
            }

            current = TabItem.create(tabSwitcher, viewRecycler, index);
            index += reverse ? -1 : 1;
            return current;
        }

        return null;
    }

}