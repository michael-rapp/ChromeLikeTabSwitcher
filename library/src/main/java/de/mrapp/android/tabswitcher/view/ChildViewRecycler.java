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
package de.mrapp.android.tabswitcher.view;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.mrapp.android.tabswitcher.Tab;
import de.mrapp.android.tabswitcher.TabSwitcherDecorator;

import static de.mrapp.android.util.Condition.ensureNotNull;

/**
 * A recycler, which allows to cache the child views, which are inflated by a {@link
 * TabSwitcherDecorator}, in order to be able to reuse them later instead of inflating new
 * instances. For each view type only one instance is inflated.
 *
 * @author Michael Rapp
 * @since 1.0.0
 */
public class ChildViewRecycler {

    /**
     * The layout inflater, which is used to inflate child views.
     */
    private final LayoutInflater inflater;

    /**
     * The decorator, which is used to inflate child views.
     */
    private TabSwitcherDecorator decorator;

    /**
     * A sparse array, which manages the views, which have already been inflated. The views are
     * associated with the view types the correspond to.
     */
    private SparseArray<View> views;

    /**
     * Creates a new recycler, which allows to cache the child views, which are inflated by a {@link
     * TabSwitcherDecorator}, in order to be able to reuse them later instead of inflating new
     * instances.
     *
     * @param inflater
     *         The layout inflater, which should be used to inflate views, as an instance of the
     *         class {@link LayoutInflater}. The layout inflater may not be null
     */
    public ChildViewRecycler(@NonNull final LayoutInflater inflater) {
        ensureNotNull(inflater, "The inflater may not be null");
        this.inflater = inflater;
    }

    /**
     * Sets the decorator, which should be used to inflate child views.
     *
     * @param decorator
     *         The decorator, which should be set, as an instance of the class {@link
     *         TabSwitcherDecorator}. The decorator may not be null
     */
    public final void setDecorator(@NonNull final TabSwitcherDecorator decorator) {
        ensureNotNull(decorator, "The decorator may not be null");
        this.decorator = decorator;
        clearCache();
    }

    /**
     * Inflates the child view, which visualizes a specific tab. If a view with the same view type
     * has already been inflated, it will be returned instead.
     *
     * @param tab
     *         The tab, which should be visualized, as an instance of the class {@link Tab}. The tab
     *         may not be null
     * @param parent
     *         The parent, the child view will be added to, as an instance of the class {@link
     *         ViewGroup} or null, if the child view will not be added to a parent
     * @return The view, which has been inflated, as an instance of the class {@link View}. The view
     * may not be null
     */
    @NonNull
    public final View inflate(@NonNull final Tab tab, @Nullable final ViewGroup parent) {
        ensureNotNull(tab, "The tab may not be null");
        ensureNotNull(decorator, "No decorator has been set", IllegalStateException.class);
        int viewType = decorator.getViewType(tab);
        View child = null;

        if (views == null) {
            views = new SparseArray<>(decorator.getViewTypeCount());
        } else {
            // TODO: Views are now forced to be always inflated because of concurrency issues. A more sophisticated view recycling mechanism is needed.
            // child = views.get(viewType);
        }

        if (child == null) {
            child = decorator.inflateView(inflater, parent, tab);
            views.put(viewType, child);
        }

        return child;
    }

    /**
     * Removes all views from the cache.
     */
    public final void clearCache() {
        if (views != null) {
            views.clear();
            views = null;
        }
    }

}