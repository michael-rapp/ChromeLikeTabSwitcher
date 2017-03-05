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

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Map;

import static de.mrapp.android.util.Condition.ensureNotNull;

/**
 * A recycler, which allows to cache views in order to be able to reuse them later, instead of
 * inflating new instances.
 *
 * @param <ItemType>
 *         The type of the items, which should be visualized by inflated views
 * @param <ParamType>
 *         The type of the optional parameters, which may be passed when inflating a view
 * @author Michael Rapp
 * @since 1.14.0
 */
public class ViewRecycler<ItemType, ParamType> extends AbstractViewRecycler<ItemType, ParamType> {

    /**
     * Creates a new recycler, which allows to cache views in order to be able to reuse them later,
     * instead of inflating new instances.
     *
     * @param context
     *         The context, which should be used by the recycler, as an instance of the class {@link
     *         Context}. The context may not be null
     */
    public ViewRecycler(@NonNull final Context context) {
        this(LayoutInflater.from(context));
    }

    /**
     * Creates a new recycler, which allows to cache views in order to be able to reuse them later,
     * instead of inflating new instances.
     *
     * @param inflater
     *         The layout inflater, which should be used to inflate views, as an instance of the
     *         class {@link LayoutInflater}. The layout inflater may not be null
     */
    public ViewRecycler(@NonNull final LayoutInflater inflater) {
        super(inflater);
    }

    /**
     * Inflates the view, which is used to visualize a specific item. If possible, an unused view
     * will be retrieved from the cache, instead of inflating a new instance.
     *
     * @param item
     *         The item, which should be visualized by the inflated view, as an instance of the
     *         generic type ItemType. The item may not be null
     * @param parent
     *         The parent of the inflated view as an instance of the class {@link ViewGroup} or
     *         null, if no parent is available
     * @param params
     *         An array, which may contain optional parameters, as an array of the generic type
     *         ParamType or an empty array, if no optional parameters are available
     * @return A pair, which contains the view, which is used to visualize the given item, as well
     * as a boolean value, which indicates, whether a new view has been inflated, or if an unused
     * view has been reused from the cache, as an instance of the class {@link Pair}. The pair may
     * not be null
     */
    @SafeVarargs
    @NonNull
    public final Pair<View, Boolean> inflate(@NonNull final ItemType item,
                                             @Nullable final ViewGroup parent,
                                             @NonNull final ParamType... params) {
        return inflate(item, parent, true, params);
    }

    /**
     * Inflates the view, which is used to visualize a specific item.
     *
     * @param item
     *         The item, which should be visualized by the inflated view, as an instance of the
     *         generic type ItemType. The item may not be null
     * @param parent
     *         The parent of the inflated view as an instance of the class {@link ViewGroup} or
     *         null, if no parent is available
     * @param useCache
     *         True, if an unused view should retrieved from the cache, if possible, false, if a new
     *         instance should be inflated instead
     * @param params
     *         An array, which may contain optional parameters, as an array of the generic type
     *         ParamType or an empty array, if no optional parameters are available
     * @return A pair, which contains the view, which is used to visualize the given item, as well
     * as a boolean value, which indicates, whether a new view has been inflated, or if an unused
     * view has been reused from the cache, as an instance of the class {@link Pair}. The pair may
     * not be null
     */
    @SafeVarargs
    @NonNull
    public final Pair<View, Boolean> inflate(@NonNull final ItemType item,
                                             @Nullable final ViewGroup parent,
                                             final boolean useCache,
                                             @NonNull final ParamType... params) {
        ensureNotNull(params, "The array may not be null");
        ensureNotNull(getAdapter(), "No adapter has been set", IllegalStateException.class);

        View view = getView(item);
        boolean inflated = false;

        if (view == null) {
            int viewType = getAdapter().getViewType(item);

            if (useCache) {
                view = pollUnusedView(viewType);
            }

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
        }

        getAdapter().onShowView(getContext(), view, item, inflated, params);
        getLogger().logDebug(getClass(), "Updated view of item " + item);
        return Pair.create(view, inflated);
    }

    @SafeVarargs
    @NonNull
    @Override
    public final Pair<View, Boolean> inflate(@NonNull final ItemType item, final boolean useCache,
                                             @NonNull final ParamType... params) {
        return inflate(item, null, useCache, params);
    }

    @Override
    public final void remove(@NonNull final ItemType item) {
        ensureNotNull(item, "The item may not be null");
        ensureNotNull(getAdapter(), "No adapter has been set", IllegalStateException.class);
        View view = getActiveViews().remove(item);

        if (view != null) {
            getAdapter().onRemoveView(view, item);
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

        for (Map.Entry<ItemType, View> entry : getActiveViews().entrySet()) {
            ItemType item = entry.getKey();
            View view = entry.getValue();
            getAdapter().onRemoveView(view, item);
            int viewType = getAdapter().getViewType(item);
            addUnusedView(view, viewType);
        }

        getActiveViews().clear();
        getLogger().logInfo(getClass(), "Removed all views");
    }

}