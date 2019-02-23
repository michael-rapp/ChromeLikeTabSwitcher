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
package de.mrapp.android.tabswitcher.iterator;

import androidx.annotation.NonNull;
import de.mrapp.android.tabswitcher.Tab;
import de.mrapp.android.tabswitcher.model.AbstractItem;
import de.mrapp.android.tabswitcher.model.Model;
import de.mrapp.android.tabswitcher.model.TabItem;
import de.mrapp.android.util.view.AttachedViewRecycler;
import de.mrapp.util.Condition;

/**
 * An iterator, which allows to iterate the items, which correspond to the tabs, which are contained
 * by an array.
 *
 * @author Michael Rapp
 * @since 0.1.0
 */
public class ArrayItemIterator extends AbstractItemIterator {

    /**
     * A builder, which allows to configure an create instances of the class {@link
     * ArrayItemIterator}.
     */
    public static class Builder extends AbstractBuilder<Builder, ArrayItemIterator> {

        /**
         * The model, which belongs to the tab switcher, whose items should be iterated by the
         * iterator, which is created by the builder.
         */
        private final Model model;

        /**
         * The view recycler, which allows to inflate the views, which are used to visualize the
         * tabs, which are iterated by the iterator, which is created by the builder.
         */
        private final AttachedViewRecycler<AbstractItem, ?> viewRecycler;

        /**
         * The array, which contains the tabs, which are iterated by the iterator, which is created
         * by the builder.
         */
        private final Tab[] array;

        /**
         * The index of the first tab, which is iterated by the iterator, which is created by the
         * builder.
         */
        private final int firstIndex;

        /**
         * Creates a new builder, which allows to configure and create instances of the class {@link
         * ArrayItemIterator}.
         *
         * @param model
         *         The model, which belongs to the tab switcher, whose items should be iterated by
         *         the iterator, which is created by the builder, as an instance of the type {@link
         *         Model}. The model may not be null
         * @param viewRecycler
         *         The view recycler, which allows to inflate the views, which are used to visualize
         *         the tabs, which should be iterated by the iterator, as an instance of the class
         *         AttachedViewRecycler. The view recycler may not be null
         * @param array
         *         The array, which contains the tabs, which should be iterated by the iterator, as
         *         an array of the type {@link Tab}. The array may not be null
         * @param firstIndex
         *         The index of the first tab, which should be iterated by the iterator, as an
         *         {@link Integer} value. The index must be at least 0
         */
        public Builder(@NonNull final Model model,
                       @NonNull final AttachedViewRecycler<AbstractItem, ?> viewRecycler,
                       @NonNull final Tab[] array, final int firstIndex) {
            Condition.INSTANCE.ensureNotNull(model, "The model may not be null");
            Condition.INSTANCE.ensureNotNull(viewRecycler, "The view recycler may not be null");
            Condition.INSTANCE.ensureNotNull(array, "The array may not be null");
            Condition.INSTANCE.ensureAtLeast(firstIndex, 0, "The first index must be at least 0");
            this.model = model;
            this.viewRecycler = viewRecycler;
            this.array = array;
            this.firstIndex = firstIndex;
        }

        @NonNull
        @Override
        public ArrayItemIterator create() {
            return new ArrayItemIterator(model, viewRecycler, array, firstIndex, reverse, start);
        }

    }

    /**
     * The model, which belongs to the tab switcher, whose tabs are iterated.
     */
    private final Model model;

    /**
     * The view recycler, which allows to inflate the views, which are used to visualize the
     * iterated tabs.
     */
    private final AttachedViewRecycler<AbstractItem, ?> viewRecycler;

    /**
     * The array, which contains the tabs, which are iterated by the iterator.
     */
    private final Tab[] array;

    /**
     * The index of the first tab, which is iterated by the iterator.
     */
    private final int firstIndex;

    /**
     * Creates a new iterator, which allows to iterate the items, which correspond to the tabs,
     * which are contained by an array.
     *
     * @param model
     *         The model, which belongs to the tab switcher, whose items should be iterated, as an
     *         instance of the type {@link Model}. The model may not be null
     * @param viewRecycler
     *         The view recycler, which allows to inflate the views, which are used to visualize the
     *         iterated tabs, as an instance of the class AttachedViewRecycler. The view recycler
     *         may not be null
     * @param array
     *         The array, which contains the tabs, which should be iterated by the iterator, as an
     *         array of the type {@link Tab}. The array may not be null
     * @param firstIndex
     *         The index of the first tab, which should be iterated by the iterator, as an {@link
     *         Integer} value. The index must be at least 0
     * @param reverse
     *         True, if the items should be iterated in reverse order, false otherwise
     * @param start
     *         The index of the first item, which should be iterated, as an {@link Integer} value or
     *         -1, if all items should be iterated
     */
    private ArrayItemIterator(@NonNull final Model model,
                              @NonNull final AttachedViewRecycler<AbstractItem, ?> viewRecycler,
                              @NonNull final Tab[] array, final int firstIndex,
                              final boolean reverse, final int start) {
        Condition.INSTANCE.ensureNotNull(model, "The model may not be null");
        Condition.INSTANCE.ensureNotNull(viewRecycler, "The view recycler may not be null");
        Condition.INSTANCE.ensureNotNull(array, "The array may not be null");
        Condition.INSTANCE.ensureAtLeast(firstIndex, 0, "The first index must be at least 0");
        this.model = model;
        this.viewRecycler = viewRecycler;
        this.array = array;
        this.firstIndex = firstIndex;
        initialize(reverse, start);
    }

    @Override
    public final int getCount() {
        return array.length;
    }

    @NonNull
    @Override
    public final AbstractItem getItem(final int index) {
        return TabItem.create(model, viewRecycler, firstIndex + index, array[index]);
    }

}
