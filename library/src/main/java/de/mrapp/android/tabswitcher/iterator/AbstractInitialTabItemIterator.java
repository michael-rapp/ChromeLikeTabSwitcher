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
import de.mrapp.android.tabswitcher.model.TabItem;

import static de.mrapp.android.util.Condition.ensureNotNull;

/**
 * An abstract base class for all iterators, which allow to iterate the tab items, which correspond
 * to the tabs of a {@link TabSwitcher}. When a tab item is referenced for the first time, its
 * initial position and state is calculated and the tab item is stored in a backing array. When the
 * tab item is iterated again, it is retrieved from the backing array.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public abstract class AbstractInitialTabItemIterator extends AbstractTabItemIterator {

    /**
     * The backing array, which is used to store tab items, once their initial position and
     * state has been calculated.
     */
    private final TabItem[] backingArray;

    /**
     * Creates a new iterator, which allows to iterate the tab items, which corresponds to the tabs
     * of a {@link TabSwitcher}. When a tab item is referenced for the first time, its initial
     * position and state is calculated and the tab item is stored in a backing array. When the tab
     * item is iterated again, it is retrieved from the backing array.
     *
     * @param backingArray
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
    public AbstractInitialTabItemIterator(@NonNull final TabItem[] backingArray,
                                          final boolean reverse, final int start) {
        ensureNotNull(backingArray, "The backing array may not be null");
        this.backingArray = backingArray;
        initialize(reverse, start);
    }

    /**
     * The method, which is invoked on implementing subclasses in order to create an initial tab
     * item.
     *
     * @param index
     *         The index of the tab item, which should be created, as an {@link Integer} value
     * @return The tab item, which has been created, as an instance of the class {@link TabItem}.
     * The tab item may not be null
     */
    @NonNull
    protected abstract TabItem createInitialTabItem(final int index);

    @Override
    public final int getCount() {
        return backingArray.length;
    }

    @NonNull
    @Override
    public final TabItem getItem(final int index) {
        TabItem tabItem = backingArray[index];

        if (tabItem == null) {
            tabItem = createInitialTabItem(index);
            backingArray[index] = tabItem;
        }

        return tabItem;
    }

}