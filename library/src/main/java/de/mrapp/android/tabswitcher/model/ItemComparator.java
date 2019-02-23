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
package de.mrapp.android.tabswitcher.model;

import java.util.Comparator;

import androidx.annotation.NonNull;
import de.mrapp.android.tabswitcher.Tab;
import de.mrapp.android.tabswitcher.TabSwitcher;
import de.mrapp.util.Condition;

/**
 * A comparator, which allows to compare two instances of the class {@link AbstractItem}.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public class ItemComparator implements Comparator<AbstractItem> {

    /**
     * The tab switcher, the items, which are compared by the comparator, belong to.
     */
    private final TabSwitcher tabSwitcher;

    /**
     * Returns the tab switcher, the items, which are compared by the comparator, belong to.
     *
     * @return The tab switcher, the items, which are compared by the comparator, belong to, as an
     * instance of the class {@link TabSwitcher}. The tab switcher may not be null
     */
    @NonNull
    protected final TabSwitcher getTabSwitcher() {
        return tabSwitcher;
    }

    /**
     * Creates a new comparator, which allows to compare two instances of the class {@link
     * AbstractItem}.
     *
     * @param tabSwitcher
     *         The tab switcher, the tab items, which should be compared by the comparator, belong
     *         to, as a instance of the class {@link TabSwitcher}. The tab switcher may not be null
     */
    public ItemComparator(@NonNull final TabSwitcher tabSwitcher) {
        Condition.INSTANCE.ensureNotNull(tabSwitcher, "The tab switcher may not be null");
        this.tabSwitcher = tabSwitcher;
    }

    @Override
    public int compare(final AbstractItem o1, final AbstractItem o2) {
        if (o1 instanceof AddTabItem && o2 instanceof AddTabItem) {
            return 0;
        } else if (o1 instanceof AddTabItem) {
            return -1;
        } else if (o2 instanceof AddTabItem) {
            return 1;
        } else if (o1 instanceof TabItem && o2 instanceof TabItem) {
            TabItem item1 = (TabItem) o1;
            TabItem item2 = (TabItem) o2;
            Tab tab1 = item1.getTab();
            Tab tab2 = item2.getTab();
            int index1 = tabSwitcher.indexOf(tab1);
            index1 = index1 == -1 ? item1.getIndex() : index1;
            int index2 = tabSwitcher.indexOf(tab2);
            index2 = index2 == -1 ? item2.getIndex() : index2;
            Condition.INSTANCE
                    .ensureNotEqual(index1, -1, "Tab " + tab1 + " not contained by tab switcher",
                            RuntimeException.class);
            Condition.INSTANCE
                    .ensureNotEqual(index2, -1, "Tab " + tab2 + " not contained by tab switcher",
                            RuntimeException.class);
            return index1 < index2 ? -1 : 1;
        } else {
            throw new RuntimeException("Unknown item types");
        }
    }

}