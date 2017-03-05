/*
 * Copyright 2015 - 2017 Michael Rapp
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
package de.mrapp.android.tabswitcher.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static de.mrapp.android.util.Condition.ensureNotNull;

/**
 * A recycler, which allows to cache views in order to be able to reuse them later instead of
 * inflating new instances. As an particularity, an {@link AttachedViewRecycler} is bound to a
 * {@link ViewGroup}, which acts as the parent of all inflated views. Each time a view is inflated
 * using the <code>inflate</code>-method, it is added the the parent. By calling the
 * <code>remove</code>- or <code>removeAll</code>-method, previously inflated views can be removed
 * from the parent. They will be kept in a cache in order to reuse them later.
 *
 * @param <ItemType>
 *         The type of the items, which should be visualized by inflated views
 * @param <ParamType>
 *         The type of the optional parameters, which may be passed when inflating a view
 * @author Michael Rapp
 * @since 1.14.0
 */
public class AttachedViewRecycler<ItemType, ParamType>
        extends AbstractViewRecycler<ItemType, ParamType> {

    /**
     * The parent, the recycler is bound to.
     */
    private final ViewGroup parent;

    /**
     * The comparator, which is used to determine the order, which is used to add views to the
     * parent.
     */
    private final Comparator<ItemType> comparator;

    /**
     * A list, which contains the items, which are currently visualized by the active views. The
     * order of the items corresponds to the hierarchical order of the corresponding views in their
     * parent.
     */
    private final List<ItemType> items;

    /**
     * Creates a new recycler, which allows to cache views in order to be able to reuse them later
     * instead of inflating new instances. By default, views are added to the parent in the order of
     * their inflation.
     *
     * @param parent
     *         The parent, the recycler should be bound to, as an instance of the class {@link
     *         ViewGroup}. The parent may not be null
     */
    public AttachedViewRecycler(@NonNull final ViewGroup parent) {
        this(parent, LayoutInflater.from(parent.getContext()));
    }

    /**
     * Creates a new recycler, which allows to cache views in order to be able to reuse them later
     * instead of inflating new instances. This constructor allows to specify a comparator, which
     * allows to determine the order, which should be used to add views to the parent.
     *
     * @param parent
     *         The parent, the recycler should be bound to, as an instance of the class {@link
     *         ViewGroup}. The parent may not be null
     * @param comparator
     *         The comparator, which allows to determine the order, which should be used to add
     *         views to the parent, as an instance of the type {@link Comparator} or null, if the
     *         views should be added in the order of their inflation
     */
    public AttachedViewRecycler(@NonNull final ViewGroup parent,
                                @Nullable final Comparator<ItemType> comparator) {
        this(parent, LayoutInflater.from(parent.getContext()), comparator);
    }

    /**
     * Creates a new recycler, which allows to cache views in order to be able to reuse them later
     * instead of inflating new instances. By default, views are added to the parent in the order of
     * their inflation.
     *
     * @param parent
     *         The parent, the recycler should be bound to, as an instance of the class {@link
     *         ViewGroup}. The parent may not be null
     * @param inflater
     *         The layout inflater, which should be used to inflate views, as an instance of the
     *         class {@link LayoutInflater}. The layout inflater may not be null
     */
    public AttachedViewRecycler(@NonNull final ViewGroup parent,
                                @NonNull final LayoutInflater inflater) {
        this(parent, inflater, null);
    }

    /**
     * Creates a new recycler, which allows to cache views in order to be able to reuse them later
     * instead of inflating new instances. This constructor allows to specify a comparator, which
     * allows to determine the order, which should be used to add views to the parent.
     *
     * @param parent
     *         The parent, the recycler should be bound to, as an instance of the class {@link
     *         ViewGroup}. The parent may not be null
     * @param inflater
     *         The layout inflater, which should be used to inflate views, as an instance of the
     *         class {@link LayoutInflater}. The layout inflater may not be null
     * @param comparator
     *         The comparator, which allows to determine the order, which should be used to add
     *         views to the parent, as an instance of the type {@link Comparator} or null, if the
     *         views should be added in the order of their inflation
     */
    public AttachedViewRecycler(@NonNull final ViewGroup parent,
                                @NonNull final LayoutInflater inflater,
                                @Nullable final Comparator<ItemType> comparator) {
        super(inflater);
        ensureNotNull(parent, "The parent may not be null");
        this.parent = parent;
        this.comparator = comparator;
        this.items = new ArrayList<>();
    }

    @SafeVarargs
    @NonNull
    @Override
    public final Pair<View, Boolean> inflate(@NonNull final ItemType item,
                                             @NonNull final ParamType... params) {
        ensureNotNull(params, "The array may not be null");
        ensureNotNull(getAdapter(), "No adapter has been set", IllegalStateException.class);

        View view = getView(item);
        boolean inflated = false;

        if (view == null) {
            int viewType = getAdapter().getViewType(item);
            view = pollUnusedView(viewType);

            if (view == null) {
                view = getAdapter()
                        .onInflateView(getLayoutInflater(), parent, item, viewType, params);
                inflated = true;
                getLogger().logInfo(getClass(),
                        "Inflated view to visualize item " + item + " using view type " + viewType);
            } else {
                getLogger().logInfo(getClass(),
                        "Reusing view to visualize item " + item + " using view type " + viewType);
            }

            getActiveViews().put(item, view);
            int index;

            if (comparator != null) {
                index = Collections.binarySearch(items, item, comparator);

                if (index < 0) {
                    index = ~index;
                }
            } else {
                index = items.size();
            }

            items.add(index, item);
            parent.addView(view, index);
            getLogger().logDebug(getClass(), "Added view of item " + item + " at index " + index);
        }

        getAdapter().onShowView(getContext(), view, item, inflated, params);
        getLogger().logDebug(getClass(), "Updated view of item " + item);
        return Pair.create(view, inflated);
    }

    @Override
    public final void remove(@NonNull final ItemType item) {
        ensureNotNull(item, "The item may not be null");
        ensureNotNull(getAdapter(), "No adapter has been set", IllegalStateException.class);
        int index = items.indexOf(item);

        if (index != -1) {
            items.remove(index);
            View view = getActiveViews().remove(item);
            getAdapter().onRemoveView(view, item);
            parent.removeViewAt(index);
            int viewType = getAdapter().getViewType(item);
            addUnusedView(view, viewType);
            getLogger().logInfo(getClass(), "Removed view of item " + item);
        } else {
            getLogger().logDebug(getClass(),
                    "Did not remove view of item " + item + ". View is not inflated");
        }
    }

    @Override
    public final void removeAll() {
        ensureNotNull(getAdapter(), "No adapter has been set", IllegalStateException.class);

        for (int i = items.size() - 1; i >= 0; i--) {
            ItemType item = items.remove(i);
            View view = getActiveViews().remove(item);
            getAdapter().onRemoveView(view, item);
            parent.removeViewAt(i);
            int viewType = getAdapter().getViewType(item);
            addUnusedView(view, viewType);
        }

        getLogger().logInfo(getClass(), "Removed all views");
    }

}