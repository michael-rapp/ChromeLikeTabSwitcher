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
import androidx.annotation.Nullable;
import de.mrapp.android.tabswitcher.R;
import de.mrapp.android.tabswitcher.Tab;
import de.mrapp.android.tabswitcher.TabSwitcher;
import de.mrapp.android.tabswitcher.layout.AbstractTabViewHolder;
import de.mrapp.android.util.view.AttachedViewRecycler;
import de.mrapp.util.Condition;

/**
 * An item, which contains information about a tab of a {@link TabSwitcher}.
 *
 * @author Michael Rapp
 * @since 0.1.0
 */
public class TabItem extends AbstractItem {

    /**
     * The tab.
     */
    private final Tab tab;

    /**
     * The view holder, which stores references the views, which belong to the tab.
     */
    private AbstractTabViewHolder viewHolder;

    /**
     * Creates a new item, which contains information about a tab of a {@link TabSwitcher}. By
     * default, the item is neither associated with a view, nor with a view holder.
     *
     * @param index
     *         The index of the item as an {@link Integer} value. The index must be at least 0
     * @param tab
     *         The tab as an instance of the class {@link Tab}. The tab may not be null
     */
    private TabItem(final int index, @NonNull final Tab tab) {
        super(index);
        Condition.INSTANCE.ensureNotNull(tab, "The tab may not be null");
        this.tab = tab;
        this.viewHolder = null;
    }

    /**
     * Creates a new item, which contains information about a tab of a tab switcher.
     *
     * @param model
     *         The model, the tab belongs to, as an instance of the type {@link Model}. The model
     *         may not be null
     * @param index
     *         The index of the tab as an {@link Integer} value. The index must be at least 0
     * @param tab
     *         The tab as an instance of the class {@link Tab}. The tab may not be null
     * @return The item, which has been created, as an instance of the class {@link TabItem}. The
     * item may not be null
     */
    @NonNull
    public static TabItem create(@NonNull final Model model, final int index,
                                 @NonNull final Tab tab) {
        return new TabItem(index + (model.isAddTabButtonShown() ? 1 : 0), tab);
    }

    /**
     * Creates a new item, which contains information about a tab of a tab switcher.
     *
     * @param model
     *         The model, the tab belongs to, as an instance of the type {@link Model}. The model
     *         may not be null
     * @param viewRecycler
     *         The view recycler, which is used to reuse the views, which are used to visualize
     *         tabs, as an instance of the class AttachedViewRecycler. The view recycler may not be
     *         null
     * @param index
     *         The index of the tab as an {@link Integer} value. The index must be at least 0
     * @return The item, which has been created, as an instance of the class {@link TabItem}. The
     * item may not be null
     */
    @NonNull
    public static TabItem create(@NonNull final Model model,
                                 @NonNull final AttachedViewRecycler<AbstractItem, ?> viewRecycler,
                                 final int index) {
        Tab tab = model.getTab(index);
        return create(model, viewRecycler, index, tab);
    }

    /**
     * Creates a new item, which contains information about a specific tab.
     *
     * @param model
     *         The model, the tab belongs to, as an instance of the type {@link Model}. The model
     *         may not be null
     * @param viewRecycler
     *         The view recycler, which is used to reuse the views, which are used to visualize
     *         tabs, as an instance of the class AttachedViewRecycler. The view recycler may not be
     *         null
     * @param index
     *         The index of the tab as an {@link Integer} value. The index must be at least 0
     * @param tab
     *         The tab as an instance of the class {@link Tab}. The tab may not be null
     * @return The item, which has been created, as an instance of the class {@link TabItem}. The
     * item may not be null
     */
    @NonNull
    public static TabItem create(@NonNull final Model model,
                                 @NonNull final AttachedViewRecycler<AbstractItem, ?> viewRecycler,
                                 final int index, @NonNull final Tab tab) {
        TabItem tabItem = new TabItem(index + (model.isAddTabButtonShown() ? 1 : 0), tab);
        View view = viewRecycler.getView(tabItem);

        if (view != null) {
            tabItem.setView(view);
            tabItem.setViewHolder((AbstractTabViewHolder) view.getTag(R.id.tag_view_holder));
            Tag tag = (Tag) view.getTag(R.id.tag_properties);

            if (tag != null) {
                tabItem.setTag(tag);
            }
        }

        return tabItem;
    }

    /**
     * Returns the tab.
     *
     * @return The tab as an instance of the class {@link Tab}. The tab may not be null
     */
    @NonNull
    public final Tab getTab() {
        return tab;
    }

    /**
     * Returns the view holder, which stores references to the views, which belong to the tab.
     *
     * @return The view holder as an instance of the class {@link AbstractTabViewHolder} or null, if
     * no view is is currently inflated to visualize the tab
     */
    public final AbstractTabViewHolder getViewHolder() {
        return viewHolder;
    }

    /**
     * Sets the view holder, which stores references to the views, which belong to the tab.
     *
     * @param viewHolder
     *         The view holder, which should be set, as an instance of the class {@link
     *         AbstractTabViewHolder} or null, if no view holder should be set
     */
    public final void setViewHolder(@Nullable final AbstractTabViewHolder viewHolder) {
        this.viewHolder = viewHolder;
    }

    @Override
    public final boolean isInflated() {
        return super.isInflated() && viewHolder != null;
    }

    @Override
    public final String toString() {
        return "TabItem [index = " + getIndex() + "]";
    }

    @Override
    public final int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + tab.hashCode();
        return result;
    }

    @Override
    public final boolean equals(final Object obj) {
        if (obj == null)
            return false;
        if (obj.getClass() != getClass())
            return false;
        TabItem other = (TabItem) obj;
        return tab.equals(other.tab);
    }

}
