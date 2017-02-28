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

import java.util.List;

import de.mrapp.android.tabswitcher.model.TabItem;

import static de.mrapp.android.util.Condition.ensureNotNull;

/**
 * An iterator, which allows to iterate the tab items, which are contained by a {@link List}.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public class ListIterator extends AbstractIterator {

    /**
     * A builder, which allows to configure and create instances of the class {@link ListIterator}.
     */
    public static class Builder extends AbstractBuilder<Builder, ListIterator> {

        /**
         * The list, whose items should be iterated by the iterator.
         */
        private final List<TabItem> list;

        /**
         * Creates a new builder, which allows to configure and create instances of the class {@link
         * ListIterator}.
         *
         * @param list
         *         The tab switcher, whose items should be iterated by the iterator, as an instance
         *         of the type {@link List}. The list may not be null
         */
        public Builder(@NonNull final List<TabItem> list) {
            ensureNotNull(list, "The list may not be null");
            this.list = list;
        }

        @NonNull
        @Override
        public ListIterator create() {
            return new ListIterator(list, reverse, start);
        }

    }

    /**
     * The list, whose items are iterated.
     */
    private final List<TabItem> list;

    /**
     * Creates a new iterator, which allows to iterate the tab items, which are contained by a
     * {@link List}.
     *
     * @param list
     *         The list, whose items should be iterated, as an instance of the type {@link List}.
     *         The list may not be null
     * @param reverse
     *         True, if the tabs should be iterated in reverse order, false otherwise
     * @param start
     *         The index of the first tab, which should be iterated, as an {@link Integer} value or
     *         -1, if all tabs should be iterated
     */
    private ListIterator(@NonNull final List<TabItem> list, final boolean reverse,
                         final int start) {
        ensureNotNull(list, "The list may not be null");
        this.list = list;
        initialize(reverse, start);
    }

    @Override
    protected final int getCount() {
        return list.size();
    }

    @NonNull
    @Override
    protected final TabItem getItem(final int index) {
        return list.get(index);
    }

}