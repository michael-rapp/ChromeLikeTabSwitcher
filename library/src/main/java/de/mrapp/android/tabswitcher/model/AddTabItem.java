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

import android.view.View;

import androidx.annotation.NonNull;
import de.mrapp.android.tabswitcher.R;
import de.mrapp.android.tabswitcher.TabSwitcher;
import de.mrapp.android.util.view.AttachedViewRecycler;

/**
 * An item, which contains information about a button a {@link TabSwitcher}, which allows to add a
 * new tab.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public class AddTabItem extends AbstractItem {

    /**
     * Creates a new item, which contains information about a button of a {@link TabSwitcher}, which
     * allows to add a new tab. By default, the item is neither associated with a view, nor with a
     * view holder.
     *
     * @param index
     *         The index of the item as an {@link Integer} value. The index must be at least 0
     */
    private AddTabItem(final int index) {
        super(index);
    }

    /**
     * Creates a new item, which contains information about a button of a {@link TabSwitcher}, which
     * allows to add a new tab.
     *
     * @param viewRecycler
     *         The view recycler, which is used to reuse the views, which are used to visualize
     *         tabs, as an instance of the class AttachedViewRecycler. The view recycler may not be
     *         null
     * @return The item, which has been created, as an instance of the class {@link AddTabItem}. The
     * item may not be null
     */
    @NonNull
    public static AddTabItem create(
            @NonNull final AttachedViewRecycler<AbstractItem, ?> viewRecycler) {
        AddTabItem addTabItem = new AddTabItem(0);
        View view = viewRecycler.getView(addTabItem);

        if (view != null) {
            addTabItem.setView(view);
            Tag tag = (Tag) view.getTag(R.id.tag_properties);

            if (tag != null) {
                addTabItem.setTag(tag);
            }
        }

        return addTabItem;
    }

    @Override
    public final String toString() {
        return "AddTabItem [index = " + getIndex() + "]";
    }

    @Override
    public final int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + getIndex();
        return result;
    }

    @Override
    public final boolean equals(final Object obj) {
        if (obj == null)
            return false;
        if (obj.getClass() != getClass())
            return false;
        AddTabItem other = (AddTabItem) obj;
        return getIndex() == other.getIndex();
    }

}