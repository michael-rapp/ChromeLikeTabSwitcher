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

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.mrapp.android.tabswitcher.TabSwitcher;
import de.mrapp.util.Condition;

/**
 * An item, which contains information about a child view of a {@link TabSwitcher}.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public abstract class AbstractItem {

    /**
     * The index of the item.
     */
    private final int index;

    /**
     * The view, which is used to visualize the item.
     */
    private View view;

    /**
     * The tag, which is associated with the item.
     */
    private Tag tag;

    /**
     * Creates a new item, which contains information about a child view of a {@link TabSwitcher}.
     *
     * @param index
     *         The index of the item as an {@link Integer} value. The index must be at least 0
     */
    public AbstractItem(final int index) {
        Condition.INSTANCE.ensureAtLeast(index, 0, "The index must be at least 0");
        this.index = index;
        this.view = null;
        this.tag = new Tag();
    }

    /**
     * Returns the index of the item. The index allows to identify the item's view among all child
     * views, which are contained by the tab switcher.
     *
     * @return The index of the item as an {@link Integer} value. The index must be at least 0
     */
    public final int getIndex() {
        return index;
    }

    /**
     * Returns the view, which is used to visualize the item.
     *
     * @return The view, which is used to visualize the item, as an instance of the class {@link
     * View} or null, if no such view is currently inflated
     */
    public final View getView() {
        return view;
    }

    /**
     * Sets the view, which is used to visualize the item.
     *
     * @param view
     *         The view, which should be set, as an instance of the class {@link View} or null, if
     *         no view should be set
     */
    public final void setView(@Nullable final View view) {
        this.view = view;
    }

    /**
     * Returns the tag, which is associated with the item.
     *
     * @return The tag as an instance of the class {@link Tag}. The tag may not be null
     */
    @NonNull
    public final Tag getTag() {
        return tag;
    }

    /**
     * Sets the tag, which is associated with the item.
     *
     * @param tag
     *         The tag, which should be set, as an instance of the class {@link Tag}. The tag may
     *         not be null
     */
    public final void setTag(@NonNull final Tag tag) {
        Condition.INSTANCE.ensureNotNull(tag, "The tag may not be null");
        this.tag = tag;
    }

    /**
     * Returns, whether the item is currently visible, or not.
     *
     * @return True, if the item is currently visible, false otherwise
     */
    public final boolean isVisible() {
        return tag.getState() != State.HIDDEN || tag.isClosing();
    }

    /**
     * Returns, whether a view, which is used to visualize the item, is currently inflated, or not.
     *
     * @return True, if a view, which is used to visualize the item, is currently inflated, false
     * otherwise
     */
    @CallSuper
    public boolean isInflated() {
        return view != null;
    }

}