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

import de.mrapp.android.tabswitcher.TabSwitcher;

/**
 * An item, which contains information about a button a {@link TabSwitcher}, which allows to add a
 * new tab.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public class AddTabItem extends AbstractItem {

    /**
     * Creates a new item, which contains information about a child view of a {@link TabSwitcher}.
     *
     * @param index
     *         The index of the item as an {@link Integer} value. The index must be at least 0
     */
    public AddTabItem(final int index) {
        super(index);
    }

    @Override
    public final String toString() {
        return "AddTabItem [index = " + getIndex() + "]";
    }

}