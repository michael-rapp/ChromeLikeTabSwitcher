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

import static de.mrapp.android.util.Condition.ensureNotNull;

/**
 * An iterator, which allows to iterate the tab items, which are contained by an array.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public class ArrayIterator extends AbstractIterator {

    /**
     * A builder, which allows to configure and create instances of the class {@link
     * ArrayIterator}.
     */
    public static class Builder extends AbstractBuilder<Builder, ArrayIterator> {

        /**
         * The array, whose items should be iterated by the iterator.
         */
        private final TabItem[] array;

        /**
         * Creates a new builder, which allows to configure and create instances of the class {@link
         * ArrayIterator}.
         *
         * @param array
         *         The array, whose items should be iterated by the iterator, as an array of the
         *         type {@link TabItem}. The array may not be null
         */
        public Builder(@NonNull final TabItem[] array) {
            ensureNotNull(array, "The array may not be null");
            this.array = array;
        }

        @NonNull
        @Override
        public ArrayIterator create() {
            return new ArrayIterator(array, reverse, start);
        }

    }

    /**
     * A factory, which allows to create instances of the class {@link Builder}.
     */
    public static class Factory implements AbstractIterator.Factory {

        /**
         * The array, which is used by the builders, which are created by the factory.
         */
        private final TabItem[] array;

        /**
         * Creates a new factory, which allows to create instances of the class {@link Builder}.
         *
         * @param array
         *         The array, which should be used by the builders, which are created by the
         *         factory, as an array of the type {@link TabItem}. The array may not be null
         */
        public Factory(@NonNull final TabItem[] array) {
            ensureNotNull(array, "The array may not be null");
            this.array = array;
        }

        @NonNull
        @Override
        public AbstractBuilder<?, ?> create() {
            return new Builder(array);
        }

    }

    /**
     * The array, whose items are iterated.
     */
    private final TabItem[] array;

    /**
     * Creates a new iterator, which allows to iterate the tab items, which are contained by an
     * array.
     *
     * @param array
     *         The array, whose items should be iterated, as an array of the type {@link TabItem}.
     *         The array may not be null
     * @param reverse
     *         True, if the tabs should be iterated in reverse order, false otherwise
     * @param start
     *         The index of the first tab, which should be iterated, as an {@link Integer} value or
     *         -1, if all tabs should be iterated
     */
    private ArrayIterator(@NonNull final TabItem[] array, final boolean reverse, final int start) {
        ensureNotNull(array, "The array may not be null");
        this.array = array;
        initialize(reverse, start);
    }

    @Override
    public final int getCount() {
        return array.length;
    }

    @NonNull
    @Override
    public final TabItem getItem(final int index) {
        return array[index];
    }

}