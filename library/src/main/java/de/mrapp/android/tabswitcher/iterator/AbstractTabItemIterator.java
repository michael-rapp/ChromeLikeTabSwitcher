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

import android.support.annotation.NonNull;

import de.mrapp.android.tabswitcher.model.TabItem;

import static de.mrapp.android.util.Condition.ensureAtLeast;

/**
 * An abstract base class for all iterators, which allow to iterate items of the type {@link
 * TabItem}.
 *
 * @author Michael Rapp
 * @since 0.1.0
 */
public abstract class AbstractTabItemIterator implements java.util.Iterator<TabItem> {

    /**
     * An abstract base class of all builders, which allows to configure and create instances of the
     * class {@link AbstractTabItemIterator}.
     */
    public static abstract class AbstractBuilder<BuilderType extends AbstractBuilder<?, ProductType>, ProductType extends AbstractTabItemIterator> {

        /**
         * True, if the tabs should be iterated in reverse order, false otherwise.
         */
        protected boolean reverse;

        /**
         * The index of the first tab, which should be iterated.
         */
        protected int start;

        /**
         * Returns a reference to the builder itself. It is implicitly cast to the generic type
         * BuilderType.
         *
         * @return The builder as an instance of the generic type BuilderType
         */
        @SuppressWarnings("unchecked")
        private BuilderType self() {
            return (BuilderType) this;
        }

        /**
         * Creates a new builder, which allows to configure and create instances of the class {@link
         * AbstractTabItemIterator}.
         */
        protected AbstractBuilder() {
            reverse(false);
            start(-1);
        }

        /**
         * Creates the iterator, which has been configured by using the builder.
         *
         * @return The iterator, which has been created, as an instance of the class {@link
         * TabItemIterator}. The iterator may not be null
         */
        @NonNull
        public abstract ProductType create();

        /**
         * Sets, whether the tabs should be iterated in reverse order, or not.
         *
         * @param reverse
         *         True, if the tabs should be iterated in reverse order, false otherwise
         * @return The builder, this method has been called upon, as an instance of the generic type
         * BuilderType. The builder may not be null
         */
        @NonNull
        public BuilderType reverse(final boolean reverse) {
            this.reverse = reverse;
            return self();
        }

        /**
         * Sets the index of the first tab, which should be iterated.
         *
         * @param start
         *         The index, which should be set, as an {@link Integer} value or -1, if all tabs
         *         should be iterated Builder}. The builder may not be null
         * @return The builder, this method has been called upon, as an instance of the generic type
         * BuilderType. The builder may not be null
         */
        @NonNull
        public BuilderType start(final int start) {
            ensureAtLeast(start, -1, "The start must be at least -1");
            this.start = start;
            return self();
        }

    }

    /**
     * True, if the tabs should be iterated in reverse order, false otherwise.
     */
    private boolean reverse;

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
     * The method, which is invoked on subclasses in order to retrieve the total number of available
     * items.
     *
     * @return The total number of available items as an {@link Integer} value
     */
    public abstract int getCount();

    /**
     * The method, which is invoked on subclasses in order to retrieve the item, which corresponds
     * to a specific index.
     *
     * @param index
     *         The index of the item, which should be returned, as an {@link Integer} value
     * @return The item, which corresponds to the given index, as an instance of the class {@link
     * TabItem}. The tab item may not be null
     */
    @NonNull
    public abstract TabItem getItem(final int index);

    /**
     * Initializes the iterator.
     *
     * @param reverse
     *         True, if the tabs should be iterated in reverse order, false otherwise
     * @param start
     *         The index of the first tab, which should be iterated, as an {@link Integer} value or
     *         -1, if all tabs should be iterated
     */
    protected final void initialize(final boolean reverse, final int start) {
        ensureAtLeast(start, -1, "The start must be at least -1");
        this.reverse = reverse;
        this.previous = null;
        this.index = start != -1 ? start : (reverse ? getCount() - 1 : 0);
        int previousIndex = reverse ? this.index + 1 : this.index - 1;

        if (previousIndex >= 0 && previousIndex < getCount()) {
            this.current = getItem(previousIndex);
        } else {
            this.current = null;
        }
    }

    /**
     * Returns the tab item, which corresponds to the first tab.
     *
     * @return The tab item, which corresponds to the first tab, as an instance of the class {@link
     * TabItem} or null, if no tabs are available
     */
    public final TabItem first() {
        return first;
    }

    /**
     * Returns the tab item, which corresponds to the previous tab.
     *
     * @return The tab item, which corresponds to the previous tab, as an instance of the class
     * {@link TabItem} or null, if no previous tab is available
     */
    public final TabItem previous() {
        return previous;
    }

    /**
     * Returns the tab item, which corresponds to the next tab.
     *
     * @return The tab item, which corresponds to the next tab, as an instance of the class {@link
     * TabItem} or null, if no next tab is available
     */
    public final TabItem peek() {
        return index >= 0 && index < getCount() ? getItem(index) : null;
    }

    @Override
    public final boolean hasNext() {
        if (reverse) {
            return index >= 0;
        } else {
            return getCount() - index >= 1;
        }
    }

    @Override
    public final TabItem next() {
        if (hasNext()) {
            previous = current;

            if (first == null) {
                first = current;
            }

            current = getItem(index);
            index += reverse ? -1 : 1;
            return current;
        }

        return null;
    }

}
