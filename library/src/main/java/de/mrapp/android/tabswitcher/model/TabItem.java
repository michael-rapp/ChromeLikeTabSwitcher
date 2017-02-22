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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import de.mrapp.android.tabswitcher.Tab;
import de.mrapp.android.tabswitcher.TabSwitcher;
import de.mrapp.android.tabswitcher.view.TabViewHolder;

import static de.mrapp.android.util.Condition.ensureAtLeast;
import static de.mrapp.android.util.Condition.ensureNotNull;

/**
 * An item, which contains information about a tab of a {@link TabSwitcher}.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public class TabItem {

    /**
     * The index of the tab.
     */
    private final int index;

    /**
     * The tab.
     */
    private final Tab tab;

    /**
     * The view, which is used to visualize the tab.
     */
    private View view;

    /**
     * The view holder, which stores references the views, which belong to the tab.
     */
    private TabViewHolder viewHolder;

    /**
     * The tag, which is associated with the tab.
     */
    private Tag tag;

    /**
     * Creates a new item, which contains information about a tab of a {@link TabSwitcher}. By
     * default, the item is neither associated with a view, nor with a view holder.
     *
     * @param index
     *         The index of the tab as an {@link Integer} value. The index must be at least 0
     * @param tab
     *         The tab as an instance of the class {@link Tab}. The tab may not be null
     */
    public TabItem(final int index, @NonNull final Tab tab) {
        ensureAtLeast(index, 0, "The index must be at least 0");
        ensureNotNull(tab, "The tab may not be null");
        this.index = index;
        this.tab = tab;
        this.view = null;
        this.viewHolder = null;
        this.tag = null;
    }

    /**
     * Returns the index of the tab.
     *
     * @return The index of the tab as an {@link Integer} value. The index must be at least 0
     */
    public final int getIndex() {
        return index;
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
     * Returns the view, which is used to visualize the tab.
     *
     * @return The view, which is used to visualize the tab, as an instance of the class {@link
     * View} or null, if no such view is currently inflated
     */
    public final View getView() {
        return view;
    }

    /**
     * Sets the view, which is used to visualize the tab.
     *
     * @param view
     *         The view, which should be set, as an instance of the class {@link View} or null, if
     *         no view should be set
     */
    public final void setView(@Nullable final View view) {
        this.view = view;
    }

    /**
     * Returns the view holder, which stores references to the views, which belong to the tab.
     *
     * @return The view holder as an instance of the class {@link TabViewHolder} or null, if no view
     * is is currently inflated to visualize the tab
     */
    public final TabViewHolder getViewHolder() {
        return viewHolder;
    }

    /**
     * Sets the view holder, which stores references to the views, which belong to the tab.
     *
     * @param viewHolder
     *         The view holder, which should be set, as an instance of the class {@link
     *         TabViewHolder} or null, if no view holder should be set
     */
    public final void setViewHolder(@Nullable final TabViewHolder viewHolder) {
        this.viewHolder = viewHolder;
    }

    /**
     * Returns the tag, which is associated with the tab.
     *
     * @return The tag as an instance of the class {@link Tag}. The tag may not be null
     */
    @NonNull
    public final Tag getTag() {
        return tag;
    }

    /**
     * Sets the tag, which is associated with the tab.
     *
     * @param tag
     *         The tag, which should be set, as an instance of the class {@link Tag}. The tag may
     *         not be null
     */
    public final void setTag(@NonNull final Tag tag) {
        ensureNotNull(tag, "The tag may not be null");
        this.tag = tag;
    }

    /**
     * Returns, whether a view, which is used to visualize the tab, is currently inflated, or not.
     *
     * @return True, if a view, which is used to visualize the tab, is currently inflated, false
     * otherwise
     */
    public final boolean isInflated() {
        return view != null && viewHolder != null;
    }

    /**
     * Returns, whether the tab is currently visible, or not.
     *
     * @return True, if the tab is currently visible, false otherwise
     */
    public final boolean isVisible() {
        return tag.getState() != State.HIDDEN || tag.isClosing();
    }

    @Override
    public final String toString() {
        return "TabItem [index = " + index + "]";
    }

    @Override
    public final int hashCode() {
        return tab.hashCode();
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