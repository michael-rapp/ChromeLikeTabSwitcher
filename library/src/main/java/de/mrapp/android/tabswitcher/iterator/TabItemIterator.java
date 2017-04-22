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

import de.mrapp.android.tabswitcher.TabSwitcher;
import de.mrapp.android.tabswitcher.model.Model;
import de.mrapp.android.tabswitcher.model.TabItem;
import de.mrapp.android.util.view.AttachedViewRecycler;

import static de.mrapp.android.util.Condition.ensureNotNull;

/**
 * An iterator, which allows to iterate the tab items, which correspond to the tabs of a {@link
 * TabSwitcher}.
 *
 * @author Michael Rapp
 * @since 0.1.0
 */
public class TabItemIterator extends AbstractTabItemIterator {

    /**
     * A builder, which allows to configure and create instances of the class {@link
     * TabItemIterator}.
     */
    public static class Builder extends AbstractBuilder<Builder, TabItemIterator> {

        /**
         * The model, which belongs to the tab switcher, whose tabs should be iterated by the
         * iterator, which is created by the builder.
         */
        private final Model model;

        /**
         * The view recycler, which allows to inflate the views, which are used to visualize the
         * tabs, which are iterated by the iterator, which is created by the builder.
         */
        private final AttachedViewRecycler<TabItem, ?> viewRecycler;

        /**
         * Creates a new builder, which allows to configure and create instances of the class {@link
         * TabItemIterator}.
         *
         * @param model
         *         The model, which belongs to the tab switcher, whose tabs should be iterated by
         *         the iterator, which is created by the builder, as an instance of the type {@link
         *         Model}. The model may not be null
         * @param viewRecycler
         *         The view recycler, which allows to inflate the views, which are used to visualize
         *         the tabs, which are iterated by the iterator, which is created by the builder, as
         *         an instance of the class AttachedViewRecycler. The view recycler may not be null
         */
        public Builder(@NonNull final Model model,
                       @NonNull final AttachedViewRecycler<TabItem, ?> viewRecycler) {
            ensureNotNull(model, "The model may not be null");
            ensureNotNull(viewRecycler, "The view recycler may not be null");
            this.model = model;
            this.viewRecycler = viewRecycler;
        }

        @NonNull
        @Override
        public TabItemIterator create() {
            return new TabItemIterator(model, viewRecycler, reverse, start);
        }

    }

    /**
     * The model, which belongs to the tab switcher, whose tabs are iterated.
     */
    private final Model model;

    /**
     * The view recycler, which allows to inflated the views, which are used to visualize the
     * iterated tabs.
     */
    private final AttachedViewRecycler<TabItem, ?> viewRecycler;

    /**
     * Creates a new iterator, which allows to iterate the tab items, which correspond to the tabs
     * of a {@link TabSwitcher}.
     *
     * @param model
     *         The model, which belongs to the tab switcher, whose tabs should be iterated, as an
     *         instance of the type {@link Model}. The model may not be null
     * @param viewRecycler
     *         The view recycler, which allows to inflate the views, which are used to visualize the
     *         iterated tabs, as an instance of the class AttachedViewRecycler. The view recycler
     *         may not be null
     * @param reverse
     *         True, if the tabs should be iterated in reverse order, false otherwise
     * @param start
     *         The index of the first tab, which should be iterated, as an {@link Integer} value or
     *         -1, if all tabs should be iterated
     */
    private TabItemIterator(@NonNull final Model model,
                            @NonNull final AttachedViewRecycler<TabItem, ?> viewRecycler,
                            final boolean reverse, final int start) {
        ensureNotNull(model, "The model may not be null");
        ensureNotNull(viewRecycler, "The view recycler may not be null");
        this.model = model;
        this.viewRecycler = viewRecycler;
        initialize(reverse, start);
    }

    @Override
    public final int getCount() {
        return model.getCount();
    }

    @NonNull
    @Override
    public final TabItem getItem(final int index) {
        return TabItem.create(model, viewRecycler, index);
    }

}